package info.mudbourn.mmsarsenal.gun;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

// Runs a reload on the server: waits out the gun's reload timer and moves ammo from the inventory into the gun.
public final class ReloadTracker {

    private static final Map<Player, ReloadTracker> TRACKERS = new WeakHashMap<>();
    private static final Map<Player, Boolean> FIRST_PERSON = new WeakHashMap<>();

    private final int startTick;
    private final int slot;
    private final ItemStack stack;
    private final Gun gun;
    private boolean firstReload = true;

    private ReloadTracker(Player player) {
        this.startTick = player.tickCount;
        this.slot = player.getInventory().getSelectedSlot();
        this.stack = player.getInventory().getSelectedItem();
        this.gun = ((GunItem) this.stack.getItem()).getGun(false);
    }

    // Records whether the player reloads in first person, which decides whether they hear their own reload from the server.
    public static void setFirstPerson(Player player, boolean firstPerson) {
        FIRST_PERSON.put(player, firstPerson);
    }

    // Advances every reloading player once per server tick.
    public static void tick(ServerPlayer player) {
        if (!GunState.isReloading(player)) {
            TRACKERS.remove(player);
            return;
        }
        ReloadTracker tracker = TRACKERS.get(player);
        if (tracker == null) {
            if (!(player.getInventory().getSelectedItem().getItem() instanceof GunItem)) {
                GunState.setReloading(player, false);
                return;
            }
            tracker = new ReloadTracker(player);
            TRACKERS.put(player, tracker);
        }
        if (!tracker.isSameWeapon(player) || tracker.isFull() || GunAmmo.lacksAmmo(player, tracker.gun)) {
            TRACKERS.remove(player);
            GunState.setReloading(player, false);
            return;
        }
        if (tracker.canReload(player)) {
            switch (tracker.gun.reloads().reloadType()) {
                case MAG_FED -> tracker.increaseMagAmmo(player);
                case SINGLE_ITEM -> tracker.reloadItem(player);
                case MANUAL -> {
                    tracker.increaseAmmo(player);
                    tracker.firstReload = false;
                }
                case INVENTORY_FED -> tracker.increaseAmmo(player);
            }
            if (tracker.isFull() || GunAmmo.lacksAmmo(player, tracker.gun)) {
                TRACKERS.remove(player);
                GunState.setReloading(player, false);
            }
        }
    }

    // Takes one round from the inventory into the gun after each shot of an inventory-fed gun.
    public static void inventoryFeed(Player player, Gun gun) {
        ItemStack ammo = GunAmmo.find(player, gun.projectile().item());
        ItemStack held = player.getMainHandItem();
        if (!ammo.isEmpty()) {
            GunComponents.setAmmo(held, GunComponents.ammo(held) + 1);
            ammo.shrink(1);
        }
    }

    private boolean isSameWeapon(Player player) {
        return !this.stack.isEmpty()
            && player.getInventory().getSelectedSlot() == this.slot
            && player.getInventory().getSelectedItem() == this.stack;
    }

    private boolean isFull() {
        return GunComponents.ammo(this.stack) >= this.gun.reloads().maxAmmo();
    }

    private boolean canReload(Player player) {
        int deltaTicks = player.tickCount - this.startTick;
        if (deltaTicks == 4) {
            playReloadSound(this.gun.sounds().reloadStart(), player);
        }
        Gun.Reloads reloads = this.gun.reloads();
        int interval = GunComponents.ammo(this.stack) == 0
            ? reloads.reloadTimer() + reloads.additionalReloadTimer()
            : reloads.reloadTimer();
        interval = Math.max(interval, 1);
        ReloadType type = reloads.reloadType();
        if (type != ReloadType.MAG_FED && type != ReloadType.INVENTORY_FED && type != ReloadType.SINGLE_ITEM) {
            if (this.firstReload) {
                interval += reloads.additionalReloadTimer();
            }
            return deltaTicks > 0 && deltaTicks % interval == 0;
        }
        if (type != ReloadType.INVENTORY_FED) {
            if (deltaTicks == interval / 2) {
                playReloadSound(this.gun.sounds().reloadLoad(), player);
            }
            if (deltaTicks == interval - 5) {
                playReloadSound(this.gun.sounds().ejectorPull(), player);
            }
        }
        return deltaTicks > interval;
    }

    private void increaseMagAmmo(Player player) {
        List<ItemStack> ammo = GunAmmo.findAll(player, this.gun.projectile().item());
        if (!ammo.isEmpty()) {
            int maxAmmo = this.gun.reloads().maxAmmo();
            int available = Math.min(GunAmmo.total(ammo), maxAmmo);
            int current = GunComponents.ammo(this.stack);
            int missing = maxAmmo - current;
            if (available < missing) {
                GunComponents.setAmmo(this.stack, current + available);
                shrink(ammo, player, available);
            } else {
                GunComponents.setAmmo(this.stack, maxAmmo);
                shrink(ammo, player, missing);
            }
        }
        playReloadSound(this.gun.sounds().ejectorRelease(), player);
    }

    private void reloadItem(Player player) {
        ItemStack ammo = GunAmmo.find(player, this.gun.reloads().reloadItem());
        if (!ammo.isEmpty()) {
            GunComponents.setAmmo(this.stack, this.gun.reloads().maxAmmo());
            if (!player.isCreative()) {
                ammo.shrink(1);
            }
        }
        playReloadSound(this.gun.sounds().reloadLoad(), player);
    }

    private void increaseAmmo(Player player) {
        ItemStack ammo = GunAmmo.find(player, this.gun.projectile().item());
        if (!ammo.isEmpty()) {
            int amount = Math.min(ammo.getCount(), this.gun.reloads().reloadAmount());
            amount = Math.min(amount, this.gun.reloads().maxAmmo() - GunComponents.ammo(this.stack));
            GunComponents.setAmmo(this.stack, GunComponents.ammo(this.stack) + amount);
            if (!player.isCreative()) {
                ammo.shrink(amount);
            }
            playReloadSound(this.gun.sounds().reloadLoad(), player);
        }
        playReloadSound(this.gun.sounds().reloadLoad(), player);
    }

    private static void shrink(List<ItemStack> stacks, Player player, int amount) {
        if (player.isCreative()) {
            return;
        }
        int left = amount;
        for (ItemStack stack : stacks) {
            int taken = Math.min(left, stack.getCount());
            stack.shrink(taken);
            left -= taken;
            if (left == 0) {
                return;
            }
        }
    }

    // Plays a reload sound to everyone nearby, leaving out a first-person reloader who hears their own animation.
    private static void playReloadSound(Identifier sound, Player player) {
        if (sound == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.getValue(sound);
        if (event == null) {
            event = SoundEvent.createVariableRangeEvent(sound);
        }
        Player except = FIRST_PERSON.getOrDefault(player, false) ? player : null;
        level.playSound(except, player.getOnPos(), event, SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
