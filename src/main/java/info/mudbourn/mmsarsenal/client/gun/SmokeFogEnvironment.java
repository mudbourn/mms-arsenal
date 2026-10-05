package info.mudbourn.mmsarsenal.client.gun;

import info.mudbourn.mmsarsenal.client.gun.mixin.FogRendererAccessor;
import info.mudbourn.mmsarsenal.gun.GunMobEffects;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.MobEffectFogEnvironment;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

// Thick grey fog while Smoked, so nothing outside a couple of blocks shows through a smoke grenade's cloud.
public final class SmokeFogEnvironment extends MobEffectFogEnvironment {

    private static final int SMOKE_COLOR = 0xFF8C8C8C;
    // How far you can see at the heart of the smoke, in blocks.
    private static final float SIGHT = 2.5F;

    public static void register() {
        FogRendererAccessor.mmsArsenal$environments().addFirst(new SmokeFogEnvironment());
    }

    @Override
    public Holder<MobEffect> getMobEffect() {
        return GunMobEffects.SMOKED;
    }

    @Override
    public void setupFog(FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker) {
        if (!(camera.entity() instanceof LivingEntity entity)) {
            return;
        }
        if (!entity.hasEffect(GunMobEffects.SMOKED)) {
            return;
        }
        float end = SIGHT;
        fog.environmentalStart = 0.0F;
        fog.environmentalEnd = end;
        fog.skyEnd = end;
        fog.cloudEnd = end;
    }

    @Override
    public boolean providesColor() {
        return true;
    }

    @Override
    public int getBaseColor(ClientLevel level, Camera camera, int renderDistance, float partialTick) {
        return SMOKE_COLOR;
    }

    @Override
    public boolean modifiesDarkness() {
        return false;
    }
}
