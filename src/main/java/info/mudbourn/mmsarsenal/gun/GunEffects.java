package info.mudbourn.mmsarsenal.gun;

import info.mudbourn.mmsarsenal.config.ArsenalConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// What happens around a shot on the server: casings, wear, pushback, gun-specific blasts, and mobs reacting to the noise.
public final class GunEffects {

    private GunEffects() {
    }

    // Everything a shot does after it leaves the barrel, before ammo is spent.
    public static void afterShot(ServerLevel level, ServerPlayer player, ItemStack held, Gun gun) {
        if (!held.is(GunItems.HYPERSONIC_CANNON) && !gun.general().silenced()) {
            level.gameEvent(player, GameEvent.EXPLODE, player.position());
        }
        if (held.is(GunItems.BLOSSOM_RIFLE)) {
            Vec3 pos = besideShooter(player);
            GunNetwork.particlesToAll(level, ParticleTypes.CHERRY_LEAVES, pos.x, pos.y, pos.z, 1, 0.3, 0.2, 0.3, 0.0);
        }
        if (gun.projectile().ejectsCasing() && (GunComponents.ammo(held) >= 1 || player.getAbilities().instabuild) && held.is(GunItems.ROCKET_LAUNCHER)) {
            firingSmoke(level, player);
        }
        if (held.isDamageableItem()) {
            if (GunComponents.ammo(held) >= 1 && ArsenalConfig.get().gunDurability) {
                damageGun(held, level, player);
            }
            if (held.getDamageValue() >= held.getMaxDamage() / 1.5) {
                float pitch = 1.5F + player.getRandom().nextFloat() * 0.25F;
                level.playSound(player, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.3F, pitch);
            }
        }
        float pushback = gun.general().shooterPushback();
        if (player.isCrouching() && level.getBlockState(player.getOnPos()).isSolid()) {
            pushback /= 2.0F;
        }
        push(player, pushback);
        if (held.is(GunItems.HYPERSONIC_CANNON)) {
            hypersonicBlast(level, player, gun.projectile().damage());
        }
    }

    // Wears the gun by one, stopping at its last point so it jams instead of breaking.
    public static void damageGun(ItemStack stack, ServerLevel level, Player player) {
        if (!ArsenalConfig.get().gunDurability || player.getAbilities().instabuild || !stack.isDamageableItem()) {
            return;
        }
        if (stack.getDamageValue() >= stack.getMaxDamage() - 1) {
            if (stack.getDamageValue() >= stack.getMaxDamage() - 2) {
                level.playSound(player, player.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            return;
        }
        stack.setDamageValue(stack.getDamageValue() + 1);
    }

    // Shoves the shooter back along their aim and cancels fall damage built up so far.
    public static void push(Player player, double force) {
        Vec3 look = player.getLookAngle();
        player.push(look.x * force, look.y * force, look.z * force);
        player.fallDistance = 0.0;
    }

    private static void firingSmoke(ServerLevel level, Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 pos = player.position().add(look.x * 1.8, look.y * 1.8 + player.getEyeHeight(), look.z * 1.8);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 6, 0.0, 0.0, 0.0, 0.2);
    }

    // A point just off the shooter's right shoulder, where casings and flair spawn.
    private static Vec3 besideShooter(LivingEntity entity) {
        Vec3 look = entity.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0.0, look.x).normalize();
        Vec3 forward = new Vec3(look.x, 0.0, look.z).normalize();
        double divisor = entity instanceof Player player && GunState.isAiming(player) ? 0.4 : 0.5;
        return entity.position().add(
            right.x * divisor + forward.x * divisor,
            entity.getEyeHeight() - 0.4,
            right.z * divisor + forward.z * divisor
        );
    }

