package info.mudbourn.mmsarsenal.client.gun.pose;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import info.mudbourn.mmsarsenal.gun.GripType;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

// A grip's third-person pose: arm angles, body turn and held-gun offset blended between up, forward and down looks, idle and aimed.
public abstract class GunPose {

    private static final GunPose ONE_HANDED = new OneHanded();
    private static final GunPose TWO_HANDED = new Posed(twoHandedUp(), twoHandedForward(), twoHandedDown(), true, true);
    private static final GunPose MINI_GUN = new Posed(miniGunUp(), miniGunForward(), miniGunDown(), false, false);
    private static final GunPose BAZOOKA = new Posed(bazookaUp(), bazookaForward(), bazookaDown(), false, false);

    public static GunPose of(GripType grip) {
        return switch (grip) {
            case ONE_HANDED -> ONE_HANDED;
            case TWO_HANDED -> TWO_HANDED;
            case MINI_GUN -> MINI_GUN;
            case BAZOOKA -> BAZOOKA;
        };
    }

    // Sets the arms, and for two-handed guns the head, from where the player looks and how far they aim.
    public abstract void applyArms(ModelPart rightArm, ModelPart leftArm, ModelPart head, float pitch, float aim, boolean rightHanded, boolean crouching);

    // How far the body turns from the head toward the gun, in degrees.
    public float bodyYawOffset(float pitch, float aim, boolean rightHanded) {
        return 0.0F;
    }

    // Offsets the held gun in the main hand.
    public void applyHeldItem(PoseStack poseStack, float pitch, float aim, boolean rightHanded) {
    }

    // One limb's angles in degrees and pivot in pixels; any of them may be unset and left as the model had it.
    public record Limb(Float angleX, Float angleY, Float angleZ, Float pointX, Float pointY, Float pointZ) {
    }

    // One look direction's pose: both arms, the body turn, and the held gun's offset and rotation.
    public record Stance(Limb leftArm, Limb rightArm, float yawOffset, Vector3f itemTranslate, Vector3f itemRotation) {

        static final Stance EMPTY = new Stance(limb(), limb(), 0.0F, new Vector3f(), new Vector3f());
    }

    // A look direction's idle and fully aimed stances.
    public record Aim(Stance idle, Stance aiming) {
    }

    // Grips posed by blending up, forward and down stances, as Just Enough Guns' weapon poses do.
    private static final class Posed extends GunPose {

        private final Aim up;
        private final Aim forward;
        private final Aim down;
        private final boolean aimPose;
        private final boolean tiltHead;

        Posed(Aim up, Aim forward, Aim down, boolean aimPose, boolean tiltHead) {
            this.up = up;
            this.forward = forward;
            this.down = down;
            this.aimPose = aimPose;
            this.tiltHead = tiltHead;
        }

        @Override
        public void applyArms(ModelPart rightArm, ModelPart leftArm, ModelPart head, float pitch, float aim, boolean rightHanded, boolean crouching) {
            ModelPart main = rightHanded ? rightArm : leftArm;
            ModelPart secondary = rightHanded ? leftArm : rightArm;
            float side = rightHanded ? 1.0F : -1.0F;
            float zoom = this.aimPose ? aim : 0.0F;
            Aim target = pitch > 0.0F ? this.down : this.up;
            float partial = Math.abs(pitch);
            applyLimb(target.idle().rightArm(), target.aiming().rightArm(), this.forward.idle().rightArm(), this.forward.aiming().rightArm(), main, partial, zoom, side, crouching);
            applyLimb(target.idle().leftArm(), target.aiming().leftArm(), this.forward.idle().leftArm(), this.forward.aiming().leftArm(), secondary, partial, zoom, side, crouching);
            if (this.tiltHead) {
                head.xRot = (float) Math.toRadians(pitch > 0.0F ? pitch * 70.0F : pitch * 90.0F);
            }
        }

        @Override
        public float bodyYawOffset(float pitch, float aim, boolean rightHanded) {
            Aim target = pitch > 0.0F ? this.down : this.up;
            float zoom = this.aimPose ? aim : 0.0F;
            return value(target.idle().yawOffset(), target.aiming().yawOffset(), this.forward.idle().yawOffset(), this.forward.aiming().yawOffset(), 0.0F, Math.abs(pitch), zoom, rightHanded ? 1.0F : -1.0F);
        }

