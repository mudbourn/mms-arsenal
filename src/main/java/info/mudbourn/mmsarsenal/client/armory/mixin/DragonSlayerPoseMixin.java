package info.mudbourn.mmsarsenal.client.armory.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import info.mudbourn.mmsarsenal.armory.item.DragonSlayerItem;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Keeps Better Combat's weapon pose on the player while they draw a Dragon Slayer focus sweep.
@Mixin(value = AbstractClientPlayer.class, priority = 1500)
public abstract class DragonSlayerPoseMixin {

    @WrapOperation(
        method = "updateAnimationsOnTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;isUsingItem()Z",
            remap = true
        ),
        remap = false
    )
    private boolean mmsArsenal$poseWhileDrawing(Player player, Operation<Boolean> original) {
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof DragonSlayerItem) {
            return false;
        }
        return original.call(player);
    }
}
