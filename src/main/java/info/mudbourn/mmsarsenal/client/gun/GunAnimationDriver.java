package info.mudbourn.mmsarsenal.client.gun;

import info.mudbourn.mmsarsenal.config.ArsenalConfig;
import info.mudbourn.mmsarsenal.gun.DrawTracker;
import info.mudbourn.mmsarsenal.gun.FireMode;
import info.mudbourn.mmsarsenal.gun.Gun;
import info.mudbourn.mmsarsenal.gun.GunAmmo;
import info.mudbourn.mmsarsenal.gun.GunItem;
import info.mudbourn.mmsarsenal.gun.GunSounds;
import info.mudbourn.mmsarsenal.gun.GunState;
import info.mudbourn.mmsarsenal.gun.ReloadType;
import info.mudbourn.mmsarsenal.gun.net.GunActionPayload;
import info.mudbourn.mmsrendercommon.client.geo.BonePose;
import info.mudbourn.mmsrendercommon.client.geo.GeoAnimation;
import info.mudbourn.mmsrendercommon.client.geo.GeoAssets;
import info.mudbourn.mmsrendercommon.client.geo.MolangScope;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// Picks each first-person gun's animation every frame from what the player is doing, as Just Enough Guns' controller does.
public final class GunAnimationDriver {

    private static final GunAnimationDriver INSTANCE = new GunAnimationDriver();
    private static final int ACTION_TICKS = 5;

    private static final GunAnimator.Sequence IDLE = loop("idle");
    private static final GunAnimator.Sequence SHOOT = GunAnimator.Sequence.of(stage("shoot", GunAnimator.Loop.HOLD_ON_LAST_FRAME));
    private static final GunAnimator.Sequence AIM_SHOOT = GunAnimator.Sequence.of(stage("aim_shoot", GunAnimator.Loop.HOLD_ON_LAST_FRAME));
    private static final GunAnimator.Sequence HOLD_FIRE = GunAnimator.Sequence.of(
        stage("hold_fire", GunAnimator.Loop.PLAY_ONCE),
        stage("hold", GunAnimator.Loop.LOOP)
    );
    private static final GunAnimator.Sequence RELOAD = onceThenIdle("reload");
    private static final GunAnimator.Sequence RELOAD_START = GunAnimator.Sequence.of(
        stage("reload_start", GunAnimator.Loop.PLAY_ONCE),
        stage("reload_loop", GunAnimator.Loop.LOOP)
    );
    private static final GunAnimator.Sequence RELOAD_STOP = onceThenIdle("reload_stop");
    private static final GunAnimator.Sequence MELEE = onceThenIdle("melee");
    private static final GunAnimator.Sequence SPRINT = GunAnimator.Sequence.of(stage("sprint", GunAnimator.Loop.HOLD_ON_LAST_FRAME));
    private static final GunAnimator.Sequence INSPECT = onceThenIdle("inspect");
    private static final GunAnimator.Sequence DRAW = onceThenIdle("draw");

    private final Map<Item, GunAnimator> animators = new HashMap<>();
    private final Map<Item, Integer> actionTicks = new HashMap<>();
    private Action action = Action.NONE;
    private int heldSlot = -1;
    private Item heldItem;

    private enum Action {
        NONE,
        SHOOTING,
        MELEEING,
        INSPECTING
    }

    public static GunAnimationDriver get() {
        return INSTANCE;
    }

