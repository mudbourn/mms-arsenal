package info.mudbourn.mmsarsenal.client.gun;

import info.mudbourn.mmsarsenal.gun.Gun;
import info.mudbourn.mmsarsenal.gun.GunItem;
import info.mudbourn.mmsarsenal.gun.GunItems;
import info.mudbourn.mmsarsenal.gun.GunState;
import info.mudbourn.mmsarsenal.gun.GripType;
import info.mudbourn.mmsarsenal.gun.net.GunActionPayload;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

// First-person view state: sprint easing, fall sway, strafe roll, the lowered off hand, hit markers, muzzle flashes and the melee key.
public final class GunView {

    private static final GunView INSTANCE = new GunView();
    private static final double SWAY_SENSITIVITY = 0.3;
    private static final float CAMERA_ROLL_ANGLE = 1.5F;

    private final Random random = new Random();
    private final Set<Integer> muzzleFlashes = new HashSet<>();
    private final Set<Integer> drawnMuzzleFlashes = new HashSet<>();
    private final Map<Integer, Float> muzzleFlashRandom = new HashMap<>();

    private int sprintTransition;
    private int prevSprintTransition;
    private int sprintCooldown;
    private int meleeCooldown;
    private float sprintIntensity;
    private float offhandTranslate;
    private float prevOffhandTranslate;
    private float immersiveRoll;
    private float prevImmersiveRoll;
    private float fallSway;
    private float prevFallSway;
    private boolean playingHitMarker;
    private int hitMarkerTime;
    private int prevHitMarkerTime;
    private boolean hitMarkerCrit;

    public static GunView get() {
        return INSTANCE;
    }

    public void tick(Minecraft mc) {
        this.updateHitMarker();
        this.handleMeleeKey(mc);
        this.updateSprinting(mc);
        this.updateMuzzleFlash();
        this.updateOffhandTranslate(mc);
        this.updateImmersiveCamera(mc);
    }

    public void onFire(LocalPlayer player, Gun gun) {
        this.sprintTransition = 0;
        this.sprintCooldown = 20;
        if (gun.display().flash() != null) {
            this.showMuzzleFlash(player.getId());
        }
    }

    public void showMuzzleFlash(int entityId) {
        this.muzzleFlashes.add(entityId);
        this.muzzleFlashRandom.put(entityId, this.random.nextFloat());
    }

    public boolean hasMuzzleFlash(int entityId) {
        return this.muzzleFlashes.contains(entityId);
    }

    public float muzzleFlashRandom(int entityId) {
        return this.muzzleFlashRandom.getOrDefault(entityId, 0.0F);
    }

    public void playHitMarker(boolean crit) {
        this.playingHitMarker = true;
        this.hitMarkerCrit = crit;
        this.hitMarkerTime = 1;
        this.prevHitMarkerTime = 0;
    }

    private void updateHitMarker() {
        this.prevHitMarkerTime = this.hitMarkerTime;
        if (this.playingHitMarker) {
            this.hitMarkerTime++;
            if (this.hitMarkerTime > 2) {
                this.playingHitMarker = false;
                this.hitMarkerTime = 0;
            }
        } else {
            this.hitMarkerTime = 0;
        }
    }

    // Each press of the melee key bashes with the held gun.
    private void handleMeleeKey(Minecraft mc) {
        while (GunKeys.MELEE.consumeClick()) {
            if (this.meleeCooldown == 0) {
                this.meleeCooldown = 14;
            }
            if (mc.player != null) {
                this.onMeleePressed(mc.player);
            }
        }
    }

