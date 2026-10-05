package info.mudbourn.mmsarsenal.gun.entity;

import info.mudbourn.mmsarsenal.MmsArsenal;
import info.mudbourn.mmsarsenal.gun.throwable.GrenadeEntity;
import info.mudbourn.mmsarsenal.gun.throwable.MolotovCocktailEntity;
import info.mudbourn.mmsarsenal.gun.throwable.SmokeGrenadeEntity;
import info.mudbourn.mmsarsenal.gun.throwable.StunGrenadeEntity;
import info.mudbourn.mmsarsenal.gun.throwable.TimedThrowableEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

// Projectile entity types: bullets stay on the server and are drawn only as client-side trails; rockets and thrown grenades are tracked so clients see them.
public final class GunEntities {

    public static final EntityType<ProjectileEntity> PROJECTILE = bullet("projectile", ProjectileEntity::new);
    public static final EntityType<BlazeProjectileEntity> BLAZE_PROJECTILE = bullet("blaze_projectile", BlazeProjectileEntity::new);
    public static final EntityType<SpectreProjectileEntity> SPECTRE_PROJECTILE = bullet("spectre_projectile", SpectreProjectileEntity::new);
    public static final EntityType<RocketEntity> ROCKET = register(
        "rocket",
        EntityType.Builder.<RocketEntity>of(RocketEntity::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .clientTrackingRange(100)
            .updateInterval(1)
            .noSummon()
            .fireImmune()
            .noSave()
    );

    public static final EntityType<GrenadeEntity> GRENADE = thrown("grenade", GrenadeEntity::new);
    public static final EntityType<StunGrenadeEntity> STUN_GRENADE = thrown("stun_grenade", StunGrenadeEntity::new);
    public static final EntityType<SmokeGrenadeEntity> SMOKE_GRENADE = thrown("smoke_grenade", SmokeGrenadeEntity::new);
    public static final EntityType<MolotovCocktailEntity> MOLOTOV_COCKTAIL = thrown("molotov_cocktail", MolotovCocktailEntity::new);

    private GunEntities() {
    }

    public static void register() {
    }

    private static <T extends ProjectileEntity> EntityType<T> bullet(String path, EntityType.EntityFactory<T> factory) {
        return register(
            path,
            EntityType.Builder.of(factory, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(0)
                .noSummon()
                .fireImmune()
                .noSave()
        );
    }

    private static <T extends TimedThrowableEntity> EntityType<T> thrown(String path, EntityType.EntityFactory<T> factory) {
        return register(
            path,
            EntityType.Builder.of(factory, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .clientTrackingRange(8)
                .updateInterval(2)
        );
    }

    private static <T extends Entity> EntityType<T> register(String path, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, MmsArsenal.id(path));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }
}