    // Kicks a spent casing out beside the shooter, styled for the gun's ammo.
    public static void ejectCasing(ServerLevel level, LivingEntity entity, Gun gun) {
        Vec3 pos = besideShooter(entity);
        SimpleParticleType casing = gun.projectile().item().equals(BuiltInRegistries.ITEM.getKey(GunItems.SPECTRE_ROUND))
            ? GunParticles.SPECTRE_CASING
            : GunParticles.CASING;
        level.sendParticles(casing, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    // How far the Hypersonic Cannon's beam reaches, in blocks.
    private static final double BEAM_RANGE = 100.0;
    // How far from its centre line the beam hits, in blocks, matching the drawn width of the sonic boom shockwaves along it.
    private static final double BEAM_RADIUS = 1.5;
    // What the beam deals to a player; sonic boom damage already ignores armor, toughness and Protection, so this is what they lose.
    private static final float BEAM_PLAYER_DAMAGE = 15.0F;

    // The Hypersonic Cannon's sonic beam: a wide blast through everything living in a hundred blocks, deafening, weakening and darkening each one.
    private static void hypersonicBlast(ServerLevel level, LivingEntity shooter, float damage) {
        Vec3 from = shooter.getEyePosition();
        Vec3 normal = shooter.getLookAngle().normalize();
        Vec3 to = from.add(normal.scale(BEAM_RANGE));
        AABB reach = new AABB(from, to).inflate(BEAM_RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, reach, entity -> entity != shooter && entity.isAlive())) {
            Vec3 centre = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
            double along = Math.max(0.0, Math.min(BEAM_RANGE, centre.subtract(from).dot(normal)));
            if (target.getBoundingBox().distanceToSqr(from.add(normal.scale(along))) > BEAM_RADIUS * BEAM_RADIUS) {
                continue;
            }
            if (target.isOnFire()) {
                target.clearFire();
                level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 1.0, target.getZ(), 6, 0.3, 0.3, 0.3, 0.0);
            }
            level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, centre.x, centre.y, centre.z, 12, 0.2, 0.0, 0.3, 0.1);
            target.hurtServer(level, level.damageSources().sonicBoom(shooter), target instanceof Player ? BEAM_PLAYER_DAMAGE : damage);
            target.addEffect(new MobEffectInstance(GunMobEffects.DEAFENED, 100, 0, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50));
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120));
        }
        for (int i = 3; i < (int) BEAM_RANGE; i++) {
            Vec3 point = from.add(normal.scale(i));
            GunNetwork.particlesToAll(level, ParticleTypes.SONIC_BOOM, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        GunNetwork.particlesToAll(level, GunParticles.SONIC_RING, from.x, from.y, from.z, 5, 0.0, 0.0, 0.0, 0.2);
        GunNetwork.particlesToAll(level, GunParticles.BIG_SONIC_RING, from.x, from.y, from.z, 2, 0.0, 0.0, 0.0, 0.1);
    }

    // Angers hostile mobs and scatters skittish ones within earshot of an unsilenced shot.
    public static void alertMobs(Player player, ItemStack held) {
        ArsenalConfig config = ArsenalConfig.get();
        if (!config.aggroMobs && !config.fleeingMobs) {
            return;
        }
        double radius = Math.max(config.aggroUnsilencedRange, config.fleeingUnsilencedRange);
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius))) {
            if (entity.getHealth() > 60.0F) {
                continue;
            }
            if (config.aggroMobs && isHostile(entity)) {
                anger(entity, player);
            }
            if (config.fleeingMobs && entity instanceof PathfinderMob mob && isSkittish(entity)) {
                flee(mob);
            }
        }
    }

    private static boolean isHostile(LivingEntity entity) {
        return (entity.getSoundSource() == SoundSource.HOSTILE || entity.getType() == EntityType.ZOMBIFIED_PIGLIN || entity.getType() == EntityType.PIGLIN)
            && entity.getType() != EntityType.ENDERMAN;
    }

    private static boolean isSkittish(LivingEntity entity) {
        return ArsenalConfig.get().fleeingEntities.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }

    private static void anger(LivingEntity entity, Player player) {
        if (entity instanceof ZombifiedPiglin piglin) {
            piglin.setPersistentAngerTarget(EntityReference.of(player));
            piglin.setTimeToRemainAngry(400 + player.getRandom().nextInt(400));
        } else if (entity instanceof Piglin piglin) {
            piglin.setTarget(player);
            piglin.setAggressive(true);
        } else {
            entity.setLastHurtByMob(player);
        }
    }

    private static void flee(PathfinderMob mob) {
        if (mob instanceof Wolf wolf && (wolf.isTame() || wolf.isInSittingPose())) {
            return;
        }
        mob.getBrain().eraseMemory(MemoryModuleType.HURT_BY);
        mob.getBrain().eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
        mob.getNavigation().stop();
        Vec3 target = LandRandomPos.getPos(mob, 16, 7);
        if (target != null) {
            mob.getNavigation().moveTo(target.x, target.y, target.z, mob.getType() == EntityType.VILLAGER ? 1.0 : 1.5);
        }
    }
}
