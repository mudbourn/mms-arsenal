package info.mudbourn.mmsarsenal.gun.throwable;

import info.mudbourn.mmsarsenal.gun.GunSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// A throwable held to prime and released to throw; cookable ones explode in hand if held past their fuse.
public abstract class ThrowableWeaponItem extends Item {

    protected static final int MIN_THROW_TICKS = 10;
    private static final int MAX_HOLD_DURATION = 72000;

    private final int maxCookTime;

    protected ThrowableWeaponItem(Properties properties, int maxCookTime) {
        super(properties);
        this.maxCookTime = maxCookTime;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return this.canCook() ? this.maxCookTime : MAX_HOLD_DURATION;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseTicks) {
        if (this.canCook() && this.chargeDuration(stack, entity, remainingUseTicks) == MIN_THROW_TICKS) {
            this.playPrimeSound(level, entity);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (this.canCook() && !level.isClientSide() && !entity.isInWater()) {
            TimedThrowableEntity projectile = this.createProjectile(level, entity, 0);
            projectile.setPos(entity.getEyePosition());
            level.addFreshEntity(projectile);
            projectile.explodeNow();
            this.consumeAndAward(stack, entity);
        }
        return stack;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide() || entity.isInWater()) {
            return false;
        }
        int duration = this.chargeDuration(stack, entity, timeLeft);
        if (duration < MIN_THROW_TICKS) {
            return false;
        }
        TimedThrowableEntity projectile = this.createProjectile(level, entity, this.remainingFuseTicks(duration));
        Vec3 look = entity.getLookAngle();
        projectile.setPos(entity.getEyePosition().add(look.scale(0.35)));
        projectile.shootFromRotation(entity, entity.getXRot(), entity.getYRot(), 0.0F, Math.min(1.0F, duration / (float) MIN_THROW_TICKS), 1.0F);
        level.addFreshEntity(projectile);
        this.onThrown(level, projectile);
        this.consumeAndAward(stack, entity);
        return true;
    }

    protected boolean canCook() {
        return true;
    }

    protected int remainingFuseTicks(int useDuration) {
        return Math.max(5, this.maxCookTime - useDuration);
    }

    protected void onThrown(Level level, TimedThrowableEntity projectile) {
    }

    protected void playPrimeSound(Level level, Entity entity) {
        this.playSound(level, entity, GunSounds.GRENADE_PIN);
    }

    protected final void playSound(Level level, Entity source, SoundEvent sound) {
        level.playSound(null, source.getX(), source.getY(), source.getZ(), sound, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    private int chargeDuration(ItemStack stack, LivingEntity entity, int remainingUseTicks) {
        int usedTicks = this.getUseDuration(stack, entity) - remainingUseTicks;
        return this.canCook() ? usedTicks : Math.min(usedTicks, this.maxCookTime);
    }

    private void consumeAndAward(ItemStack stack, LivingEntity entity) {
        if (entity instanceof Player player) {
            player.awardStat(Stats.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        } else {
            stack.shrink(1);
        }
    }

    protected abstract TimedThrowableEntity createProjectile(Level level, LivingEntity entity, int fuseTicks);
}
