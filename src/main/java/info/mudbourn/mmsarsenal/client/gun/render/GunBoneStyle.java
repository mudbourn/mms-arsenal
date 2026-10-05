package info.mudbourn.mmsarsenal.client.gun.render;

import info.mudbourn.mmsrendercommon.client.geo.GeoModel;
import java.util.Set;
import net.minecraft.client.renderer.LightTexture;

// Which gun bones show: no attachment parts, arm stand-ins hidden but their children kept, sights swapped for a scope, rounds by ammo left, and glowing parts lit.
public final class GunBoneStyle implements GeoModel.BoneStyle {

    private static final Set<String> ARMS = Set.of("left_arm", "right_arm", "fake_left_arm", "fake_right_arm");
    private static final Set<String> NO_ATTACHMENT = Set.of(
        "modified_iron_sight",
        "tactical_handguard",
        "light_handguard",
        "makeshift_stock",
        "light_stock",
        "tactical_stock",
        "weighted_stock",
        "silencer",
        "light_grip",
        "vertical_grip",
        "angled_grip",
        "extended_mag",
        "extended_mag_2",
        "drum_mag",
        "drum_mag_2"
    );

    private final GunFrame frame;

    public GunBoneStyle(GunFrame frame) {
        this.frame = frame;
    }

    @Override
    public boolean hidden(String bone) {
        boolean scoped = this.frame.scope() != null;
        if (ARMS.contains(bone) || NO_ATTACHMENT.contains(bone)) {
            return true;
        }
        if (bone.equals("railing") || bone.equals("hidden_iron_sight")) {
            return !scoped;
        }
        if (bone.equals("iron_sight") || bone.equals("stock_iron_sight")) {
            return scoped;
        }
        if (bone.startsWith("aim_hide") && this.frame.aiming()) {
            return true;
        }
        if (bone.startsWith("empty_hide") && this.frame.ammo() == 0) {
            return true;
        }
        if (bone.startsWith("bullet_") && bone.length() == 8 && Character.isDigit(bone.charAt(7))) {
            return this.frame.ammo() < bone.charAt(7) - '0';
        }
        return false;
    }

    @Override
    public boolean keepsChildren(String bone) {
        return ARMS.contains(bone);
    }

    @Override
    public int light(String bone, int light) {
        return bone.startsWith("glow") ? LightTexture.FULL_BRIGHT : light;
    }
}
