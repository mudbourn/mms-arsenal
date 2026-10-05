package info.mudbourn.mmsarsenal.gun.throwable;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

// A molotov cocktail that bursts on impact, lit by a flint-and-steel click while held.
public class MolotovCocktailItem extends ThrowableWeaponItem {

    private static final int FUSE_TICKS = 60;

    public MolotovCocktailItem(Properties properties) {
        super(properties, FUSE_TICKS);
    }

    @Override
    protected TimedThrowableEntity createProjectile(Level level, LivingEntity entity, int fuseTicks) {
        return new MolotovCocktailEntity(level, entity, fuseTicks);
    }

    @Override
    protected void playPrimeSound(Level level, Entity entity) {
        this.playSound(level, entity, SoundEvents.FLINTANDSTEEL_USE);
    }
}
