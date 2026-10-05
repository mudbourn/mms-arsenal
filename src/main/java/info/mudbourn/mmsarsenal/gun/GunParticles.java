package info.mudbourn.mmsarsenal.gun;

import info.mudbourn.mmsarsenal.MmsArsenal;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

// Particles for casings, round trails, sonic rings, impacts, explosions and smoke.
public final class GunParticles {

    public static final SimpleParticleType CASING = register("casing");
    public static final SimpleParticleType SPECTRE_CASING = register("spectre_casing");
    public static final SimpleParticleType SCRAP = register("scrap");
    public static final SimpleParticleType GHOST_GLINT = register("ghost_glint");
    public static final SimpleParticleType SOUL_LAVA = register("soul_lava");
    public static final SimpleParticleType SONIC_RING = register("sonic_ring");
    public static final SimpleParticleType BIG_SONIC_RING = register("big_sonic_ring");
    public static final SimpleParticleType SPARK = register("spark");
    public static final SimpleParticleType BIG_EXPLOSION = register("big_explosion");
    public static final SimpleParticleType SMALL_EXPLOSION = register("small_explosion");
    public static final SimpleParticleType SMOKE = register("smoke");
    public static final SimpleParticleType SMOKE_CLOUD = register("smoke_cloud");
    public static final SimpleParticleType SMOKE_EFFECT = register("smoke_effect");
    public static final SimpleParticleType FIRE = register("fire");
    public static final SimpleParticleType FLAME = register("flame");

    private GunParticles() {
    }

    public static void register() {
    }

    private static SimpleParticleType register(String path) {
        return Registry.register(BuiltInRegistries.PARTICLE_TYPE, MmsArsenal.id(path), FabricParticleTypes.simple(true));
    }
}
