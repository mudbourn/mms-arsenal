package info.mudbourn.mmsarsenal.armory.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import info.mudbourn.mmsarsenal.armory.ArmoryComponents;
import info.mudbourn.mmsarsenal.armory.ArmoryItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Swapping to a different main-hand item resets the attack bar, and Better Combat will not swing until it refills.
// The quickdraw swaps the sheathed Murasama for the drawn one, so that reset is skipped while the quickdraw is
// active: the blade can swing the instant it is out. Its own swings still reset the bar as usual.
@Mixin(Player.class)
public abstract class QuickdrawNoGateMixin {

    @WrapOperation(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;resetAttackStrengthTicker()V")
    )
    private void mmsArsenal$noGateOnQuickdraw(Player player, Operation<Void> original) {
        ItemStack held = player.getMainHandItem();
        if (held.is(ArmoryItems.MURASAMA) && held.getOrDefault(ArmoryComponents.ABILITY_TICK, 0) > 0) {
            return;
        }
        original.call(player);
    }
}
