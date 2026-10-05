package info.mudbourn.mmsarsenal.gun.throwable;

import info.mudbourn.mmsarsenal.config.ArsenalConfig;
import info.mudbourn.mmsarsenal.gun.GunItems;
import info.mudbourn.mmsarsenal.gun.GunMobEffects;
import info.mudbourn.mmsarsenal.gun.GunSounds;
import info.mudbourn.mmsarsenal.gun.entity.GunEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

// A smoke grenade: bursts into a stack of lingering clouds that choke mobs and snuff out nearby fire.
public class SmokeGrenadeEntity extends TimedThrowableEntity {

    private static final double[] CLOUD_OFFSETS = {-0.5, 0.5, 1.5, 2.5};
    private static final int EXTINGUISH_RADIUS = 4;

    public SmokeGrenadeEntity(EntityType<? extends SmokeGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public SmokeGrenadeEntity(Level level, LivingEntity owner, int fuseTicks) {
        super(GunEntities.SMOKE_GRENADE, level, owner, fuseTicks);
    }

    @Override
    protected Item getDefaultItem() {
        return GunItems.SMOKE_GRENADE;
    }

    @Override
    protected void spawnFlightParticles() {
        this.level().addParticle(
            ParticleTypes.CAMPFIRE_COSY_SMOKE,
            this.getX(),
            this.getY() + 0.2,
            this.getZ(),
            (this.random.nextDouble() - 0.5) * 0.08,
            0.08,
            (this.random.nextDouble() - 0.5) * 0.08
        );
    }

    @Override
    protected void explode() {
        ServerLevel level = (ServerLevel) this.level();
        ArsenalConfig config = ArsenalConfig.get();
        level.playSound(null, this.getX(), this.getY(), this.getZ(), GunSounds.SMOKE_GRENADE_EXPLOSION, SoundSource.NEUTRAL, 2.0F, 1.0F);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 60, 0.8, 1.2, 0.8, 0.02);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 20, 0.6, 0.8, 0.6, 0.01);
        for (double offset : CLOUD_OFFSETS) {
            AreaEffectCloud cloud = new AreaEffectCloud(level, this.getX(), this.getY() + offset, this.getZ());
            cloud.setRadius((float) (config.smokeGrenadeCloudDiameter / 2.0));
            cloud.setDuration((int) (config.smokeGrenadeCloudDuration * 20.0));
            cloud.setWaitTime(0);
            cloud.setRadiusPerTick(0.0F);
            cloud.addEffect(new MobEffectInstance(GunMobEffects.SMOKED, 40, 0, false, false, true));
            level.addFreshEntity(cloud);
        }
        this.extinguishNearbyFire(level);
    }

    private void extinguishNearbyFire(ServerLevel level) {
        BlockPos center = this.blockPosition();
        boolean extinguished = false;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-EXTINGUISH_RADIUS, -2, -EXTINGUISH_RADIUS), center.offset(EXTINGUISH_RADIUS, 2, EXTINGUISH_RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BaseFireBlock) {
                level.removeBlock(pos, false);
                extinguished = true;
            } else if (CampfireBlock.isLitCampfire(state)) {
                CampfireBlock.dowse(null, level, pos, state);
                level.setBlock(pos, state.setValue(CampfireBlock.LIT, false), 11);
                extinguished = true;
            } else if (AbstractCandleBlock.isLit(state)) {
                AbstractCandleBlock.extinguish(null, state, level, pos);
                extinguished = true;
            }
        }
        if (extinguished) {
            level.playSound(null, center, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.8F, 1.0F);
        }
    }
}
