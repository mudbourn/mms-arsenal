package info.mudbourn.mmsarsenal.client.gun.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import info.mudbourn.mmsarsenal.client.gun.AimHandler;
import info.mudbourn.mmsarsenal.client.gun.GunView;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Zooms the world FOV while aiming and rolls the camera when strafing with a gun.
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @ModifyReturnValue(method = "getFov", at = @At("RETURN"))
    private float mmsArsenal$aimFov(float fov, Camera camera, float partialTick, boolean useFovSetting) {
        return useFovSetting ? AimHandler.get().modifyFov(fov) : fov;
    }

    @Inject(
        method = "renderLevel",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V")
    )
    private void mmsArsenal$cameraRoll(DeltaTracker delta, CallbackInfo ci, @Local PoseStack poseStack) {
        poseStack.mulPose(Axis.ZP.rotationDegrees(GunView.get().cameraRoll(delta.getGameTimeDeltaPartialTick(true))));
    }
}
