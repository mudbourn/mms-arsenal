package info.mudbourn.mmsarsenal.gun.throwable;

import info.mudbourn.mmsarsenal.gun.GunItems;
import info.mudbourn.mmsarsenal.gun.GunMobEffects;
import info.mudbourn.mmsarsenal.gun.GunNetwork;
import info.mudbourn.mmsarsenal.gun.GunParticles;
import info.mudbourn.mmsarsenal.gun.GunSounds;
import info.mudbourn.mmsarsenal.gun.entity.GunEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// A flashbang: deafens and blinds everything in range, longest for whatever looks at it with a clear view; stunned mobs lose their senses and wander.
public class StunGrenadeEntity extends TimedThrowableEntity {

    private static final double EFFECT_RADIUS = 12.0;

    public StunGrenadeEntity(EntityType<? extends StunGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public StunGrenadeEntity(Level level, LivingEntity owner, int fuseTicks) {
        super(GunEntities.STUN_GRENADE, level, owner, fuseTicks);
    }

    @Override
    protected Item getDefaultItem() {
        return GunItems.STUN_GRENADE;
    }

    @Override
    protected void spawnFlightParticles() {
        if (this.random.nextInt(3) == 0) {
            this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 0.1, this.getZ(), 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void explode() {
        ServerLevel level = (ServerLevel) this.level();
        level.playSound(
            null,
            this.getX(),
            this.getY(),
            this.getZ(),
            GunSounds.STUN_GRENADE_EXPLOSION,
            SoundSource.NEUTRAL,
            2.0F,
            0.7F + this.random.nextFloat() * 0.2F
        );
        GunNetwork.particlesToAll(level, GunParticles.BIG_EXPLOSION, this.getX(), this.getY(), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        Vec3 center = this.position().add(0.0, this.getBbHeight() * 0.5, 0.0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(EFFECT_RADIUS))) {
            double distanceFactor = 1.0 - Math.sqrt(target.distanceToSqr(center)) / EFFECT_RADIUS;
            if (!target.isAlive() || distanceFactor <= 0.0) {
                continue;
            }
            int deafenedTicks = Mth.floor(60.0 + 160.0 * distanceFactor);
            target.addEffect(new MobEffectInstance(GunMobEffects.DEAFENED, deafenedTicks, 0, false, false, true));
            double facing = facingFactor(target, center);
            double sight = facing > 0.2 && this.hasLineOfSight(target, center) ? facing : 0.0;
            int blindedTicks = Mth.floor(40.0 + 80.0 * distanceFactor + 60.0 * distanceFactor * sight);
            target.addEffect(new MobEffectInstance(GunMobEffects.BLINDED, blindedTicks, 0, false, false, true));
            if (target instanceof Mob mob) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
        }
    }

    private static double facingFactor(LivingEntity target, Vec3 center) {
        Vec3 toGrenade = center.subtract(target.getEyePosition()).normalize();
        return Math.max(0.0, target.getViewVector(1.0F).normalize().dot(toGrenade));
    }

    private boolean hasLineOfSight(LivingEntity target, Vec3 center) {
        HitResult hit = this.level().clip(new ClipContext(target.getEyePosition(), center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(center) < 0.25;
    }
}
