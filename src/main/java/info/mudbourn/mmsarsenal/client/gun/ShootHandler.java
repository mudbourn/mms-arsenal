package info.mudbourn.mmsarsenal.client.gun;

import info.mudbourn.mmsarsenal.gun.DrawTracker;
import info.mudbourn.mmsarsenal.gun.FireMode;
import info.mudbourn.mmsarsenal.gun.Gun;
import info.mudbourn.mmsarsenal.gun.GunComponents;
import info.mudbourn.mmsarsenal.gun.GunItem;
import info.mudbourn.mmsarsenal.gun.GunState;
import info.mudbourn.mmsarsenal.gun.ReloadType;
import info.mudbourn.mmsarsenal.gun.net.GunActionPayload;
import info.mudbourn.mmsarsenal.gun.net.GunFlagPayload;
import info.mudbourn.mmsarsenal.gun.net.ShootPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

// The trigger: turns the attack button into shots by fire mode, with fire timers, bursts and overheating.
public final class ShootHandler {

    private static final ShootHandler INSTANCE = new ShootHandler();

    private int fireTimer;
    private int holdFire;
    private int overheatTimer;
    private boolean previouslyPressed;
    private boolean overheated;
    private boolean shooting;
    private int burstCount;

    public static ShootHandler get() {
        return INSTANCE;
    }

    public boolean isShooting() {
        return this.shooting;
    }

    public int fireTimer() {
        return this.fireTimer;
    }

    public int overheatTimer() {
        return this.overheatTimer;
    }

    private static boolean inGame(Minecraft mc) {
        return mc.getOverlay() == null && mc.screen == null && mc.mouseHandler.isMouseGrabbed() && mc.isWindowActive();
    }

    // Start of tick: starts reloads on an empty trigger pull and reports holding the trigger.
    public void tickStart(Minecraft mc) {
        if (!inGame(mc)) {
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null) {
            this.shooting = false;
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof GunItem gunItem && GunKeys.shoot().isDown() && !GunComponents.hasAmmo(held)) {
            Gun gun = gunItem.getGun(true);
            if (GunComponents.ammo(held) <= 0 && !player.isCreative() && gun.reloads().reloadType() != ReloadType.INVENTORY_FED) {
                ReloadClient.get().setReloading(true);
            }
        }
        if (held.getItem() instanceof GunItem gunItem && (GunComponents.hasAmmo(held) || player.isCreative())) {
            Gun gun = gunItem.getGun(true);
            boolean pulling = GunKeys.shoot().isDown()
                || this.burstCount > 0 && gun.general().fireMode() == FireMode.BURST;
            if (pulling) {
                if (!this.shooting) {
                    this.shooting = true;
                    ClientPlayNetworking.send(new GunFlagPayload(GunFlagPayload.Flag.SHOOTING, true));
                }
            } else if (this.shooting) {
                this.shooting = false;
                ClientPlayNetworking.send(new GunFlagPayload(GunFlagPayload.Flag.SHOOTING, false));
            }
        } else if (this.shooting) {
            this.shooting = false;
            ClientPlayNetworking.send(new GunFlagPayload(GunFlagPayload.Flag.SHOOTING, false));
        }
    }

