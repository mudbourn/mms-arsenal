package info.mudbourn.mmsarsenal.armory.mixin;

import info.mudbourn.mmsarsenal.armory.item.DragonSlayerItem;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.logic.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Holds the Dragon Slayer upright in both hands while the player draws a focus sweep.
@Mixin(value = PlayerAttackHelper.class, remap = false)
public abstract class DragonSlayerDrawPoseMixin {

    private static final Pose DRAW_POSE = new Pose("bettercombat:pose_two_handed_sword", "");

    @Inject(method = "poseForPlayer", at = @At("HEAD"), cancellable = true)
    private static void mmsArsenal$drawPose(Player player, CallbackInfoReturnable<Pose> cir) {
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof DragonSlayerItem) {
            cir.setReturnValue(DRAW_POSE);
        }
    }
}
