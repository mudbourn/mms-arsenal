package info.mudbourn.mmsarsenal.gun;

import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.phys.Vec3;

// One gun's stats, read from data/<namespace>/guns/<item>.json in the Just Enough Guns format.
public record Gun(
    General general,
    Reloads reloads,
    Projectile projectile,
    PotionEffect potionEffect,
    Sounds sounds,
    Display display,
    Modules modules
) {

    public static Gun fromJson(JsonObject json) {
        return new Gun(
            General.fromJson(GsonHelper.getAsJsonObject(json, "general", new JsonObject())),
            Reloads.fromJson(GsonHelper.getAsJsonObject(json, "reloads", new JsonObject())),
            Projectile.fromJson(GsonHelper.getAsJsonObject(json, "projectile", new JsonObject())),
            PotionEffect.fromJson(GsonHelper.getAsJsonObject(json, "potionEffect", new JsonObject())),
            Sounds.fromJson(GsonHelper.getAsJsonObject(json, "sounds", new JsonObject())),
            Display.fromJson(GsonHelper.getAsJsonObject(json, "display", new JsonObject())),
            Modules.fromJson(GsonHelper.getAsJsonObject(json, "modules", new JsonObject()))
        );
    }

    public boolean canAimDownSight() {
        return this.modules.attachments().scope() != null || this.modules.zoom() != null;
    }

    // The zoom FOV modifier, or 0 when the gun has no zoom.
    public float fovModifier() {
        Zoom zoom = this.modules.zoom();
        return zoom == null ? 0.0F : zoom.fovModifier();
    }

    public record General(
        FireMode fireMode,
        int burstAmount,
        int burstDelay,
        int rate,
        int fireTimer,
        int maxHoldFire,
        int overheatTimer,
        int drawTimer,
        boolean silenced,
        GripType gripType,
        float shooterPushback,
        float recoilAngle,
        float recoilKick,
        float recoilDurationOffset,
        float recoilAdsReduction,
        int projectileAmount,
        boolean alwaysSpread,
        float spread,
        boolean canFireUnderwater
    ) {

        static General fromJson(JsonObject json) {
            return new General(
                FireMode.byId(GsonHelper.getAsString(json, "fireMode", "semi_automatic")),
                GsonHelper.getAsInt(json, "burstAmount", 0),
                GsonHelper.getAsInt(json, "burstDelay", 0),
                GsonHelper.getAsInt(json, "rate", 0),
                GsonHelper.getAsInt(json, "fireTimer", 0),
                GsonHelper.getAsInt(json, "maxHoldFire", 0),
                GsonHelper.getAsInt(json, "overheatTimer", 0),
                GsonHelper.getAsInt(json, "drawTimer", 20),
                GsonHelper.getAsBoolean(json, "silenced", false),
                GripType.byId(GsonHelper.getAsString(json, "gripType", "one_handed")),
                GsonHelper.getAsFloat(json, "shooterPushback", 0.0F),
                GsonHelper.getAsFloat(json, "recoilAngle", 0.0F),
                GsonHelper.getAsFloat(json, "recoilKick", 0.0F),
                GsonHelper.getAsFloat(json, "recoilDurationOffset", 0.0F),
                GsonHelper.getAsFloat(json, "recoilAdsReduction", 0.2F),
                GsonHelper.getAsInt(json, "projectileAmount", 1),
                GsonHelper.getAsBoolean(json, "alwaysSpread", false),
                GsonHelper.getAsFloat(json, "spread", 0.0F),
                GsonHelper.getAsBoolean(json, "canFireUnderwater", false)
            );
        }
    }

    public record Reloads(
        Identifier reloadItem,
        int maxAmmo,
        ReloadType reloadType,
        int reloadTimer,
        int additionalReloadTimer,
        int reloadAmount
    ) {

        static Reloads fromJson(JsonObject json) {
            return new Reloads(
                identifier(json, "reloadItem", "mms_arsenal:scrap"),
                GsonHelper.getAsInt(json, "maxAmmo", 30),
                ReloadType.byId(GsonHelper.getAsString(json, "reloadType", "manual")),
                GsonHelper.getAsInt(json, "reloadTimer", 20),
                GsonHelper.getAsInt(json, "additionalReloadTimer", 5),
                GsonHelper.getAsInt(json, "reloadAmount", 1)
            );
        }
    }

    public record Projectile(
        Identifier item,
        boolean ejectsCasing,
        boolean visible,
        boolean ignoresBlocks,
        boolean collateral,
        float damage,
        float headshotMultiplier,
        Identifier advantage,
        float size,
        double speed,
        int life,
        boolean gravity,
        boolean damageReduceOverLife,
        int trailColor,
        double trailLengthMultiplier,
        boolean hideTrail,
        boolean noProjectile
    ) {

        static Projectile fromJson(JsonObject json) {
            return new Projectile(
                identifier(json, "item", "mms_arsenal:pistol_ammo"),
                GsonHelper.getAsBoolean(json, "ejectsCasing", false),
                GsonHelper.getAsBoolean(json, "visible", false),
                GsonHelper.getAsBoolean(json, "ignoresBlocks", false),
                GsonHelper.getAsBoolean(json, "collateral", false),
                GsonHelper.getAsFloat(json, "damage", 0.0F),
                GsonHelper.getAsFloat(json, "headshotMultiplier", 1.5F),
                identifier(json, "advantage", "mms_arsenal:none"),
                GsonHelper.getAsFloat(json, "size", 0.0F),
                GsonHelper.getAsDouble(json, "speed", 0.0),
                GsonHelper.getAsInt(json, "life", 0),
                GsonHelper.getAsBoolean(json, "gravity", false),
                GsonHelper.getAsBoolean(json, "damageReduceOverLife", false),
                GsonHelper.getAsInt(json, "trailColor", 16765577),
                GsonHelper.getAsDouble(json, "trailLengthMultiplier", 1.0),
                GsonHelper.getAsBoolean(json, "hideTrail", false),
                GsonHelper.getAsBoolean(json, "noProjectile", false)
            );
        }

        public boolean hasProjectile() {
            return !this.noProjectile;
        }
    }

    public record PotionEffect(boolean selfApplied, Identifier effect, int strength, int duration) {

        static PotionEffect fromJson(JsonObject json) {
            return new PotionEffect(
                GsonHelper.getAsBoolean(json, "selfPotionEffect", false),
                json.has("potionEffect") ? identifier(json, "potionEffect", "") : null,
                GsonHelper.getAsInt(json, "potionEffectStrength", 0),
                GsonHelper.getAsInt(json, "potionEffectDuration", 0)
            );
        }
    }

    public record Sounds(
        Identifier fire,
        Identifier reloadStart,
        Identifier reloadLoad,
        Identifier reloadEnd,
        Identifier ejectorPull,
        Identifier ejectorRelease,
        Identifier silencedFire,
        Identifier enchantedFire,
        Identifier preFire
    ) {

        static Sounds fromJson(JsonObject json) {
            return new Sounds(
                optionalIdentifier(json, "fire"),
                optionalIdentifier(json, "reloadStart"),
                optionalIdentifier(json, "reloadLoad"),
                optionalIdentifier(json, "reloadEnd"),
                optionalIdentifier(json, "ejectorPull"),
                optionalIdentifier(json, "ejectorRelease"),
                optionalIdentifier(json, "silencedFire"),
                optionalIdentifier(json, "enchantedFire"),
                optionalIdentifier(json, "preFire")
            );
        }
    }

    // Where the muzzle flash sits and how big it is, or a null flash for a gun with none.
    public record Display(Flash flash) {

        static Display fromJson(JsonObject json) {
            JsonObject flash = GsonHelper.getAsJsonObject(json, "flash", null);
            return new Display(flash == null || flash.isEmpty() ? null : Flash.fromJson(flash));
        }
    }

    public record Flash(double size, Vec3 offset) {

        static Flash fromJson(JsonObject json) {
            return new Flash(GsonHelper.getAsDouble(json, "size", 0.5), Gun.offset(json));
        }
    }

    public record Modules(Zoom zoom, Attachments attachments) {

        static Modules fromJson(JsonObject json) {
            JsonObject zoom = GsonHelper.getAsJsonObject(json, "zoom", null);
            return new Modules(
                zoom == null ? null : Zoom.fromJson(zoom),
                Attachments.fromJson(GsonHelper.getAsJsonObject(json, "attachments", new JsonObject()))
            );
        }
    }

    public record Zoom(float fovModifier, Vec3 offset) {

        static Zoom fromJson(JsonObject json) {
            return new Zoom(GsonHelper.getAsFloat(json, "fovModifier", 0.0F), Gun.offset(json));
        }
    }

    // Attachment mount points; only the scope is used, for guns that come with one built in.
    public record Attachments(ScaledPositioned scope) {

        static Attachments fromJson(JsonObject json) {
            JsonObject scope = GsonHelper.getAsJsonObject(json, "scope", null);
            return new Attachments(scope == null ? null : ScaledPositioned.fromJson(scope));
        }
    }

    public record ScaledPositioned(Vec3 offset, double scale) {

        static ScaledPositioned fromJson(JsonObject json) {
            return new ScaledPositioned(Gun.offset(json), GsonHelper.getAsDouble(json, "scale", 1.0));
        }
    }

    private static Vec3 offset(JsonObject json) {
        return new Vec3(
            GsonHelper.getAsDouble(json, "xOffset", 0.0),
            GsonHelper.getAsDouble(json, "yOffset", 0.0),
            GsonHelper.getAsDouble(json, "zOffset", 0.0)
        );
    }

    private static Identifier identifier(JsonObject json, String key, String fallback) {
        return Identifier.parse(GsonHelper.getAsString(json, key, fallback));
    }

    private static Identifier optionalIdentifier(JsonObject json, String key) {
        return json.has(key) ? Identifier.parse(GsonHelper.getAsString(json, key)) : null;
    }
}
