package info.mudbourn.mmsarsenal.gun.entity;

import info.mudbourn.mmsarsenal.gun.Gun;
import info.mudbourn.mmsarsenal.gun.GunItems;
import info.mudbourn.mmsarsenal.gun.GunNetwork;
import info.mudbourn.mmsarsenal.gun.GunParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// A burning round: trails lava and ash, sets what it hits on fire, and fizzles out underwater.
public class BlazeProjectileEntity extends ProjectileEntity {

    public BlazeProjectileEntity(EntityType<? extends ProjectileEntity> type, Level level) {
        super(type, level);
    }

    public BlazeProjectileEntity(EntityType<? extends ProjectileEntity> type, Level level, LivingEntity shooter, ItemStack weapon, Gun gun) {
        super(type, level, shooter, weapon, gun);
    }

    @Override
    protected void onProjectileTick() {
        if (!(this.level() instanceof ServerLevel level) || this.tickCount <= 1 || this.tickCount >= this.getLife()) {
            return;
        }
        double x = this.getX() - this.getDeltaMovement().x;
        double y = this.getY() - this.getDeltaMovement().y;
        double z = this.getZ() - this.getDeltaMovement().z;
        SimpleParticleType lava = ParticleTypes.LAVA;
        if (this.weapon.is(GunItems.SOULHUNTER_MK2)) {
            if (this.tickCount % 2 == 0) {
                GunNetwork.particlesToAll(level, ParticleTypes.SOUL, x, y, z, 1, 0.0, 0.0, 0.0, 0.05);
            }
            lava = this.tickCount % 3 == 0 ? GunParticles.SOUL_LAVA : ParticleTypes.LAVA;
        }
        for (int i = 0; i < 3; i++) {
            GunNetwork.particlesToAll(level, lava, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        GunNetwork.particlesToAll(level, ParticleTypes.ASH, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        GunNetwork.particlesToAll(level, ParticleTypes.WHITE_ASH, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel level && this.isUnderWater()) {
            GunNetwork.particlesToAll(level, ParticleTypes.CLOUD, this.getX() - this.getDeltaMovement().x, this.getY() - this.getDeltaMovement().y, this.getZ() - this.getDeltaMovement().z, 3, 0.1, 0.1, 0.1, 0.05);
            level.playSound(this, this.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 1.0F);
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(ServerLevel level, Entity entity, Vec3 hitVec, Vec3 start, Vec3 end, boolean headshot) {
        super.onHitEntity(level, entity, hitVec, start, end, headshot);
        entity.igniteForSeconds(5.0F);
    }

    @Override
    protected void onHitBlock(ServerLevel level, BlockState state, BlockPos pos, Direction face, Vec3 hitVec) {
        super.onHitBlock(level, state, pos, face, hitVec);
        this.igniteFace(level, pos, face, hitVec);
    }
}
