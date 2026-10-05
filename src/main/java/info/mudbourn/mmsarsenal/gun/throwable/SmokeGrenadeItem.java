package info.mudbourn.mmsarsenal.gun.throwable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

// A smoke grenade whose fuse runs from the moment it is thrown.
public class SmokeGrenadeItem extends ThrowableWeaponItem {

    private static final int FUSE_TICKS = 60;

    public SmokeGrenadeItem(Properties properties) {
        super(properties, FUSE_TICKS);
    }

    @Override
    protected boolean canCook() {
        return false;
    }

    @Override
    protected TimedThrowableEntity createProjectile(Level level, LivingEntity entity, int fuseTicks) {
        return new SmokeGrenadeEntity(level, entity, fuseTicks);
    }

    @Override
    protected void onThrown(Level level, TimedThrowableEntity projectile) {
        this.playPrimeSound(level, projectile);
    }
}