    // End of tick: fires by mode, runs fire and hold timers, and handles overheating.
    public void tickEnd(Minecraft mc) {
        if (!inGame(mc)) {
            return;
        }
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        boolean down = GunKeys.shoot().isDown();
        if (!down) {
            this.fireTimer = 0;
            if (!this.previouslyPressed) {
                this.holdFire = 0;
            } else if (this.holdFire > 0) {
                this.holdFire--;
            }
            if (this.overheatTimer > 0) {
                this.overheatTimer--;
            }
        }
        if (this.overheated && this.overheatTimer <= 0) {
            this.overheated = false;
        }
        if (player.isUnderWater()) {
            this.overheatTimer = 0;
            this.overheated = false;
        }
        if (this.overheated) {
            GunKeys.shoot().setDown(false);
        }
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GunItem gunItem)) {
            this.previouslyPressed = false;
            return;
        }
        Gun gun = gunItem.getGun(true);
        FireMode mode = gun.general().fireMode();
        if (gun.general().overheatTimer() == 0) {
            this.overheated = false;
        }
        if (!GunKeys.shoot().isDown() && mode == FireMode.RELEASE_FIRE && this.holdFire > 5 && this.previouslyPressed) {
            this.fire(player, held);
        }
        boolean burstPending = this.burstCount > 0 && mode == FireMode.BURST;
        if ((GunKeys.shoot().isDown() || burstPending) && mode != FireMode.RELEASE_FIRE && mode != FireMode.SEMI_AUTOMATIC && mode != FireMode.PULSE) {
            this.fire(player, held);
            if (mode != FireMode.AUTOMATIC && mode != FireMode.BURST) {
                GunKeys.shoot().setDown(false);
            }
        }
        if (!GunComponents.hasAmmo(held) && !player.isCreative()) {
            GunKeys.shoot().setDown(false);
            this.burstCount = 0;
        }
        if (!GunKeys.shoot().isDown()) {
            return;
        }
        ItemCooldowns cooldowns = player.getCooldowns();
        if (gun.general().overheatTimer() != 0 && this.overheatTimer >= gun.general().overheatTimer()) {
            cooldowns.addCooldown(held, 80);
            this.overheated = true;
            GunKeys.shoot().setDown(false);
            ClientPlayNetworking.send(new GunActionPayload(GunActionPayload.Action.OVERHEAT));
        }
        if (gun.general().maxHoldFire() != 0 && this.holdFire < gun.general().maxHoldFire() && !cooldowns.isOnCooldown(held)) {
            this.previouslyPressed = true;
            this.holdFire++;
        }
        if (gun.general().fireTimer() != 0) {
            if (this.fireTimer < gun.general().fireTimer() && !cooldowns.isOnCooldown(held)) {
                if (this.fireTimer == 2) {
                    ClientPlayNetworking.send(new GunActionPayload(GunActionPayload.Action.PRE_FIRE_SOUND));
                }
                this.fireTimer++;
                if (player.isUnderWater()) {
                    this.fireTimer++;
                }
            } else {
                this.fire(player, held);
                if (mode == FireMode.SEMI_AUTOMATIC || mode == FireMode.PULSE) {
                    GunKeys.shoot().setDown(false);
                    this.fireTimer = 0;
                }
            }
        } else {
            if (mode != FireMode.RELEASE_FIRE) {
                this.fire(player, held);
            }
            if (mode == FireMode.SEMI_AUTOMATIC) {
                GunKeys.shoot().setDown(false);
            }
        }
    }

    // Fires once if the gun is off cooldown, setting the fire-rate cooldown and burst count and telling the server.
    public void fire(LocalPlayer player, ItemStack held) {
        if (!(held.getItem() instanceof GunItem gunItem) || player.isSpectator()) {
            return;
        }
        if (!GunComponents.hasAmmo(held) && !player.isCreative()) {
            this.burstCount = 0;
            return;
        }
        if (player.getUseItem().getItem() instanceof ShieldItem) {
            return;
        }
        this.holdFire = 0;
        this.previouslyPressed = false;
        ItemCooldowns cooldowns = player.getCooldowns();
        if (cooldowns.isOnCooldown(held)) {
            return;
        }
        Gun gun = gunItem.getGun(true);
        if (!GunFireClient.canFire(player, held, gun)) {
            return;
        }
        if (held.isDamageableItem() && held.getDamageValue() >= held.getMaxDamage() - 1) {
            return;
        }
        cooldowns.addCooldown(held, gun.general().rate());
        if (gun.general().fireMode() == FireMode.BURST) {
            if (this.burstCount == 1) {
                cooldowns.addCooldown(held, gun.general().burstDelay());
            }
            this.burstCount = this.burstCount <= 0 ? gun.general().burstAmount() - 1 : this.burstCount - 1;
        }
        if (!GunAnimationDriver.get().isPlaying(held, "draw")) {
            this.overheatTimer++;
        }
        ClientPlayNetworking.send(new ShootPayload(player.getYRot(), player.getXRot()));
        GunFireClient.afterFire(player, held, gun);
        GunState.setReloading(player, false);
    }

    // Whether the gun in hand is still being drawn, which blocks firing.
    public static boolean drawing(LocalPlayer player) {
        return DrawTracker.get(player).isDrawing(player, true);
    }
}