    // Counts down the short-lived shooting, melee and inspect flags, and restarts a gun's controller when it is drawn again.
    public void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        int slot = player.getInventory().getSelectedSlot();
        if (slot != this.heldSlot || held.getItem() != this.heldItem) {
            this.heldSlot = slot;
            this.heldItem = held.getItem();
            this.action = Action.NONE;
            GunAnimator animator = this.animators.get(held.getItem());
            if (animator != null) {
                animator.reset();
            }
        }
        if (this.action != Action.NONE) {
            int ticks = this.actionTicks.getOrDefault(held.getItem(), 0) + 1;
            if (ticks >= ACTION_TICKS) {
                this.action = Action.NONE;
                this.actionTicks.remove(held.getItem());
            } else {
                this.actionTicks.put(held.getItem(), ticks);
            }
        }
    }

    public void onFire(ItemStack held) {
        this.startAction(held, Action.SHOOTING);
        GunAnimator animator = this.animators.get(held.getItem());
        if (animator != null) {
            animator.forceReset();
        }
    }

    public void onMelee(ItemStack held) {
        this.startAction(held, Action.MELEEING);
    }

    // Starts the inspect animation, restarting it if it is already playing; nothing while the gun is still being drawn.
    public void onInspect(LocalPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GunItem) || DrawTracker.get(player).isDrawing(player, true)) {
            return;
        }
        this.startAction(held, Action.INSPECTING);
        GunAnimator animator = this.animators.get(held.getItem());
        if (animator != null && animator.isPlaying("inspect")) {
            animator.forceReset();
        }
    }

    private void startAction(ItemStack held, Action action) {
        this.action = action;
        this.actionTicks.put(held.getItem(), 0);
    }

    public boolean isPlaying(ItemStack held, String name) {
        GunAnimator animator = this.animators.get(held.getItem());
        return animator != null && animator.isPlaying(name);
    }

    // The local player's first-person gun pose this frame, choosing the animation and firing its keyframe events.
    public Map<String, BonePose> pose(LocalPlayer player, ItemStack held, GunItem gunItem, Identifier animationFile, double nowTicks) {
        GunAnimator animator = this.animators.computeIfAbsent(held.getItem(), item -> new GunAnimator(animations(animationFile)));
        Gun gun = gunItem.getGun(true);
        animator.setSpeed(1.0F);
        animator.set(this.choose(player, held, gun, animator), nowTicks);
        return animator.sample(
            nowTicks,
            new MolangScope(),
            sound -> this.playKeyframeSound(player, gun, sound),
            effect -> this.playKeyframeEffect(effect)
        );
    }

    // An idle pose for guns drawn anywhere but the local player's first-person hand.
    public static Map<String, BonePose> idlePose(Identifier animationFile, double nowTicks) {
        GeoAnimation idle = animations(animationFile).get("idle");
        Map<String, BonePose> poses = new HashMap<>();
        if (idle == null) {
            return poses;
        }
        float time = idle.length() <= 0.0F ? 0.0F : (float) (nowTicks / 20.0 % idle.length());
        MolangScope scope = new MolangScope();
        for (String bone : idle.animatedBones()) {
            BonePose pose = idle.sampleAt(bone, time, scope);
            if (pose != null) {
                poses.put(bone, pose);
            }
        }
        return poses;
    }

    private GunAnimator.Sequence choose(LocalPlayer player, ItemStack held, Gun gun, GunAnimator animator) {
        if (!animator.hasCurrent()) {
            return IDLE;
        }
        boolean aiming = GunState.isAiming(player);
        if (GunState.isShooting(player) && gun.general().fireMode() == FireMode.RELEASE_FIRE) {
            return HOLD_FIRE;
        }
        boolean shootAnimation = animator.isPlaying("shoot") || animator.isPlaying("aim_shoot");
        if (this.action == Action.SHOOTING || shootAnimation && animator.state() != GunAnimator.State.PAUSED) {
            return aiming ? AIM_SHOOT : SHOOT;
        }
        if (this.action == Action.MELEEING || animator.isPlaying("melee") && animator.state() != GunAnimator.State.PAUSED) {
            return MELEE;
        }
        if (!GunAmmo.lacksAmmo(player, gun)) {
            boolean reloading = GunState.isReloading(player);
            if (!reloading && animator.isPlaying("reload_loop") || animator.isPlaying("reload_stop")) {
                return RELOAD_STOP;
            }
            boolean reloadAnimation = animator.isPlaying("reload") || animator.isPlaying("reload_alt") || animator.isPlaying("reload_start");
            if (reloading || reloadAnimation && !animator.finished()) {
                return gun.reloads().reloadType() == ReloadType.MANUAL ? RELOAD_START : RELOAD;
            }
        }
        if (this.action == Action.INSPECTING || animator.isPlaying("inspect") && !animator.finished()) {
            return aiming ? IDLE : INSPECT;
        }
        boolean drawing = DrawTracker.get(player).isDrawing(player, true);
        if (ArsenalConfig.get().drawAnimation && (drawing || animator.isPlaying("draw") && !animator.finished() && !AimHandler.get().isAiming())) {
            return DRAW;
        }
        if (player.isSprinting() && !aiming) {
            return SPRINT;
        }
        if (animator.isPlaying("reload") && !animator.finished()) {
            return RELOAD;
        }
        return IDLE;
    }

    // Plays the sound a keyframe names: the gun's own reload sounds, or a generic rustle, screw or click.
    private void playKeyframeSound(LocalPlayer player, Gun gun, String name) {
        Identifier gunSound = switch (name) {
            case "reload_mag_out" -> gun.sounds().reloadStart();
            case "reload_mag_in" -> gun.sounds().reloadLoad();
            case "reload_end" -> gun.sounds().reloadEnd();
            case "ejector_pull" -> gun.sounds().ejectorPull();
            case "ejector_release" -> gun.sounds().ejectorRelease();
            default -> null;
        };
        if (gunSound != null) {
            player.playSound(GunSounds.byId(gunSound), 1.0F, 1.0F);
            return;
        }
        SoundEvent generic = switch (name) {
            case "trapdoor" -> SoundEvents.IRON_TRAPDOOR_OPEN;
            case "break" -> SoundEvents.ITEM_BREAK.value();
            case "rustle" -> GunSounds.GUN_RUSTLE;
            case "screw" -> GunSounds.GUN_SCREW;
            default -> null;
        };
        if (generic != null) {
            player.playSound(generic, 1.0F, 1.0F);
        } else if (name.equals("jammed")) {
            player.playSound(SoundEvents.ANVIL_LAND, 0.8F, 1.5F);
        }
    }

    private void playKeyframeEffect(String effect) {
        if (effect.equals("eject_casing")) {
            ClientPlayNetworking.send(new GunActionPayload(GunActionPayload.Action.CASING));
        }
    }

    private static Map<String, GeoAnimation> animations(Identifier file) {
        return GeoAssets.animations(file);
    }

    private static GunAnimator.Stage stage(String name, GunAnimator.Loop loop) {
        return new GunAnimator.Stage(name, loop);
    }

    private static GunAnimator.Sequence loop(String name) {
        return GunAnimator.Sequence.of(stage(name, GunAnimator.Loop.LOOP));
    }

    private static GunAnimator.Sequence onceThenIdle(String name) {
        return GunAnimator.Sequence.of(stage(name, GunAnimator.Loop.PLAY_ONCE), stage("idle", GunAnimator.Loop.LOOP));
    }
}
