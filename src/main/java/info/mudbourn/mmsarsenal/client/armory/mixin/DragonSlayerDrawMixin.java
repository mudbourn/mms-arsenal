package info.mudbourn.mmsarsenal.client.armory.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import info.mudbourn.mmsarsenal.armory.item.DragonSlayerItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Pulls the first-person Dragon Slayer back over its draw and holds it still once the focus sweep is ready.
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class DragonSlayerDrawMixin {

    @Inject(
        method = "submitArmWithItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
            ordinal = 1
        )
    )
    private void mmsArsenal$drawDragonSlayer(PlayerRenderState state, FirstPersonHandsAndItemsRenderState hands, float partialTick, float pitch, InteractionHand hand, float swing, ItemStack stack, float equip, PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        if (!(stack.getItem() instanceof DragonSlayerItem) || !state.avatarRenderState.isUsingItem || state.avatarRenderState.useItemHand != hand) {
            return;
        }
        float held = stack.getUseDuration(Minecraft.getInstance().player) - (hands.useItemRemainingTicks - partialTick + 1.0F);
        float progress = Mth.clamp(held / DragonSlayerItem.DRAW_TICKS, 0.0F, 1.0F);
        float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? state.avatarRenderState.mainArm : state.avatarRenderState.mainArm.getOpposite();
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        poseStack.translate(side * 0.1F * eased, 0.2F * eased, 0.3F * eased);
        poseStack.rotate(Axis.XP.rotationDegrees(-35.0F * eased));
        poseStack.rotate(Axis.ZP.rotationDegrees(side * -20.0F * eased));
    }
}
