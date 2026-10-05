package info.mudbourn.mmsarsenal.client.gun.mixin;

import info.mudbourn.mmsarsenal.client.gun.pose.GunHold;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Poses a player's arms, and head for two-handed guns, around the gun they hold.
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void mmsArsenal$gunArms(AvatarRenderState state, CallbackInfo ci) {
        GunHold hold = state.getData(GunHold.KEY);
        if (hold == null) {
            return;
        }
        PlayerModel model = (PlayerModel) (Object) this;
        if (hold.restingArms()) {
            model.rightArm.setRotation(0.0F, 0.0F, 0.0F);
            model.leftArm.setRotation(0.0F, 0.0F, 0.0F);
            return;
        }
        hold.pose().applyArms(model.rightArm, model.leftArm, model.head, hold.pitch(), hold.aim(), hold.rightHanded(), hold.crouching());
    }
}
