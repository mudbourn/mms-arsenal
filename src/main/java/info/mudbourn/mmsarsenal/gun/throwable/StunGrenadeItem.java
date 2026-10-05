package info.mudbourn.mmsarsenal.gun.throwable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

// A flashbang on a short fixed fuse that starts when thrown.
public class StunGrenadeItem extends ThrowableWeaponItem {

    private static final int FUSE_TICKS = 20;

    public StunGrenadeItem(Properties properties) {
        super(properties, FUSE_TICKS);
    }

    @Override
    protected boolean canCook() {
        return false;
    }

    @Override
    protected int remainingFuseTicks(int useDuration) {
        return FUSE_TICKS;
    }

    @Override
    protected TimedThrowableEntity createProjectile(Level level, LivingEntity entity, int fuseTicks) {
        return new StunGrenadeEntity(level, entity, FUSE_TICKS);
    }

    @Override
    protected void onThrown(Level level, TimedThrowableEntity projectile) {
        this.playPrimeSound(level, projectile);
    }
}
