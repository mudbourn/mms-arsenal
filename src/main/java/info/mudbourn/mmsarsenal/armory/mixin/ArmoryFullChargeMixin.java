package info.mudbourn.mmsarsenal.armory.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import info.mudbourn.mmsarsenal.armory.item.ArmoryWeaponItem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Lands Armory weapon hits at full charge, since Better Combat strikes before the vanilla attack bar refills.
@Mixin(Player.class)
public abstract class ArmoryFullChargeMixin {

    @WrapOperation(
        method = {"attack", "baseDamageScaleFactor"},
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getAttackStrengthScale(F)F"
        )
    )
    private float mmsArsenal$fullCharge(Player player, float partialTick, Operation<Float> original) {
        if (player.getMainHandItem().getItem() instanceof ArmoryWeaponItem) {
            return 1.0F;
        }
        return original.call(player, partialTick);
    }
}
