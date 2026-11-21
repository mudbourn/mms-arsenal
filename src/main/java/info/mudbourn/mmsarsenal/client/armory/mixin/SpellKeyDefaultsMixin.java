package info.mudbourn.mmsarsenal.client.armory.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.InputConstants;
import net.spell_engine.client.input.Keybindings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Defaults the first two Spell Engine skill keys to R and Z.
@Mixin(Keybindings.class)
public abstract class SpellKeyDefaultsMixin {

    @ModifyExpressionValue(
        method = "hotbarKey",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/InputConstants$Key;getValue()I")
    )
    private static int mmsArsenal$skillKeyDefault(int original, @Local(argsOnly = true) int number) {
        return switch (number) {
            case 1 -> InputConstants.KEY_R;
            case 2 -> InputConstants.KEY_Z;
            default -> original;
        };
    }
}
