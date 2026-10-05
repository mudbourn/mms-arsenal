package info.mudbourn.mmsarsenal.gun;

import info.mudbourn.mmsarsenal.MmsArsenal;
import info.mudbourn.mmsarsenal.config.ArsenalConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

// Effects from sonic blasts and grenades: deafened, blinded by a flash, and choking in smoke.
public final class GunMobEffects {

    public static final Holder<MobEffect> DEAFENED = register("deafened", new PlainEffect(MobEffectCategory.HARMFUL, 0));
    public static final Holder<MobEffect> BLINDED = register("blinded", new PlainEffect(MobEffectCategory.HARMFUL, 0));
    public static final Holder<MobEffect> SMOKED = register("smoked", new SmokedEffect(MobEffectCategory.HARMFUL, 0));

    private GunMobEffects() {
    }

    public static void register() {
    }

    private static Holder<MobEffect> register(String path, MobEffect effect) {
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, MmsArsenal.id(path), effect);
    }

    // A plain effect with no tick of its own.
    public static class PlainEffect extends MobEffect {

        PlainEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }

    // Smoke that drops a mob's target and optionally chips at health, ticking once a second.
    public static class SmokedEffect extends PlainEffect {

        SmokedEffect(MobEffectCategory category, int color) {
            super(category, color);
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            double damage = ArsenalConfig.get().smokeGrenadeDamage;
            if (entity.getHealth() > 1.0F && damage > 0.0) {
                entity.hurtServer(level, level.damageSources().magic(), (float) damage);
            }
            if (entity instanceof Mob mob) {
                mob.setTarget(null);
            }
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            int interval = 20 >> amplifier;
            return interval <= 0 || duration % interval == 0;
        }
    }
}
