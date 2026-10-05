package info.mudbourn.mmsarsenal.client.gun.mixin;

import java.util.List;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Exposes the fog environment list so smoke fog can be checked ahead of the vanilla ones.
@Mixin(FogRenderer.class)
public interface FogRendererAccessor {

    @Accessor("FOG_ENVIRONMENTS")
    static List<FogEnvironment> mmsArsenal$environments() {
        throw new AssertionError();
    }
}
