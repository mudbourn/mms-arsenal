package info.mudbourn.mmsarsenal.gun.throwable;

import info.mudbourn.mmsarsenal.config.ArsenalConfig;
import info.mudbourn.mmsarsenal.gun.GunGameRules;
import info.mudbourn.mmsarsenal.gun.GunItems;
import info.mudbourn.mmsarsenal.gun.GunNetwork;
import info.mudbourn.mmsarsenal.gun.GunParticles;
import info.mudbourn.mmsarsenal.gun.entity.GunEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// A fragmentation grenade: explodes when its fuse runs out, hurting everything near with falloff and breaking no blocks.
public class GrenadeEntity extends TimedThrowableEntity {

    private static final float DAMAGE_PER_POWER = 5.0F;
    private static final float EDGE_DAMAGE_FLOOR = 0.35F;
    private static final double FALLOFF_EXPONENT = 0.8;

    public GrenadeEntity(EntityType<? extends GrenadeEntity> type, Level level) {
        super(type, level);
    }

    public GrenadeEntity(Level level, LivingEntity owner, int fuseTicks) {
        super(GunEntities.GRENADE, level, owner, fuseTicks);
    }

    @Override
    protected Item getDefaultItem() {
        return GunItems.GRENADE;
    }

    @Override
    protected void spawnFlightParticles() {
        Vec3 motion = this.getDeltaMovement();
        if (motion.length() <= 0.1) {
            return;
        }
        this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.1, this.getZ(), -motion.x * 0.1, -motion.y * 0.1, -motion.z * 0.1);
        if (this.random.nextInt(2) == 0) {
            this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY() + 0.1, this.getZ(), -motion.x * 0.05, -motion.y * 0.05, -motion.z * 0.05);
        }
    }

    @Override
    protected void explode() {
        ServerLevel level = (ServerLevel) this.level();
        double radius = Math.max(2.6, ArsenalConfig.get().grenadeExplosionRadius);
        float power = (float) (radius / 2.0);
        GunNetwork.particlesToAll(level, GunParticles.BIG_EXPLOSION, this.getX(), this.getY(), this.getZ(), 2, 0.2, 0.2, 0.2, 0.01);
        GunNetwork.particlesToAll(level, GunParticles.SMALL_EXPLOSION, this.getX(), this.getY(), this.getZ(), 14, 0.8, 0.8, 0.8, 0.12);
        GunNetwork.particlesToAll(level, GunParticles.SMOKE, this.getX(), this.getY(), this.getZ(), 10, 1.0, 1.0, 1.0, 0.02);
        GunNetwork.particlesToAll(level, GunParticles.FIRE, this.getX(), this.getY(), this.getZ(), 8, 0.7, 0.7, 0.7, 0.04);
        level.explode(this, this.getX(), this.getY(), this.getZ(), Math.max(1.2F, power * 0.5F), Level.ExplosionInteraction.NONE);
        this.damageNearby(level, radius, power * DAMAGE_PER_POWER);
        if (level.getGameRules().get(GunGameRules.GUN_GRIEFING) && ArsenalConfig.get().setFireToBlocks) {
            this.igniteNearby(level);
        }
    }

    // Hurts living things within the radius, less toward the edge and less again for the thrower.
    private void damageNearby(ServerLevel level, double radius, float baseDamage) {
        Entity owner = this.getOwner();
        AABB area = this.getBoundingBox().inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
            double distance = target.distanceTo(this);
            if (!target.isAlive() || distance > radius) {
                continue;
            }
            double curve = Math.pow(Math.max(0.0, 1.0 - distance / radius), FALLOFF_EXPONENT);
            float damage = baseDamage * (float) (EDGE_DAMAGE_FLOOR + (1.0 - EDGE_DAMAGE_FLOOR) * curve);
            if (target == owner) {
                damage *= 0.65F;
            }
            if (damage > 0.5F) {
                target.hurtServer(level, level.damageSources().explosion(this, owner), damage);
            }
        }
    }

    private void igniteNearby(ServerLevel level) {
        BlockPos center = this.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, 0, -1), center.offset(1, 1, 1))) {
            if (level.getBlockState(pos).isAir() && this.random.nextBoolean() && Blocks.FIRE.defaultBlockState().canSurvive(level, pos)) {
                level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
            }
        }
    }
}
