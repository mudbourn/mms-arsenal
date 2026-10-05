package info.mudbourn.mmsarsenal.gun;

import info.mudbourn.mmsarsenal.MmsArsenal;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Player;

// Per-player gun state synced to every client: aiming, holding the trigger, and reloading.
public final class GunState {

    public static final AttachmentType<Boolean> AIMING = flag("aiming");
    public static final AttachmentType<Boolean> SHOOTING = flag("shooting");
    public static final AttachmentType<Boolean> RELOADING = flag("reloading");

    private GunState() {
    }

    public static void register() {
    }

    public static boolean isAiming(Player player) {
        return player.getAttachedOrElse(AIMING, false);
    }

    public static boolean isShooting(Player player) {
        return player.getAttachedOrElse(SHOOTING, false);
    }

    public static boolean isReloading(Player player) {
        return player.getAttachedOrElse(RELOADING, false);
    }

    public static void setAiming(Player player, boolean aiming) {
        player.setAttached(AIMING, aiming);
    }

    public static void setShooting(Player player, boolean shooting) {
        player.setAttached(SHOOTING, shooting);
    }

    public static void setReloading(Player player, boolean reloading) {
        player.setAttached(RELOADING, reloading);
    }

    private static AttachmentType<Boolean> flag(String path) {
        return AttachmentRegistry.create(
            MmsArsenal.id(path),
            builder -> builder
                .initializer(() -> false)
                .syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.all())
        );
    }
}
