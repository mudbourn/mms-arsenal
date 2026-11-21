package info.mudbourn.mmsarsenal.client.armory.mixin;

import net.spell_engine.client.input.Keybindings;
import net.spell_engine.client.input.WrappedKeybinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

// Never falls back to vanilla hotbar keys; the third skill key falls back to right mouse.
@Mixin(Keybindings.Wrapped.class)
public abstract class SpellKeyFallbackMixin {

    @Inject(method = "all", at = @At("HEAD"), cancellable = true)
    private static void mmsArsenal$skillKeys(CallbackInfoReturnable<List<WrappedKeybinding>> cir) {
        cir.setReturnValue(List.of(
            new WrappedKeybinding(Keybindings.spell_hotbar_1, WrappedKeybinding.VanillaAlternative.NONE),
            new WrappedKeybinding(Keybindings.spell_hotbar_2, WrappedKeybinding.VanillaAlternative.NONE),
            new WrappedKeybinding(Keybindings.spell_hotbar_3, WrappedKeybinding.VanillaAlternative.USE_KEY),
            new WrappedKeybinding(Keybindings.spell_hotbar_4, WrappedKeybinding.VanillaAlternative.NONE),
            new WrappedKeybinding(Keybindings.spell_hotbar_5, WrappedKeybinding.VanillaAlternative.NONE),
            new WrappedKeybinding(Keybindings.spell_hotbar_6, WrappedKeybinding.VanillaAlternative.NONE),
            new WrappedKeybinding(Keybindings.spell_hotbar_7, WrappedKeybinding.VanillaAlternative.NONE),
            new WrappedKeybinding(Keybindings.spell_hotbar_8, WrappedKeybinding.VanillaAlternative.NONE),
            new WrappedKeybinding(Keybindings.spell_hotbar_9, WrappedKeybinding.VanillaAlternative.NONE)
        ));
    }
}
