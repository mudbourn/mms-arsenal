package info.mudbourn.mmsarsenal.gun;

import info.mudbourn.mmsarsenal.MmsArsenal;
import info.mudbourn.mmsarsenal.config.ArsenalConfig;
import info.mudbourn.mmsarsenal.gun.entity.BlazeProjectileEntity;
import info.mudbourn.mmsarsenal.gun.entity.GunEntities;
import info.mudbourn.mmsarsenal.gun.entity.ProjectileEntity;
import info.mudbourn.mmsarsenal.gun.entity.RocketEntity;
import info.mudbourn.mmsarsenal.gun.entity.SpectreProjectileEntity;
import info.mudbourn.mmsarsenal.gun.net.BulletTrailPayload;
import info.mudbourn.mmsarsenal.gun.net.GunSoundPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// Server side of firing, melee and unloading a gun.
public final class GunServer {

    private static final double MELEE_RANGE = 2.0;
    private static final double MELEE_SWEEP = Math.toRadians(100.0);

    private GunServer() {
    }

    // Fires the held gun if it is loaded, ready and not on cooldown.
    public static void shoot(ServerPlayer player, float yaw, float pitch) {
        if (player.isSpectator() || player.getUseItem().getItem() instanceof ShieldItem) {
            return;
        }
        ServerLevel level = player.level();
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(held.getItem() instanceof GunItem gunItem)) {
            return;
        }
        if (!GunComponents.hasAmmo(held) && !player.isCreative()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, 0.8F);
            return;
        }
        Gun gun = gunItem.getGun(false);
        if (!canFire(player, held, gun)) {
            return;
        }
        player.setYRot(Mth.wrapDegrees(yaw));
        player.setXRot(Mth.clamp(pitch, -90.0F, 90.0F));
        ShootTracker tracker = ShootTracker.get(player);
        if (tracker.tooEarly(gunItem)) {
            MmsArsenal.LOG.warn("{} ({}) fired before their cooldown finished or the server is lagging; {} ms remained", player.getName().getString(), player.getUUID(), tracker.remaining(gunItem));
            return;
        }
        tracker.putCooldown(gunItem, gun.general().rate());
        GunState.setReloading(player, false);
        if (!gun.general().alwaysSpread() && gun.general().spread() > 0.0F) {
            SpreadTracker.get(player).update(player, gunItem);
        }
        if (gun.projectile().hasProjectile()) {
            spawnProjectiles(level, player, held, gun);
        }
        GunEffects.afterShot(level, player, held, gun);
        playFireSound(level, player, held, gun);
        if (!player.isCreative() && !GunComponents.ignoresAmmo(held)) {
            GunComponents.setAmmo(held, Math.max(0, GunComponents.ammo(held) - 1));
            if (gun.reloads().reloadType() == ReloadType.INVENTORY_FED) {
                ReloadTracker.inventoryFeed(player, gun);
            }
        }
        player.awardStat(Stats.ITEM_USED.get(gunItem));
        if (player.getRandom().nextFloat() <= 0.1F && !gun.general().silenced()) {
            GunEffects.alertMobs(player, held);
        }
    }

    // The checks that stop a shot: firing underwater, a pulse gun still cooling, a gun still being drawn, or one about to break.
    private static boolean canFire(ServerPlayer player, ItemStack held, Gun gun) {
        if (!gun.general().canFireUnderwater() && player.isUnderWater() && !ArsenalConfig.get().underwaterFiring) {
            return false;
        }
        if (player.getCooldowns().isOnCooldown(held) && gun.general().fireMode() == FireMode.PULSE) {
            return false;
        }
        if (DrawTracker.get(player).isDrawing(player, false)) {
            return false;
        }
        if (held.isDamageableItem() && held.getDamageValue() >= held.getMaxDamage() - 1) {
            player.level().playSound(player, player.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.getCooldowns().addCooldown(held, gun.general().rate());
            return false;
        }
        return true;
    }

    private static void spawnProjectiles(ServerLevel level, ServerPlayer player, ItemStack held, Gun gun) {
        int count = gun.general().projectileAmount();
        List<BulletTrailPayload.Trail> trails = new ArrayList<>();
        ProjectileEntity first = null;
        for (int i = 0; i < count; i++) {
            ProjectileEntity projectile = create(level, player, held, gun);
            level.addFreshEntity(projectile);
            projectile.tick();
            trails.add(new BulletTrailPayload.Trail(projectile.getId(), projectile.position(), projectile.getDeltaMovement()));
            if (first == null) {
                first = projectile;
            }
        }
        Gun.Projectile props = gun.projectile();
        if (first != null && !props.hideTrail() && !props.visible()) {
            BulletTrailPayload payload = new BulletTrailPayload(
                trails,
                props.trailColor(),
                props.trailLengthMultiplier(),
                props.life(),
                first.projectileGravity(),
                player.getId()
            );
            GunNetwork.sendNear(level, player.position().add(0.0, 1.0, 0.0), ArsenalConfig.get().projectileTrackingRange, payload);
        }
    }

    // A projectile of the kind the gun's ammo fires.
    private static ProjectileEntity create(ServerLevel level, ServerPlayer player, ItemStack held, Gun gun) {
        Item ammo = BuiltInRegistries.ITEM.getValue(gun.projectile().item());
        if (ammo == GunItems.BLAZE_ROUND) {
            return new BlazeProjectileEntity(GunEntities.BLAZE_PROJECTILE, level, player, held, gun);
        }
        if (ammo == GunItems.SPECTRE_ROUND) {
            return new SpectreProjectileEntity(GunEntities.SPECTRE_PROJECTILE, level, player, held, gun);
        }
        if (ammo == GunItems.ROCKET) {
            return new RocketEntity(GunEntities.ROCKET, level, player, held, gun);
        }
        return new ProjectileEntity(GunEntities.PROJECTILE, level, player, held, gun);
    }

    // The shot's sound for everyone else nearby, and an unattenuated copy for the shooter that also flashes their muzzle.
    private static void playFireSound(ServerLevel level, ServerPlayer player, ItemStack held, Gun gun) {
        Identifier fireSound = gun.general().silenced() && gun.sounds().silencedFire() != null
            ? gun.sounds().silencedFire()
            : gun.sounds().fire();
        if (fireSound == null) {
            return;
        }
        Vec3 position = new Vec3(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
        float volume = ArsenalConfig.get().playerGunfireVolume;
        float pitch = 0.9F + level.getRandom().nextFloat() * 0.2F;
        if (player.isUnderWater()) {
            pitch = 0.7F + level.getRandom().nextFloat() * 0.2F;
            volume /= 2.0F;
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 10.0F, 1.7F);
        }
        level.playSound(player, position.x, position.y, position.z, GunSounds.byId(fireSound), SoundSource.PLAYERS, volume - 0.5F, pitch);
        boolean muzzle = gun.display().flash() != null;
        ServerPlayNetworking.send(player, new GunSoundPayload(fireSound, position, 0.7F, pitch, player.getId(), muzzle, false));
    }

    // Plays the held gun's pre-fire sound to everyone nearby, for guns that wind up before firing.
    public static void preFireSound(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GunItem gunItem) || !GunComponents.hasAmmo(held) && !player.isCreative()) {
            return;
        }
        Identifier sound = gunItem.getGun(false).sounds().preFire();
        if (sound == null) {
            return;
        }
        Vec3 position = new Vec3(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
        float pitch = 0.9F + player.getRandom().nextFloat() * 0.2F;
        GunSoundPayload payload = new GunSoundPayload(sound, position, 1.0F, pitch, player.getId(), false, false);
        GunNetwork.sendNear(player.level(), position, ArsenalConfig.get().gunShotMaxDistance, payload);
    }

    // Sweeps the gun at whatever is close in front: a light knock up and a little damage.
    public static void melee(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GunItem) || player.getCooldowns().isOnCooldown(held)) {
            return;
        }
        if (DrawTracker.get(player).isDrawing(player, false)) {
            return;
        }
        ServerLevel level = player.level();
        level.playSound(null, player.getOnPos(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 2.0F, 1.0F);
        player.getCooldowns().addCooldown(held, player.isSprinting() ? 40 : 15);
        Vec3 origin = player.position();
        Vec3 look = player.getLookAngle();
        AABB area = player.getBoundingBox().inflate(MELEE_RANGE);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            Vec3 offset = entity.position().subtract(origin);
            double angle = Math.acos(offset.normalize().dot(look.normalize()));
            if (angle < MELEE_SWEEP / 2.0 && entity != player) {
                entity.push(0.0, 0.5, 0.0);
                entity.hurtServer(level, level.damageSources().playerAttack(player), 1.0F / 1.5F);
            }
        }
        Vec3 sweep = player.position().add(look.x * 1.8, look.y * 1.8 + player.getEyeHeight(), look.z * 1.8);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, sweep.x, sweep.y, sweep.z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    // Empties a magazine or hand-loaded gun back into the inventory, dropping what does not fit.
    public static void unload(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GunItem gunItem)) {
            return;
        }
        Gun gun = gunItem.getGun(false);
        ReloadType type = gun.reloads().reloadType();
        if (type == ReloadType.SINGLE_ITEM || type == ReloadType.INVENTORY_FED || !held.has(GunComponents.AMMO_COUNT)) {
            return;
        }
        int count = GunComponents.ammo(held);
        GunComponents.setAmmo(held, 0);
        Item ammo = BuiltInRegistries.ITEM.getValue(gun.projectile().item());
        int maxStack = new ItemStack(ammo).getMaxStackSize();
        for (int i = 0; i < count / maxStack; i++) {
            giveAmmo(player, new ItemStack(ammo, maxStack));
        }
        if (count % maxStack > 0) {
            giveAmmo(player, new ItemStack(ammo, count % maxStack));
        }
    }

    private static void giveAmmo(ServerPlayer player, ItemStack stack) {
        player.getInventory().add(stack);
        if (stack.getCount() > 0) {
            player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack.copy()));
        }
    }

    // The overheat puff of smoke and fizz.
    public static void overheat(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        GunNetwork.particlesToAll(
            player.level(),
            ParticleTypes.CLOUD,
            player.getX() - player.getDeltaMovement().x + look.x,
            player.getEyeY() - 0.1 - player.getDeltaMovement().y + look.y,
            player.getZ() - player.getDeltaMovement().z + look.z,
            5,
            0.1,
            0.1,
            0.1,
            0.01
        );
        player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // A spent casing kicked out by the reload or bolt animation, seen by everyone nearby.
    public static void ejectCasing(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof GunItem gunItem) {
            GunEffects.ejectCasing(player.level(), player, gunItem.getGun(false));
        }
    }

    // Starts or stops the server-side reload the client asked for.
    public static void setReloading(ServerPlayer player, boolean reloading) {
        GunState.setReloading(player, reloading);
    }

    // Records whether the player is aiming down sights, which halves spread.
    public static void setAiming(ServerPlayer player, boolean aiming) {
        GunState.setAiming(player, aiming);
    }

    public static void setShooting(ServerPlayer player, boolean shooting) {
        GunState.setShooting(player, shooting);
    }
}
