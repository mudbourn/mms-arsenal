package info.mudbourn.mmsarsenal.gun.throwable;

import info.mudbourn.mmsarsenal.gun.GunGameRules;
import info.mudbourn.mmsarsenal.gun.GunItems;
import info.mudbourn.mmsarsenal.gun.GunSounds;
import info.mudbourn.mmsarsenal.gun.entity.GunEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

// A molotov cocktail: shatters on the first thing it touches, burning everything near and setting the ground alight.
public class MolotovCocktailEntity extends TimedThrowableEntity {

    private static final double FIRE_RADIUS = 3.0;
    private static final int IGNITE_RADIUS = 2;
    private static final float BURN_DAMAGE = 6.0F;
    private static final float BURN_SECONDS = 8.0F;

    public MolotovCocktailEntity(EntityType<? extends MolotovCocktailEntity> type, Level level) {
        super(type, level);
    }

    public MolotovCocktailEntity(Level level, LivingEntity owner, int fuseTicks) {
        super(GunEntities.MOLOTOV_COCKTAIL, level, owner, fuseTicks);
    }

    @Override
    protected Item getDefaultItem() {
        return GunItems.MOLOTOV_COCKTAIL;
    }

    @Override
    protected boolean detonatesOnImpact() {
        return true;
    }

    @Override
    protected void spawnFlightParticles() {
        this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.15, this.getZ(), 0.0, 0.0, 0.0);
        this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY() + 0.15, this.getZ(), 0.0, 0.0, 0.0);
    }

    @Override
    protected void explode() {
        ServerLevel level = (ServerLevel) this.level();
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.3F, 1.0F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), GunSounds.MOLOTOV_EXPLOSION, SoundSource.NEUTRAL, 2.0F, 1.0F);
        level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 40, 1.4, 0.4, 1.4, 0.02);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(), this.getZ(), 18, 0.7, 0.3, 0.7, 0.01);
        Entity owner = this.getOwner();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(FIRE_RADIUS))) {
            if (target.isAlive() && target.distanceToSqr(this) <= FIRE_RADIUS * FIRE_RADIUS) {
                target.hurtServer(level, level.damageSources().thrown(this, owner), BURN_DAMAGE);
                target.igniteForSeconds(BURN_SECONDS);
            }
        }
        if (level.getGameRules().get(GunGameRules.GUN_GRIEFING)) {
            this.igniteNearby(level);
        }
    }

    private void igniteNearby(ServerLevel level) {
        BlockPos center = this.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-IGNITE_RADIUS, 0, -IGNITE_RADIUS), center.offset(IGNITE_RADIUS, 1, IGNITE_RADIUS))) {
            if (level.getBlockState(pos).isAir() && this.random.nextBoolean() && Blocks.FIRE.defaultBlockState().canSurvive(level, pos)) {
                level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
            }
        }
    }
}
