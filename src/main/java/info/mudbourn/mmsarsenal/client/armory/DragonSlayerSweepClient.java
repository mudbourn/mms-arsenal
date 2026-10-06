package info.mudbourn.mmsarsenal.client.armory;

import info.mudbourn.mmsarsenal.armory.item.DragonSlayerItem;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.bettercombat.logic.AnimatedHand;

// Plays the Dragon Slayer focus sweep through Better Combat on the local player.
public final class DragonSlayerSweepClient {

    private DragonSlayerSweepClient() {
    }

    public static void register() {
        DragonSlayerItem.setLocalSweep(player -> {
            if (player instanceof PlayerAttackAnimatable animatable) {
                animatable.playAttackAnimation(
                    DragonSlayerItem.SWEEP_ANIMATION,
                    AnimatedHand.TWO_HANDED,
                    DragonSlayerItem.SWEEP_LENGTH,
                    DragonSlayerItem.SWEEP_UPSWING
                );
            }
        });
    }
}
