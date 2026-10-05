package info.mudbourn.mmsarsenal.client.gun.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import info.mudbourn.mmsarsenal.client.gun.AimHandler;
import info.mudbourn.mmsarsenal.gun.Gun;
import info.mudbourn.mmsarsenal.gun.GunItem;
import info.mudbourn.mmsarsenal.gun.GripType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// With a gun in hand the attack button only shoots: no swings or block breaking, no off-hand use, and no clicking blocks while aiming.
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow
    public LocalPlayer player;

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void mmsArsenal$noGunAttack(CallbackInfoReturnable<Boolean> cir) {
        if (this.player != null && this.player.getMainHandItem().getItem() instanceof GunItem) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void mmsArsenal$noGunBreaking(boolean attacking, CallbackInfo ci) {
        if (this.player != null && this.player.getMainHandItem().getItem() instanceof GunItem) {
            ci.cancel();
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void mmsArsenal$noUseWhileAiming(CallbackInfo ci) {
        if (this.player != null
            && this.player.getMainHandItem().getItem() instanceof GunItem
            && AimHandler.get().isZooming()
            && AimHandler.get().isLookingAtInteractableBlock()) {
            ci.cancel();
        }
    }

    @WrapOperation(
        method = "startUseItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"
        )
    )
    private ItemStack mmsArsenal$noOffhandWithGun(LocalPlayer player, InteractionHand hand, Operation<ItemStack> original) {
        ItemStack stack = original.call(player, hand);
        if (hand != InteractionHand.OFF_HAND || !(player.getMainHandItem().getItem() instanceof GunItem gunItem)) {
            return stack;
        }
        Gun gun = gunItem.getGun(true);
        boolean shieldBlock = stack.getItem() instanceof ShieldItem && gun.general().gripType() == GripType.ONE_HANDED;
        return shieldBlock ? stack : ItemStack.EMPTY;
    }
}
