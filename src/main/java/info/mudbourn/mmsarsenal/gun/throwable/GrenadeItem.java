package info.mudbourn.mmsarsenal.gun.throwable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

// A frag grenade that can be cooked: holding it burns the fuse down before the throw.
public class GrenadeItem extends ThrowableWeaponItem {

    private static final int FUSE_TICKS = 60;

    public GrenadeItem(Properties properties) {
        super(properties, FUSE_TICKS);
    }

    @Override
    protected TimedThrowableEntity createProjectile(Level level, LivingEntity entity, int fuseTicks) {
        return new GrenadeEntity(level, entity, fuseTicks);
    }
}
