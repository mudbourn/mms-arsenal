package info.mudbourn.mmsarsenal.client.gun;

import info.mudbourn.mmsarsenal.gun.Gun;
import info.mudbourn.mmsarsenal.gun.GunComponents;
import info.mudbourn.mmsarsenal.gun.GunItem;
import info.mudbourn.mmsarsenal.gun.GunState;
import info.mudbourn.mmsarsenal.gun.net.GunActionPayload;
import info.mudbourn.mmsarsenal.gun.net.GunFlagPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

// Reload and unload keys, the reload request to the server, and the client's reload timer.
public final class ReloadClient {

    private static final ReloadClient INSTANCE = new ReloadClient();

    private int startReloadTick = -1;
    private int reloadTimer;
    private int prevReloadTimer;
    private int reloadingSlot = -1;

    public static ReloadClient get() {
        return INSTANCE;
    }

    public void tick(Minecraft mc) {
        this.prevReloadTimer = this.reloadTimer;
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        if (GunState.isReloading(player)) {
            if (this.reloadingSlot != player.getInventory().getSelectedSlot()) {
                this.setReloading(false);
            }
            boolean firstPerson = mc.options.getCameraType().isFirstPerson();
            ClientPlayNetworking.send(new GunFlagPayload(GunFlagPayload.Flag.FIRST_PERSON_RELOAD, firstPerson));
        }
        if (GunState.isReloading(player)) {
            if (this.startReloadTick == -1) {
                this.startReloadTick = player.tickCount + 5;
            }
            if (this.reloadTimer < 5) {
                this.reloadTimer++;
            }
        } else {
            this.startReloadTick = -1;
            if (this.reloadTimer > 0) {
                this.reloadTimer--;
            }
        }
    }

    // Handles the reload, unload and melee keys while a gun is held.
    public void handleKeys(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || !(player.getMainHandItem().getItem() instanceof GunItem)) {
            return;
        }
        while (GunKeys.RELOAD.consumeClick()) {
            this.setReloading(true);
        }
        while (GunKeys.UNLOAD.consumeClick()) {
            this.setReloading(false);
            ClientPlayNetworking.send(new GunActionPayload(GunActionPayload.Action.UNLOAD));
        }
    }

    // Asks the server to start or stop reloading; a full or bottomless gun does not start.
    public void setReloading(boolean reloading) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (reloading) {
            if (!(held.getItem() instanceof GunItem gunItem) || held.has(GunComponents.IGNORE_AMMO)) {
                return;
            }
            Gun gun = gunItem.getGun(true);
            if (GunComponents.ammo(held) >= gun.reloads().maxAmmo()) {
                return;
            }
            GunState.setReloading(player, true);
            ClientPlayNetworking.send(new GunFlagPayload(GunFlagPayload.Flag.RELOADING, true));
            this.reloadingSlot = player.getInventory().getSelectedSlot();
        } else {
            GunState.setReloading(player, false);
            ClientPlayNetworking.send(new GunFlagPayload(GunFlagPayload.Flag.RELOADING, false));
            this.reloadingSlot = -1;
        }
    }

    public int startReloadTick() {
        return this.startReloadTick;
    }

    public int reloadTimer() {
        return this.reloadTimer;
    }

    public float reloadProgress(float partialTick) {
        return (this.prevReloadTimer + (this.reloadTimer - this.prevReloadTimer) * partialTick) / 5.0F;
    }
}
