package info.mudbourn.mmsarsenal.client.gun.mixin;

import info.mudbourn.mmsarsenal.client.gun.AimHandler;
import info.mudbourn.mmsarsenal.client.gun.pose.GunHold;
import info.mudbourn.mmsarsenal.gun.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Records how a player holds their gun for the model and hand layer, and turns the body toward the gun while the head keeps facing the look direction.
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void mmsArsenal$extractGunHold(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo ci) {
        state.setData(GunHold.KEY, null);
        if (!(avatar instanceof AbstractClientPlayer player) || !(player.getMainHandItem().getItem() instanceof GunItem gunItem)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        float pitch = mc.getCameraEntity() == player && mc.screen != null ? 0.0F : Mth.lerp(partialTick, player.xRotO, player.getXRot()) / 90.0F;
        boolean rightHanded = player.getMainArm() == HumanoidArm.RIGHT;
        float aim = AimHandler.get().progress(player, partialTick);
        boolean resting = player == mc.player && state.walkAnimationPos == 0.0F;
        GunHold hold = new GunHold(gunItem.getGun(true).general().gripType(), pitch, aim, rightHanded, player.isCrouching(), resting);
        state.setData(GunHold.KEY, hold);
        float headYaw = state.bodyRot + state.yRot;
        state.bodyRot = Mth.rotLerp(partialTick, player.yRotO, player.getYRot()) + hold.pose().bodyYawOffset(pitch, aim, rightHanded);
        // Keeps the head facing where the player looks once the body has turned toward the gun.
        state.yRot = Mth.wrapDegrees(headYaw - state.bodyRot);
    }
}
