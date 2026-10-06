package info.mudbourn.mmsarsenal.armory.mixin;

import info.mudbourn.mmsarsenal.armory.ArmoryItems;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.logic.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Holds the sheathed Murasama in the katana pose without making it a Better Combat weapon.
@Mixin(value = PlayerAttackHelper.class, remap = false)
public abstract class MurasamaSheathedPoseMixin {

    private static final Pose SHEATHED_POSE = new Pose("bettercombat:pose_two_handed_katana", "");

    @Inject(method = "poseForPlayer", at = @At("HEAD"), cancellable = true)
    private static void mmsArsenal$sheathedPose(Player player, CallbackInfoReturnable<Pose> cir) {
        if (player.getMainHandItem().is(ArmoryItems.MURASAMA_SHEATHED)) {
            cir.setReturnValue(SHEATHED_POSE);
        }
    }
}