        @Override
        public void applyHeldItem(PoseStack poseStack, float pitch, float aim, boolean rightHanded) {
            float side = rightHanded ? 1.0F : -1.0F;
            poseStack.translate(0.0, 0.0, 0.05);
            float partial = Math.abs(pitch);
            float zoom = this.aimPose ? aim : 0.0F;
            Aim target = pitch > 0.0F ? this.down : this.up;
            float tx = value(target.idle().itemTranslate().x(), target.aiming().itemTranslate().x(), this.forward.idle().itemTranslate().x(), this.forward.aiming().itemTranslate().x(), 0.0F, partial, zoom, 1.0F);
            float ty = value(target.idle().itemTranslate().y(), target.aiming().itemTranslate().y(), this.forward.idle().itemTranslate().y(), this.forward.aiming().itemTranslate().y(), 0.0F, partial, zoom, 1.0F);
            float tz = value(target.idle().itemTranslate().z(), target.aiming().itemTranslate().z(), this.forward.idle().itemTranslate().z(), this.forward.aiming().itemTranslate().z(), 0.0F, partial, zoom, 1.0F);
            poseStack.translate(tx * 0.0625 * side, ty * 0.0625, tz * 0.0625);
            float rx = value(target.idle().itemRotation().x(), target.aiming().itemRotation().x(), this.forward.idle().itemRotation().x(), this.forward.aiming().itemRotation().x(), 0.0F, partial, zoom, 1.0F);
            float ry = value(target.idle().itemRotation().y(), target.aiming().itemRotation().y(), this.forward.idle().itemRotation().y(), this.forward.aiming().itemRotation().y(), 0.0F, partial, zoom, 1.0F);
            float rz = value(target.idle().itemRotation().z(), target.aiming().itemRotation().z(), this.forward.idle().itemRotation().z(), this.forward.aiming().itemRotation().z(), 0.0F, partial, zoom, 1.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(rx));
            poseStack.mulPose(Axis.YP.rotationDegrees(ry * side));
            poseStack.mulPose(Axis.ZP.rotationDegrees(rz * side));
        }

        private static void applyLimb(Limb targetIdle, Limb targetAiming, Limb idle, Limb aiming, ModelPart part, float partial, float zoom, float side, boolean crouching) {
            part.xRot = (float) Math.toRadians(value(targetIdle.angleX(), targetAiming.angleX(), idle.angleX(), aiming.angleX(), part.xRot, partial, zoom, 1.0F));
            part.yRot = (float) Math.toRadians(value(targetIdle.angleY(), targetAiming.angleY(), idle.angleY(), aiming.angleY(), part.yRot, partial, zoom, side));
            part.zRot = (float) Math.toRadians(value(targetIdle.angleZ(), targetAiming.angleZ(), idle.angleZ(), aiming.angleZ(), part.zRot, partial, zoom, side));
            part.x = value(targetIdle.pointX(), targetAiming.pointX(), idle.pointX(), aiming.pointX(), part.x, partial, zoom, side);
            part.y = value(targetIdle.pointY(), targetAiming.pointY(), idle.pointY(), aiming.pointY(), part.y, partial, zoom, 1.0F) + (crouching ? 2.0F : 0.0F);
            part.z = value(targetIdle.pointZ(), targetAiming.pointZ(), idle.pointZ(), aiming.pointZ(), part.z, partial, zoom, 1.0F);
        }

        // Blends the forward value toward the up or down value by look angle, then idle toward aimed by aim; unset values keep the default.
        private static float value(Float targetIdle, Float targetAiming, Float idle, Float aiming, float fallback, float partial, float zoom, float side) {
            float start = targetIdle != null && idle != null ? (idle + (targetIdle - idle) * partial) * side : idle != null ? idle * side : fallback;
            float end = targetAiming != null && aiming != null ? (aiming + (targetAiming - aiming) * partial) * side : aiming != null ? aiming * side : fallback;
            return Mth.lerp(zoom, start, end);
        }
    }

    // A one-handed gun: the arm follows the head, raised to point forward.
    private static final class OneHanded extends GunPose {

        @Override
        public void applyArms(ModelPart rightArm, ModelPart leftArm, ModelPart head, float pitch, float aim, boolean rightHanded, boolean crouching) {
            ModelPart arm = rightHanded ? rightArm : leftArm;
            arm.xRot = head.xRot + (float) Math.toRadians(-70.0);
            arm.yRot = head.yRot;
            arm.zRot = head.zRot;
        }
    }

    private static Limb limb() {
        return new Limb(null, null, null, null, null, null);
    }

    private static Limb limb(Float angleX, Float angleY, Float angleZ, Float pointX, Float pointY, Float pointZ) {
        return new Limb(angleX, angleY, angleZ, pointX, pointY, pointZ);
    }

