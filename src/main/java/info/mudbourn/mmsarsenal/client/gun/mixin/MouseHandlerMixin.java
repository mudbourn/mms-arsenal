package info.mudbourn.mmsarsenal.client.gun.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import info.mudbourn.mmsarsenal.client.gun.AimHandler;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Slows the mouse while aiming down sights.
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void mmsArsenal$aimSensitivity(LocalPlayer player, double yaw, double pitch, Operation<Void> original) {
        double scale = AimHandler.get().sensitivityScale();
        original.call(player, yaw * scale, pitch * scale);
    }
}
