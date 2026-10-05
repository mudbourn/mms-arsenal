package info.mudbourn.mmsarsenal.gun.throwable;

import info.mudbourn.mmsarsenal.config.ArsenalConfig;
import info.mudbourn.mmsarsenal.gun.GunItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

// A thrown grenade: bounces off what it hits, smokes in flight, and explodes when its fuse runs out without breaking blocks.
public class ThrowableGrenadeEntity extends ThrowableItemProjectile {

    private boolean shouldBounce = true;
    private double gravity = 0.05;
    private int maxLife = 60;

    public ThrowableGrenadeEntity(EntityType<? extends ThrowableGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public ThrowableGrenadeEntity(EntityType<? extends ThrowableGrenadeEntity> type, Level level, LivingEntity thrower, int fuse) {
        super(type, thrower, level, new net.minecraft.world.item.ItemStack(GunItems.GRENADE));
        this.maxLife = fuse;
    }

    @Override
    protected Item getDefaultItem() {
        return GunItems.GRENADE;
    }

    protected void setShouldBounce(boolean shouldBounce) {
        this.shouldBounce = shouldBounce;
    }

    @Override
    protected double getDefaultGravity() {
        return this.gravity;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.shouldBounce && this.tickCount >= this.maxLife) {
            this.discard();
            this.onDeath();
        }
        this.particleTick();
    }

    // The trail it leaves in flight.
    protected void particleTick() {
        if (this.level().isClientSide()) {
            this.level().addParticle(ParticleTypes.SMOKE, true, false, this.getX(), this.getY() + 0.25, this.getZ(), 0.0, 0.0, 0.0);
        }
    }

    // Blinds everything close and explodes, leaving blocks alone.
    public void onDeath() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        for (Entity entity : level.getEntities(this, this.getBoundingBox().inflate(5.0), EntitySelector.NO_CREATIVE_OR_SPECTATOR)) {
            if (entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
            }
        }
        level.explode(this, this.getX(), this.getY(), this.getZ(), (float) ArsenalConfig.get().grenadeExplosionRadius, Level.ExplosionInteraction.NONE);
    }

    @Override
    protected void onHit(HitResult result) {
        if (this.level().isClientSide()) {
            return;
        }
        if (result instanceof BlockHitResult blockHit) {
            if (!this.shouldBounce) {
                this.discard();
                this.onDeath();
                return;
            }
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = this.level().getBlockState(pos);
            SoundEvent step = state.getSoundType().getStepSound();
            if (this.getDeltaMovement().length() > 0.1) {
                this.level().playSound(null, result.getLocation().x, result.getLocation().y, result.getLocation().z, step, SoundSource.AMBIENT, 1.0F, 1.0F);
            }
            this.bounce(blockHit.getDirection());
        } else if (result instanceof EntityHitResult entityHit) {
            if (!this.shouldBounce) {
                this.discard();
                this.onDeath();
                return;
            }
            Entity entity = entityHit.getEntity();
            if (this.getDeltaMovement().length() > 0.1 && this.level() instanceof ServerLevel level) {
                entity.hurtServer(level, this.damageSources().thrown(this, this.getOwner()), 1.0F);
            }
            this.bounce(Direction.getApproximateNearest(this.getDeltaMovement().x, this.getDeltaMovement().y, this.getDeltaMovement().z).getOpposite());
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.25, 1.0, 0.25));
        }
    }

    private void bounce(Direction direction) {
        switch (direction.getAxis()) {
            case X -> this.setDeltaMovement(this.getDeltaMovement().multiply(-0.5, 0.75, 0.75));
            case Y -> {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.75, -0.25, 0.75));
                if (this.getDeltaMovement().y < this.gravity) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.0, 1.0));
                }
            }
            case Z -> this.setDeltaMovement(this.getDeltaMovement().multiply(0.75, 0.75, -0.5));
        }
    }
}