    private static Stance stance(float yawOffset, Vector3f translate, Vector3f rotation, Limb right, Limb left) {
        return new Stance(left, right, yawOffset, translate, rotation);
    }

    private static Aim twoHandedUp() {
        return new Aim(
            stance(45.0F, new Vector3f(), new Vector3f(60.0F, 0.0F, 10.0F), limb(-120.0F, -55.0F, null, -5.0F, 3.0F, 0.0F), limb(-160.0F, -20.0F, -30.0F, null, 2.0F, -1.0F)),
            stance(45.0F, new Vector3f(-1.0F, 0.0F, 0.0F), new Vector3f(40.0F, 0.0F, 30.0F), limb(-140.0F, -55.0F, null, -5.0F, 3.0F, 0.0F), limb(-170.0F, -20.0F, -35.0F, null, 1.0F, 0.0F))
        );
    }

    private static Aim twoHandedForward() {
        return new Aim(
            stance(45.0F, new Vector3f(), new Vector3f(30.0F, -11.0F, 0.0F), limb(-60.0F, -55.0F, 0.0F, -5.0F, 2.0F, 1.0F), limb(-65.0F, -10.0F, 5.0F, null, 2.0F, -1.0F)),
            stance(45.0F, new Vector3f(), new Vector3f(5.0F, -21.0F, 0.0F), limb(-85.0F, -65.0F, 0.0F, -5.0F, 2.0F, null), limb(-90.0F, -15.0F, 0.0F, null, 2.0F, 0.0F))
        );
    }

    private static Aim twoHandedDown() {
        return new Aim(
            stance(45.0F, new Vector3f(0.0F, -0.5F, 0.5F), new Vector3f(-15.0F, -5.0F, 0.0F), limb(-30.0F, -65.0F, 0.0F, -5.0F, 2.0F, null), limb(-5.0F, -20.0F, 20.0F, null, 5.0F, 0.0F)),
            stance(45.0F, new Vector3f(0.0F, -0.5F, 1.0F), new Vector3f(-20.0F, -5.0F, -10.0F), limb(-30.0F, -65.0F, 0.0F, -5.0F, 1.0F, null), limb(-10.0F, -20.0F, 30.0F, null, 5.0F, 0.0F))
        );
    }

    private static Aim miniGunUp() {
        return new Aim(
            stance(45.0F, new Vector3f(), new Vector3f(10.0F, 0.0F, 0.0F), limb(-100.0F, -45.0F, 0.0F, null, 2.0F, null), limb(-150.0F, 40.0F, -10.0F, null, 1.0F, null)),
            Stance.EMPTY
        );
    }

    private static Aim miniGunForward() {
        return new Aim(
            stance(45.0F, new Vector3f(), new Vector3f(), limb(-15.0F, -45.0F, 0.0F, null, 2.0F, null), limb(-45.0F, 30.0F, 0.0F, null, 2.0F, null)),
            Stance.EMPTY
        );
    }

    private static Aim miniGunDown() {
        return new Aim(
            stance(45.0F, new Vector3f(0.0F, 0.0F, 1.0F), new Vector3f(-50.0F, 0.0F, 0.0F), limb(0.0F, -45.0F, 0.0F, null, 1.0F, null), limb(-25.0F, 30.0F, 15.0F, null, 4.0F, null)),
            Stance.EMPTY
        );
    }

    private static Aim bazookaUp() {
        return new Aim(
            stance(35.0F, new Vector3f(), new Vector3f(10.0F, 0.0F, 0.0F), limb(-170.0F, -35.0F, 0.0F, null, 4.0F, -2.0F), limb(-130.0F, 65.0F, 0.0F, 3.0F, 2.0F, 1.0F)),
            Stance.EMPTY
        );
    }

    private static Aim bazookaForward() {
        return new Aim(
            stance(35.0F, new Vector3f(), new Vector3f(), limb(-90.0F, -35.0F, 0.0F, null, 2.0F, 0.0F), limb(-91.0F, 35.0F, 0.0F, 4.0F, 2.0F, 0.0F)),
            Stance.EMPTY
        );
    }

    private static Aim bazookaDown() {
        return new Aim(
            stance(35.0F, new Vector3f(), new Vector3f(), limb(-10.0F, -35.0F, 0.0F, null, 2.0F, 0.0F), limb(-10.0F, 15.0F, 30.0F, 4.0F, 2.0F, 0.0F)),
            Stance.EMPTY
        );
    }
}
