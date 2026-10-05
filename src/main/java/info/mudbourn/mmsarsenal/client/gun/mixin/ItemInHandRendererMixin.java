package info.mudbourn.mmsarsenal.client.gun.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import info.mudbourn.mmsarsenal.client.gun.AimHandler;
import info.mudbourn.mmsarsenal.client.gun.GunView;
import info.mudbourn.mmsarsenal.gun.Gun;
import info.mudbourn.mmsarsenal.gun.GunItem;
import info.mudbourn.mmsarsenal.gun.GripType;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Drops the off-hand item out of view behind a two-handed gun, and out of the way of the sights while aiming a one-handed one.
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void mmsArsenal$lowerOffhand(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swing, ItemStack stack, float equip, PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        poseStack.pushPose();
        if (hand != InteractionHand.OFF_HAND || !(player.getMainHandItem().getItem() instanceof GunItem gunItem)) {
            return;
        }
        float lowered = 1.0F - GunView.get().offhandTranslate(partialTick);
        poseStack.translate(0.0F, lowered * -0.6F, 0.0F);
        Gun gun = gunItem.getGun(true);
        if (gun.general().gripType() == GripType.ONE_HANDED) {
            poseStack.translate(0.0, -1.0 * AimHandler.get().normalisedProgress(), 0.0);
        }
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"))
    private void mmsArsenal$restoreOffhand(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swing, ItemStack stack, float equip, PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        poseStack.popPose();
    }
}
