package info.mudbourn.mmsarsenal.client.gun.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import info.mudbourn.mmsarsenal.client.gun.AimHandler;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Zooms the world FOV while aiming.
@Mixin(Camera.class)
public abstract class CameraFovMixin {

    @ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
    private float mmsArsenal$aimFov(float fov) {
        return AimHandler.get().modifyFov(fov);
    }
}
