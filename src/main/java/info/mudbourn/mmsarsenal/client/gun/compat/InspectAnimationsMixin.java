package info.mudbourn.mmsarsenal.client.gun.compat;

import info.mudbourn.mmsarsenal.gun.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Stops Inspect Animations from starting an inspect while a gun is in the main hand.
@Pseudo
@Mixin(targets = "net.soundsofthesun.inspectanimations.client.AnimationHelper", remap = false)
public abstract class InspectAnimationsMixin {

    @Inject(method = "sendAnimation", at = @At("HEAD"), cancellable = true)
    private static void mmsArsenal$noGunInspect(CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.getMainHandItem().getItem() instanceof GunItem) {
            ci.cancel();
        }
    }
}