    private void onMeleePressed(LocalPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GunItem)) {
            return;
        }
        ReloadClient.get().setReloading(false);
        if (held.is(GunItems.MINIGUN)) {
            return;
        }
        ClientPlayNetworking.send(new GunActionPayload(GunActionPayload.Action.MELEE));
        if (!player.getCooldowns().isOnCooldown(held) && !ShootHandler.drawing(player)) {
            GunAnimationDriver.get().onMelee(held);
        }
    }

    // Eases toward the sprint pose while sprinting with nothing else going on, which widens the crosshair.
    private void updateSprinting(Minecraft mc) {
        this.prevSprintTransition = this.sprintTransition;
        LocalPlayer player = mc.player;
        boolean sprinting = player != null
            && player.isSprinting()
            && !GunState.isShooting(player)
            && !GunState.isReloading(player)
            && !AimHandler.get().isAiming()
            && this.sprintCooldown == 0
            && this.meleeCooldown == 0;
        if (sprinting) {
            if (this.sprintTransition < 5) {
                this.sprintTransition++;
            }
        } else if (this.sprintTransition > 0) {
            this.sprintTransition--;
        }
        if (this.sprintCooldown > 0) {
            this.sprintCooldown--;
        }
        if (this.meleeCooldown > 0) {
            this.meleeCooldown--;
        }
    }

    private void updateMuzzleFlash() {
        this.muzzleFlashes.removeAll(this.drawnMuzzleFlashes);
        this.muzzleFlashRandom.keySet().removeAll(this.drawnMuzzleFlashes);
        this.drawnMuzzleFlashes.clear();
        this.drawnMuzzleFlashes.addAll(this.muzzleFlashes);
    }

    private void updateOffhandTranslate(Minecraft mc) {
        this.prevOffhandTranslate = this.offhandTranslate;
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        Gun gun = GunItem.gun(player.getMainHandItem(), true);
        boolean down = gun != null && gun.general().gripType() != GripType.ONE_HANDED;
        this.offhandTranslate = Mth.clamp(this.offhandTranslate + (down ? -0.3F : 0.3F), 0.0F, 1.0F);
    }

    // Leans the camera with strafing and lets the gun sway with falling.
    private void updateImmersiveCamera(Minecraft mc) {
        this.prevImmersiveRoll = this.immersiveRoll;
        this.prevFallSway = this.fallSway;
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        boolean gun = player.getMainHandItem().getItem() instanceof GunItem;
        float strafe = player.input.getMoveVector().x;
        float target = gun ? strafe : 0.0F;
        float speed = strafe != 0.0F ? 0.1F : 0.15F;
        this.immersiveRoll = Mth.lerp(speed, this.immersiveRoll, target);
        float deltaY = (float) Mth.clamp(player.yo - player.getY(), -1.0, 1.0);
        deltaY = (float) (deltaY * (1.0 - AimHandler.get().normalisedProgress()));
        deltaY = deltaY * (1.0F - Mth.abs(player.getXRot()) / 90.0F);
        this.fallSway = Mth.approach(this.fallSway, deltaY * 60.0F * (float) SWAY_SENSITIVITY, 10.0F);
        float intensity = player.isSprinting() ? 0.75F : 1.0F;
        this.sprintIntensity = Mth.approach(this.sprintIntensity, intensity, 0.1F);
    }

    // The camera roll this frame from strafing with a gun out.
    public float cameraRoll(float partialTick) {
        float roll = Mth.lerp(partialTick, this.prevImmersiveRoll, this.immersiveRoll);
        roll = (float) Math.sin(roll * Math.PI / 2.0);
        return -roll * CAMERA_ROLL_ANGLE;
    }

    public float sprintTransition(float partialTick) {
        return (this.prevSprintTransition + (this.sprintTransition - this.prevSprintTransition) * partialTick) / 5.0F;
    }

    public float offhandTranslate(float partialTick) {
        return Mth.lerp(partialTick, this.prevOffhandTranslate, this.offhandTranslate);
    }

    public float fallSway(float partialTick) {
        return Mth.lerp(partialTick, this.prevFallSway, this.fallSway);
    }

    public float sprintIntensity() {
        return this.sprintIntensity;
    }

    public boolean isRenderingHitMarker() {
        return this.playingHitMarker;
    }

    public boolean hitMarkerCrit() {
        return this.hitMarkerCrit;
    }

    public float hitMarkerProgress(float partialTick) {
        return (this.prevHitMarkerTime + (this.hitMarkerTime - this.prevHitMarkerTime) * partialTick) / 2.0F;
    }
}
