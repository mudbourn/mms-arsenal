package info.mudbourn.mmsarsenal.client.gun;

import info.mudbourn.mmsarsenal.gun.Gun;
import info.mudbourn.mmsarsenal.gun.GunItems;
import java.util.function.DoubleUnaryOperator;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

// Sight data for aiming: the easing curves, the built-in scope some guns carry, zoom, and aim speed.
public final class GunSights {

    // A gun's origin in model pixels, as Just Enough Guns places every gun model.
    public static final Vec3 GUN_ORIGIN = new Vec3(8.0, 0.0, 8.0);

    // The easing curves that shape the move to the sight, the zoom, and the tilt on the way.
    public record Sight(Easing viewportCurve, Easing sightCurve, Easing fovCurve, Easing aimTransformCurve) {
    }

    // A scope mounted on the gun: its zoom, aim speed factor, model, camera point, and whether it hides the gun behind an overlay.
    public record Scope(float fovModifier, double aimSpeed, String model, Vec3 origin, Vec3 camera, boolean overlay, Sight sight) {
    }

    public enum Easing {
        LINEAR(t -> t),
        EASE_IN_QUAD(t -> t * t),
        EASE_OUT_QUAD(t -> 1.0 - (1.0 - t) * (1.0 - t)),
        EASE_OUT_CUBIC(t -> 1.0 - Math.pow(1.0 - t, 3.0));

        private final DoubleUnaryOperator curve;

        Easing(DoubleUnaryOperator curve) {
            this.curve = curve;
        }

        public double apply(double t) {
            return this.curve.applyAsDouble(t);
        }
    }

    public static final Sight DEFAULT_SIGHT = new Sight(Easing.LINEAR, Easing.EASE_OUT_QUAD, Easing.LINEAR, Easing.EASE_IN_QUAD);

    public static final Scope TELESCOPIC_SIGHT = new Scope(
        0.2F,
        0.76,
        "telescopic_sight",
        new Vec3(8.0, 8.0, 8.0),
        new Vec3(8.0, 9.3, 30.0),
        true,
        new Sight(Easing.EASE_OUT_CUBIC, Easing.EASE_OUT_QUAD, Easing.EASE_OUT_QUAD, Easing.EASE_IN_QUAD)
    );

    private GunSights() {
    }

    // The scope a gun comes with, or null for iron sights.
    public static Scope scope(ItemStack stack) {
        return stack.is(GunItems.BOLT_ACTION_RIFLE) ? TELESCOPIC_SIGHT : null;
    }

    public static Sight sight(ItemStack stack) {
        Scope scope = scope(stack);
        return scope == null ? DEFAULT_SIGHT : scope.sight();
    }

    // The FOV multiplier at full aim: the scope's, or the gun's own zoom.
    public static float fovModifier(ItemStack stack, Gun gun) {
        Scope scope = scope(stack);
        if (scope != null && scope.fovModifier() < 1.0F) {
            return Mth.clamp(scope.fovModifier(), 0.01F, 1.0F);
        }
        return gun.fovModifier();
    }

    // Aim progress gained per tick, out of five for full aim.
    public static double aimSpeed(ItemStack stack) {
        Scope scope = scope(stack);
        return Mth.clamp(scope == null ? 1.0 : scope.aimSpeed(), 0.01, Double.MAX_VALUE);
    }

    // The point the camera lines up with when aiming down iron sights, in model pixels.
    public static Vec3 ironSightCamera(Gun gun) {
        Gun.Zoom zoom = gun.modules().zoom();
        if (zoom == null) {
            return Vec3.ZERO;
        }
        return new Vec3(8.0 - zoom.offset().x, zoom.offset().y, 8.0 - zoom.offset().z);
    }

    // Where the scope mount sits on the gun, in model pixels.
    public static Vec3 scopePosition(Gun gun) {
        Gun.ScaledPositioned scope = gun.modules().attachments().scope();
        return scope == null ? Vec3.ZERO : scope.offset().add(GUN_ORIGIN);
    }

    public static double scopeScale(Gun gun) {
        Gun.ScaledPositioned scope = gun.modules().attachments().scope();
        return scope == null ? 1.0 : scope.scale();
    }
}
