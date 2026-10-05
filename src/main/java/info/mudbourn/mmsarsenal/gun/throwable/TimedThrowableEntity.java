package info.mudbourn.mmsarsenal.gun.throwable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

// A thrown item with a fuse that bounces off surfaces and goes off when the fuse runs out or, for some, on impact.
public abstract class TimedThrowableEntity extends ThrowableItemProjectile {

    private static final EntityDataAccessor<Integer> DATA_FUSE = SynchedEntityData.defineId(TimedThrowableEntity.class, EntityDataSerializers.INT);
    private static final int DEFAULT_FUSE = 60;
    private static final int OWNER_SAFE_TICKS = 8;

    protected TimedThrowableEntity(EntityType<? extends TimedThrowableEntity> type, Level level) {
        super(type, level);
    }

    protected TimedThrowableEntity(EntityType<? extends TimedThrowableEntity> type, Level level, LivingEntity owner, int fuseTicks) {
        this(type, level);
        this.setOwner(owner);
        this.setFuse(fuseTicks);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FUSE, DEFAULT_FUSE);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.spawnFlightParticles();
            return;
        }
        int fuse = this.getFuse() - 1;
        if (fuse <= 0) {
            this.explodeNow();
            return;
        }
        this.setFuse(fuse);
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && (entity != this.getOwner() || this.tickCount > OWNER_SAFE_TICKS);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level().isClientSide()) {
            return;
        }
        if (this.detonatesOnImpact()) {
            this.explodeNow();
            return;
        }
        if (this.getDeltaMovement().length() > 0.1) {
            BlockPos pos = result.getBlockPos();
            this.level().playSound(
                null,
                result.getLocation().x,
                result.getLocation().y,
                result.getLocation().z,
                this.level().getBlockState(pos).getSoundType().getStepSound(),
                SoundSource.AMBIENT,
                1.0F,
                1.0F
            );
        }
        this.bounce(result.getDirection());
        Vec3 offset = Vec3.atLowerCornerOf(result.getDirection().getUnitVec3i()).scale(0.13);
        double lift = result.getDirection() == Direction.DOWN ? 0.25 : result.getDirection().getAxis().isHorizontal() ? 0.125 : 0.0;
        this.setPos(result.getLocation().add(offset).subtract(0.0, lift, 0.0));
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide()) {
            return;
        }
        if (this.detonatesOnImpact()) {
            this.explodeNow();
            return;
        }
        Vec3 motion = this.getDeltaMovement();
        this.bounce(Direction.getApproximateNearest(motion.x, motion.y, motion.z).getOpposite());
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.25, 1.0, 0.25));
    }

    private void bounce(Direction direction) {
        switch (direction.getAxis()) {
            case X -> this.setDeltaMovement(this.getDeltaMovement().multiply(-0.5, 0.75, 0.75));
            case Y -> {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.75, -0.25, 0.75));
                if (Math.abs(this.getDeltaMovement().y) < this.getDefaultGravity()) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.0, 1.0));
                }
            }
            case Z -> this.setDeltaMovement(this.getDeltaMovement().multiply(0.75, 0.75, -0.5));
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Fuse", this.getFuse());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setFuse(Mth.clamp(input.getIntOr("Fuse", DEFAULT_FUSE), 1, 1200));
    }

    public final int getFuse() {
        return this.entityData.get(DATA_FUSE);
    }

    public final void setFuse(int fuseTicks) {
        this.entityData.set(DATA_FUSE, Math.max(1, fuseTicks));
    }

    // Goes off now and removes the entity, server side only.
    public final void explodeNow() {
        if (!this.level().isClientSide() && this.isAlive()) {
            this.explode();
            this.discard();
        }
    }

    protected boolean detonatesOnImpact() {
        return false;
    }

    // The trail it leaves in flight, client side.
    protected void spawnFlightParticles() {
    }

    protected abstract void explode();
}
