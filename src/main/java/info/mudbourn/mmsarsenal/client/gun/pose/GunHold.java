package info.mudbourn.mmsarsenal.client.gun.pose;

import info.mudbourn.mmsarsenal.gun.GripType;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;

// How a player renders holding a gun this frame: grip, look pitch, aim, handedness and crouch.
public record GunHold(GripType grip, float pitch, float aim, boolean rightHanded, boolean crouching, boolean restingArms) {

    public static final RenderStateDataKey<GunHold> KEY = RenderStateDataKey.create(() -> "mms_arsenal:gun_hold");

    public GunPose pose() {
        return GunPose.of(this.grip);
    }
}
