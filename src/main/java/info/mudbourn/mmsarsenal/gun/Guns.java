package info.mudbourn.mmsarsenal.gun;

import info.mudbourn.mmsarsenal.config.ArsenalConfig;
import info.mudbourn.mmsarsenal.gun.entity.GunEntities;
import info.mudbourn.mmsarsenal.gun.net.BulletTrailPayload;
import info.mudbourn.mmsarsenal.gun.net.GunActionPayload;
import info.mudbourn.mmsarsenal.gun.net.GunFlagPayload;
import info.mudbourn.mmsarsenal.gun.net.GunSoundPayload;
import info.mudbourn.mmsarsenal.gun.net.GunsSyncPayload;
import info.mudbourn.mmsarsenal.gun.net.ProjectileHitPayloads;
import info.mudbourn.mmsarsenal.gun.net.ShootPayload;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRules;

// Wires the guns: content, the gun data loader, payloads, per-tick trackers and kill drops.
public final class Guns {

    private Guns() {
    }

    public static void register() {
        ArsenalConfig.load();
        GunComponents.register();
        GunState.register();
        GunSounds.register();
        GunParticles.register();
        GunMobEffects.register();
        GunEntities.register();
        GunItems.register();
        ResourceLoader.get(PackType.SERVER_DATA).registerReloader(GunManager.ID, new GunManager());
        registerPayloads();
        registerReceivers();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
            sender.sendPacket(new GunsSyncPayload(GunManager.sources())));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> syncAll(server));
        ServerTickEvents.START_SERVER_TICK.register(Guns::tick);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> dropEchoShards(entity));
    }

    private static void registerPayloads() {
        PayloadTypeRegistry.playC2S().register(ShootPayload.TYPE, ShootPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GunActionPayload.TYPE, GunActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GunFlagPayload.TYPE, GunFlagPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GunsSyncPayload.TYPE, GunsSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BulletTrailPayload.TYPE, BulletTrailPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GunSoundPayload.TYPE, GunSoundPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ProjectileHitPayloads.Block.TYPE, ProjectileHitPayloads.Block.CODEC);
        PayloadTypeRegistry.playS2C().register(ProjectileHitPayloads.Entity.TYPE, ProjectileHitPayloads.Entity.CODEC);
        PayloadTypeRegistry.playS2C().register(ProjectileHitPayloads.Remove.TYPE, ProjectileHitPayloads.Remove.CODEC);
    }

    private static void registerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(ShootPayload.TYPE, (payload, context) ->
            GunServer.shoot(context.player(), payload.yaw(), payload.pitch()));
        ServerPlayNetworking.registerGlobalReceiver(GunActionPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            switch (payload.action()) {
                case UNLOAD -> GunServer.unload(player);
                case MELEE -> GunServer.melee(player);
                case PRE_FIRE_SOUND -> GunServer.preFireSound(player);
                case OVERHEAT -> GunServer.overheat(player);
                case CASING -> GunServer.ejectCasing(player);
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(GunFlagPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player.isSpectator()) {
                return;
            }
            switch (payload.flag()) {
                case AIMING -> GunServer.setAiming(player, payload.value());
                case SHOOTING -> GunServer.setShooting(player, payload.value());
                case RELOADING -> GunServer.setReloading(player, payload.value());
                case FIRST_PERSON_RELOAD -> ReloadTracker.setFirstPerson(player, payload.value());
            }
        });
    }

    private static void syncAll(MinecraftServer server) {
        GunsSyncPayload payload = new GunsSyncPayload(GunManager.sources());
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            DrawTracker.get(player).tick(player, false);
            ReloadTracker.tick(player);
        }
    }

    // A kill with an echo-fed gun shakes loose an echo shard or two.
    private static void dropEchoShards(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level) || !ArsenalConfig.get().entitiesDropEchoShards) {
            return;
        }
        LivingEntity killer = entity.getKillCredit();
        if (!(killer instanceof Player) || !level.getGameRules().get(GameRules.MOB_DROPS)) {
            return;
        }
        ItemStack weapon = killer.getMainHandItem();
        Gun gun = GunItem.gun(weapon, false);
        if (gun == null) {
            return;
        }
        String ammo = gun.projectile().item().toString();
        if (!ammo.equals("minecraft:echo_shard") && !ammo.equals("minecraft:sculk_catalyst")) {
            return;
        }
        int count = level.getRandom().nextDouble() < 0.3 ? 2 : 1;
        level.addFreshEntity(new ItemEntity(level, entity.getBlockX(), entity.getBlockY(), entity.getBlockZ(), new ItemStack(Items.ECHO_SHARD, count)));
        level.playSound(null, killer.blockPosition(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.playSound(null, entity.blockPosition(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 10.0F, 1.0F);
    }
}
