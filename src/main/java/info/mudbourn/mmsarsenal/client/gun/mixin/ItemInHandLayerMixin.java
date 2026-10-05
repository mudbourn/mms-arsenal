package info.mudbourn.mmsarsenal.client.gun.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import info.mudbourn.mmsarsenal.client.gun.pose.GunHold;
import info.mudbourn.mmsarsenal.gun.GripType;
import info.mudbourn.mmsarsenal.gun.GunItem;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Holds a gun in third person by its grip, and hides the off hand behind any gun that is not one-handed.
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void mmsArsenal$gunInHand(ArmedEntityRenderState state, ItemStackRenderState itemState, ItemStack stack, HumanoidArm arm, PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        GunHold hold = state.getData(GunHold.KEY);
        if (hold == null) {
            return;
        }
        if (arm != state.mainArm) {
            if (stack.getItem() instanceof GunItem || hold.grip() != GripType.ONE_HANDED) {
                ci.cancel();
            }
            return;
        }
        if (!(stack.getItem() instanceof GunItem)) {
            return;
        }
        ci.cancel();
        if (itemState.isEmpty()) {
            return;
        }
        ItemInHandLayer<?, ?> layer = (ItemInHandLayer<?, ?>) (Object) this;
        poseStack.pushPose();
        ((ArmedModel<ArmedEntityRenderState>) layer.getParentModel()).translateToHand(state, arm, poseStack);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate((arm == HumanoidArm.LEFT ? -1 : 1) / 16.0F, 0.125F, -0.625F);
        hold.pose().applyHeldItem(poseStack, hold.pitch(), hold.aim(), hold.rightHanded());
        itemState.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
