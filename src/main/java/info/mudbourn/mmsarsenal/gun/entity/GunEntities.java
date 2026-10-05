package info.mudbourn.mmsarsenal.gun.entity;

import info.mudbourn.mmsarsenal.MmsArsenal;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

// Projectile entity types: bullets stay on the server and are drawn only as client-side trails; rockets are tracked so clients see them.
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

    private static <T extends Entity> EntityType<T> register(String path, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, MmsArsenal.id(path));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }
}
