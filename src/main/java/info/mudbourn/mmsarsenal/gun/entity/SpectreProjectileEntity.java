package info.mudbourn.mmsarsenal.gun.entity;

import info.mudbourn.mmsarsenal.gun.Gun;
import info.mudbourn.mmsarsenal.gun.GunNetwork;
import info.mudbourn.mmsarsenal.gun.GunParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// A spectral round: trails ghostly glints and is the only round that can hit ghosts.
public class SpectreProjectileEntity extends ProjectileEntity {

    public SpectreProjectileEntity(EntityType<? extends ProjectileEntity> type, Level level) {
        super(type, level);
    }

    public SpectreProjectileEntity(EntityType<? extends ProjectileEntity> type, Level level, LivingEntity shooter, ItemStack weapon, Gun gun) {
        super(type, level, shooter, weapon, gun);
    }

    @Override
    protected void onProjectileTick() {
        if (this.level() instanceof ServerLevel level && this.tickCount > 1 && this.tickCount < this.getLife()) {
            GunNetwork.particlesToAll(level, GunParticles.GHOST_GLINT, this.getX() - this.getDeltaMovement().x, this.getY() - this.getDeltaMovement().y, this.getZ() - this.getDeltaMovement().z, 2, 0.1, 0.1, 0.1, 0.5);
        }
    }
}
