package info.mudbourn.mmsarsenal.gun.entity;

import info.mudbourn.mmsarsenal.config.ArsenalConfig;
import info.mudbourn.mmsarsenal.gun.Gun;
import info.mudbourn.mmsarsenal.gun.GunGameRules;
import info.mudbourn.mmsarsenal.gun.GunNetwork;
import info.mudbourn.mmsarsenal.gun.GunParticles;
import info.mudbourn.mmsarsenal.gun.GunTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// A rocket: trails smoke and fire and explodes on impact or when it burns out; fired while crouching, the first thing it hits rides it.
public class RocketEntity extends ProjectileEntity {

    private boolean rocketRide;

    public RocketEntity(EntityType<? extends ProjectileEntity> type, Level level) {
        super(type, level);
    }

    public RocketEntity(EntityType<? extends ProjectileEntity> type, Level level, LivingEntity shooter, ItemStack weapon, Gun gun) {
        super(type, level, shooter, weapon, gun);
        this.rocketRide = shooter.isCrouching() && ArsenalConfig.get().rocketRiding;
    }

    @Override
    protected void onProjectileTick() {
        if (!(this.level() instanceof ServerLevel level) || this.tickCount <= 1 || this.tickCount >= this.getLife()) {
            return;
        }
        double x = this.getX() - this.getDeltaMovement().x;
        double y = this.getY() - this.getDeltaMovement().y;
        double z = this.getZ() - this.getDeltaMovement().z;
        for (int i = 5; i > 0; i--) {
            GunNetwork.particlesToAll(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        if (level.getRandom().nextInt(2) == 0) {
            GunNetwork.particlesToAll(level, GunParticles.FIRE, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            GunNetwork.particlesToAll(level, ParticleTypes.LAVA, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            GunNetwork.particlesToAll(level, ParticleTypes.FLAME, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void impactEffect() {
        double x = this.getX() - this.getDeltaMovement().x;
        double y = this.getY() - this.getDeltaMovement().y;
        double z = this.getZ() - this.getDeltaMovement().z;
        if (this.level() instanceof ServerLevel level) {
            GunNetwork.particlesToAll(level, GunParticles.BIG_EXPLOSION, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            return;
        }
        for (int i = 0; i < 10; i++) {
            double xSpeed = (this.random.nextDouble() - 0.5) * 0.5 * 10.0;
            double ySpeed = (this.random.nextDouble() - 0.5) * 0.5 * 10.0;
            double zSpeed = (this.random.nextDouble() - 0.5) * 0.5 * 10.0;
            this.level().addParticle(GunParticles.SMOKE, true, true, x, y + 2.0, z, xSpeed, ySpeed, zSpeed);
            this.level().addParticle(GunParticles.FIRE, true, true, x, y + 2.0, z, xSpeed, ySpeed, zSpeed);
        }
    }

    @Override
    protected void onHitEntity(ServerLevel level, Entity entity, Vec3 hitVec, Vec3 start, Vec3 end, boolean headshot) {
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        if (entity.getType().is(GunTags.HEAVY) || entity.getType().is(GunTags.VERY_HEAVY)) {
            this.explode(level);
        } else if (this.tickCount <= 5 && this.getPassengers().isEmpty() && this.rocketRide) {
            entity.startRiding(this);
        } else if (this.getPassengers().isEmpty()) {
            this.explode(level);
        }
    }

    @Override
    protected void onHitBlock(ServerLevel level, BlockState state, BlockPos pos, Direction face, Vec3 hitVec) {
        this.explode(level);
    }

    @Override
    protected void onExpired() {
        if (this.level() instanceof ServerLevel level) {
            this.explode(level);
        }
    }

    private void explode(ServerLevel level) {
        boolean keepBlocks = !(this.getShooter() instanceof Player)
            || !ArsenalConfig.get().enableBlockRemovalOnExplosions
            || !level.getGameRules().get(GunGameRules.GUN_GRIEFING);
        level.explode(
            this,
            level.damageSources().explosion(this, this.getShooter()),
            null,
            this.getX(),
            this.getY(),
            this.getZ(),
            (float) ArsenalConfig.get().missileExplosionRadius,
            false,
            keepBlocks ? Level.ExplosionInteraction.NONE : Level.ExplosionInteraction.TNT
        );
        this.discard();
    }
}
