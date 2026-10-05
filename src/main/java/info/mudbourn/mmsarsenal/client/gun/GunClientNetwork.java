package info.mudbourn.mmsarsenal.client.gun;

import info.mudbourn.mmsarsenal.config.ArsenalConfig;
import info.mudbourn.mmsarsenal.gun.GunManager;
import info.mudbourn.mmsarsenal.gun.GunParticles;
import info.mudbourn.mmsarsenal.gun.GunSounds;
import info.mudbourn.mmsarsenal.gun.GunTags;
import info.mudbourn.mmsarsenal.gun.net.BulletTrailPayload;
import info.mudbourn.mmsarsenal.gun.net.GunSoundPayload;
import info.mudbourn.mmsarsenal.gun.net.GunsSyncPayload;
import info.mudbourn.mmsarsenal.gun.net.ProjectileHitPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// Handles the gun payloads the server sends: gun data, trails, shot sounds, impacts and hit markers.
public final class GunClientNetwork {

    private static final double IMPACT_DISTANCE = 32.0;
    private static long lastHitSound;

    private GunClientNetwork() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(GunsSyncPayload.TYPE, (payload, context) -> GunManager.acceptClient(payload.guns()));
        ClientPlayNetworking.registerGlobalReceiver(BulletTrailPayload.TYPE, (payload, context) -> BulletTrails.get().add(payload));
        ClientPlayNetworking.registerGlobalReceiver(ProjectileHitPayloads.Remove.TYPE, (payload, context) -> BulletTrails.get().remove(payload.entityId()));
        ClientPlayNetworking.registerGlobalReceiver(GunSoundPayload.TYPE, (payload, context) -> onGunSound(payload));
        ClientPlayNetworking.registerGlobalReceiver(ProjectileHitPayloads.Block.TYPE, (payload, context) -> onHitBlock(payload));
        ClientPlayNetworking.registerGlobalReceiver(ProjectileHitPayloads.Entity.TYPE, (payload, context) -> onHitEntity(payload));
    }

    // The shooter hears their own shot flat and close; anyone else hears it fall off with distance.
    private static void onGunSound(GunSoundPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (payload.muzzleFlash()) {
            GunView.get().showMuzzleFlash(payload.shooterId());
        }
        if (payload.shooterId() == mc.player.getId()) {
            mc.getSoundManager().play(new SimpleSoundInstance(
                payload.sound(),
                SoundSource.PLAYERS,
                payload.volume(),
                payload.pitch(),
                mc.level.getRandom(),
                false,
                0,
                SoundInstance.Attenuation.NONE,
                0.0,
                0.0,
                0.0,
                true
            ));
        } else {
            mc.getSoundManager().play(new GunShotSound(payload, mc.level.getRandom()));
        }
    }

    private static void onHitBlock(ProjectileHitPayloads.Block payload) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            return;
        }
        BlockState state = level.getBlockState(payload.pos());
        Vec3 hit = payload.position();
        double holeX = hit.x + 0.005 * payload.face().getStepX();
        double holeY = hit.y + 0.005 * payload.face().getStepY();
        double holeZ = hit.z + 0.005 * payload.face().getStepZ();
        double distance = Math.sqrt(mc.player.distanceToSqr(hit));
        GunParticleClient.addBulletHole(level, holeX, holeY, holeZ, payload.face(), payload.pos());
        Vec3 motion = new Vec3(payload.face().getStepX(), payload.face().getStepY(), payload.face().getStepZ());
        RandomSource random = level.getRandom();
        SoundType soundType = state.getSoundType();
        if (state.is(GunTags.SQUISHY)) {
            level.playLocalSound(hit.x, hit.y, hit.z, GunSounds.SQUISHY_HIT, SoundSource.BLOCKS, 1.0F, 1.0F, false);
        } else if (state.is(GunTags.METAL) || soundType == SoundType.METAL) {
            level.playLocalSound(hit.x, hit.y, hit.z, GunSounds.METAL_HIT, SoundSource.BLOCKS, 0.5F, 1.0F, false);
            sparks(level, hit, motion);
        } else if (state.is(GunTags.STONE) || soundType == SoundType.STONE) {
            level.playLocalSound(hit.x, hit.y, hit.z, GunSounds.STONE_HIT, SoundSource.BLOCKS, 0.4F, 1.0F, false);
            level.addParticle(ParticleTypes.CLOUD, true, false, hit.x, hit.y, hit.z, motion.x * random.nextFloat() / 10.0, motion.y * random.nextFloat() / 10.0, motion.z * random.nextFloat() / 10.0);
            sparks(level, hit, motion);
        } else if (state.is(BlockTags.MINEABLE_WITH_AXE) || state.is(GunTags.WOOD) || soundType == SoundType.WOOD) {
            level.playLocalSound(hit.x, hit.y, hit.z, GunSounds.WOOD_HIT, SoundSource.BLOCKS, 0.4F, 1.0F, false);
            level.addParticle(ParticleTypes.CLOUD, false, false, hit.x, hit.y, hit.z, motion.x * random.nextFloat() / 10.0, motion.y * random.nextFloat() / 10.0, motion.z * random.nextFloat() / 10.0);
        }
        if (distance < IMPACT_DISTANCE) {
            for (int i = 0; i < 4; i++) {
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), hit.x, hit.y, hit.z, motion.x, motion.y, motion.z);
            }
        }
        if (distance <= IMPACT_DISTANCE) {
            level.playLocalSound(hit.x, hit.y, hit.z, soundType.getBreakSound(), SoundSource.BLOCKS, 1.0F, 2.0F, false);
        }
    }

    private static void sparks(ClientLevel level, Vec3 hit, Vec3 motion) {
        for (int i = 0; i < 2; i++) {
            level.addParticle(GunParticles.SPARK, hit.x, hit.y, hit.z, motion.x, motion.y, motion.z);
        }
    }

    // A hit marker and a hit sound, no more than one sound every fifty milliseconds.
    private static void onHitEntity(ProjectileHitPayloads.Entity payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        GunView.get().playHitMarker(payload.critical() || payload.headshot());
        SoundEvent sound = payload.critical()
            ? SoundEvents.PLAYER_ATTACK_CRIT
            : payload.headshot()
            ? SoundEvents.PLAYER_ATTACK_KNOCKBACK
            : payload.player()
            ? SoundEvents.PLAYER_HURT
            : GunSounds.HIT_MARKER;
        long now = System.currentTimeMillis();
        if (now - lastHitSound > 50L) {
            lastHitSound = now;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F, 1.0F + mc.level.getRandom().nextFloat() * 0.2F));
        }
    }

    // A distant gunshot, fading with distance out to the gunshot range and squared for a sharper falloff.
    private static final class GunShotSound extends AbstractSoundInstance {

        GunShotSound(GunSoundPayload payload, RandomSource random) {
            super(payload.sound(), SoundSource.PLAYERS, random);
            this.x = payload.position().x;
            this.y = payload.position().y;
            this.z = payload.position().z;
            this.pitch = payload.pitch();
            this.attenuation = SoundInstance.Attenuation.NONE;
            Minecraft mc = Minecraft.getInstance();
            ArsenalConfig config = ArsenalConfig.get();
            float range = (float) (payload.reload() ? config.reloadMaxDistance : config.gunShotMaxDistance);
            float falloff = 1.0F - Math.min(1.0F, (float) Math.sqrt(mc.player.distanceToSqr(this.x, this.y, this.z)) / range);
            float volume = payload.volume() * falloff;
            this.volume = volume * volume;
        }
    }
}
