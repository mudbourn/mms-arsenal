package info.mudbourn.mmsarsenal.client.gun;

import info.mudbourn.mmsarsenal.gun.DrawTracker;
import info.mudbourn.mmsarsenal.gun.Gun;
import info.mudbourn.mmsarsenal.gun.GunItem;
import info.mudbourn.mmsarsenal.gun.GunState;
import info.mudbourn.mmsarsenal.gun.GripType;
import info.mudbourn.mmsarsenal.gun.net.GunFlagPayload;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

// Aiming down sights: the local player's aim progress, every other player's, the zoomed FOV and the slower mouse.
public final class AimHandler {

    private static final AimHandler INSTANCE = new AimHandler();
    private static final double MAX_AIM_PROGRESS = 5.0;
    private static final double AIM_SENSITIVITY = 0.75;

    private final AimTracker local = new AimTracker();
    private final Map<Player, AimTracker> others = new WeakHashMap<>();
    private double normalisedProgress;
    private boolean aiming;

    public static AimHandler get() {
        return INSTANCE;
    }

    // Updates the local player's aim at the start of each client tick and tells the server when it starts or stops.
    public void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        if (this.isAiming()) {
            if (!mc.options.keySprint.isDown()) {
                player.setSprinting(false);
            }
            if (!this.aiming) {
                GunState.setAiming(player, true);
                ClientPlayNetworking.send(new GunFlagPayload(GunFlagPayload.Flag.AIMING, true));
                this.aiming = true;
            }
            if (this.normalisedProgress > 0.0 && this.normalisedProgress <= 0.2) {
                player.playSound(SoundEvents.SPYGLASS_USE, 1.0F, 1.0F);
            }
        } else {
            if (this.normalisedProgress > 0.8 && this.normalisedProgress < 1.0) {
                player.playSound(SoundEvents.SPYGLASS_USE, 1.0F, 0.8F);
            }
            if (this.aiming) {
                GunState.setAiming(player, false);
                ClientPlayNetworking.send(new GunFlagPayload(GunFlagPayload.Flag.AIMING, false));
                this.aiming = false;
            }
        }
        this.local.update(player, true);
    }

    // Advances the aim of every other player seen aiming.
    public void tickOthers(Minecraft mc) {
        if (mc.level == null) {
            this.others.clear();
            return;
        }
        for (Player player : mc.level.players()) {
            if (player == mc.player) {
                continue;
            }
            AimTracker tracker = this.others.get(player);
            if (tracker == null && GunState.isAiming(player)) {
                tracker = new AimTracker();
                this.others.put(player, tracker);
            }
            if (tracker != null) {
                tracker.update(player, false);
                if (!tracker.isAiming()) {
                    this.others.remove(player);
                }
            }
        }
    }

    // Caches the smoothed aim progress for this frame.
    public void updateFrame(float partialTick) {
        this.normalisedProgress = this.local.progress(partialTick);
    }

    public float progress(Player player, float partialTick) {
        if (player == Minecraft.getInstance().player) {
            return (float) this.local.progress(partialTick);
        }
        AimTracker tracker = this.others.get(player);
        return tracker == null ? 0.0F : (float) tracker.progress(partialTick);
    }

    public double normalisedProgress() {
        return this.normalisedProgress;
    }

    public boolean isZooming() {
        return this.aiming;
    }

    // Whether the local player is holding aim on a gun that can aim and is free to.
    public boolean isAiming() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator() || mc.screen != null) {
            return false;
        }
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GunItem gunItem)) {
            return false;
        }
        Gun gun = gunItem.getGun(true);
        if (!gun.canAimDownSight() || DrawTracker.get(player).isDrawing(player, true)) {
            return false;
        }
        if (player.getOffhandItem().getItem() instanceof ShieldItem && gun.general().gripType() == GripType.ONE_HANDED) {
            return false;
        }
        if (!this.local.isAiming() && this.isLookingAtInteractableBlock()) {
            return false;
        }
        return !GunState.isReloading(player) && GunKeys.aim().isDown();
    }

    // Whether the crosshair is on something right-click should use instead of aiming.
    public boolean isLookingAtInteractableBlock() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.hitResult instanceof BlockHitResult result && mc.level != null) {
            BlockState state = mc.level.getBlockState(result.getBlockPos());
            Block block = state.getBlock();
            if (state.is(Blocks.IRON_DOOR) || state.is(Blocks.IRON_TRAPDOOR)) {
                return false;
            }
            return block instanceof EntityBlock
                || block == Blocks.CRAFTING_TABLE
                || state.is(BlockTags.DOORS)
                || state.is(BlockTags.TRAPDOORS)
                || state.is(BlockTags.FENCE_GATES);
        }
        return mc.hitResult instanceof EntityHitResult result && result.getEntity() instanceof ItemFrame;
    }

    // The world FOV narrowed by the gun's zoom while aiming in first person.
    public float modifyFov(float fov) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.getCameraType() != CameraType.FIRST_PERSON || this.normalisedProgress == 0.0) {
            return fov;
        }
        Gun gun = GunItem.gun(player.getMainHandItem(), true);
        if (gun == null || gun.modules().zoom() == null || GunState.isReloading(player)) {
            return fov;
        }
        double time = GunSights.sight(player.getMainHandItem()).fovCurve().apply(this.normalisedProgress);
        float modifier = (1.0F - GunSights.fovModifier(player.getMainHandItem(), gun)) * (float) time;
        return fov - fov * modifier;
    }

    // The mouse scale while aiming: slower in proportion to aim progress and slower still through strong zoom.
    public double sensitivityScale() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        float additional = 1.0F;
        if (player != null && mc.options.getCameraType() == CameraType.FIRST_PERSON && this.isAiming() && !GunState.isReloading(player)) {
            Gun gun = GunItem.gun(player.getMainHandItem(), true);
            if (gun != null && gun.modules().zoom() != null) {
                float modifier = GunSights.fovModifier(player.getMainHandItem(), gun);
                additional = Mth.clamp(1.0F - 1.0F / modifier / 10.0F, 0.0F, 1.0F);
            }
        }
        return (1.0 - (1.0 - AIM_SENSITIVITY) * this.normalisedProgress) * additional;
    }

    // One player's aim, counting up to full over five ticks at the gun's aim speed and back down.
    private final class AimTracker {

        private double current;
        private double previous;

        void update(Player player, boolean local) {
            this.previous = this.current;
            boolean wants = GunState.isAiming(player) || local && AimHandler.this.isAiming();
            double speed = GunSights.aimSpeed(player.getMainHandItem());
            if (wants) {
                this.current = Math.min(MAX_AIM_PROGRESS, this.current + speed);
            } else {
                this.current = Math.max(0.0, this.current - speed);
            }
        }

        boolean isAiming() {
            return this.current != 0.0 || this.previous != 0.0;
        }

        double progress(float partialTick) {
            return Mth.clamp((this.previous + (this.current - this.previous) * partialTick) / MAX_AIM_PROGRESS, 0.0, 1.0);
        }
    }
}
