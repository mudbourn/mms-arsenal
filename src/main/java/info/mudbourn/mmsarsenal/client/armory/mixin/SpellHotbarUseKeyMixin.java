package info.mudbourn.mmsarsenal.client.armory.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.spell_engine.client.input.SpellHotbar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Keeps the first skill on its own key instead of moving it onto right mouse.
@Mixin(SpellHotbar.class)
public abstract class SpellHotbarUseKeyMixin {

    @ModifyExpressionValue(
        method = "update",
        at = @At(value = "FIELD", target = "Lnet/spell_engine/config/ClientConfig;spellHotbarUseKey:Z")
    )
    private boolean mmsArsenal$noUseKeyShift(boolean original) {
        return false;
    }
}
