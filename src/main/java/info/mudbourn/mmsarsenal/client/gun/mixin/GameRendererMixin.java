package info.mudbourn.mmsarsenal.client.gun.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import info.mudbourn.mmsarsenal.client.gun.GunView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Rolls the camera when strafing with a gun.
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(
        method = "renderLevel",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V")
    )
    private void mmsArsenal$cameraRoll(CallbackInfo ci, @Local PoseStack poseStack) {
        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
        poseStack.rotate(Axis.ZP.rotationDegrees(GunView.get().cameraRoll(partialTick)));
    }
}
