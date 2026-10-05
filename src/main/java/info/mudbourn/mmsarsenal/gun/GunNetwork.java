package info.mudbourn.mmsarsenal.gun;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

// Server-side sends: payloads to players near a point or watching a chunk, and particles everyone sees from afar.
public final class GunNetwork {

    private GunNetwork() {
    }

    public static void sendNear(ServerLevel level, Vec3 position, double radius, CustomPacketPayload payload) {
        double radiusSqr = radius * radius;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(position) <= radiusSqr) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    public static void sendTrackingChunk(ServerLevel level, BlockPos pos, CustomPacketPayload payload) {
        for (ServerPlayer player : PlayerLookup.tracking(level, new ChunkPos(pos))) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    // Sends particles to every player in the level, drawn even when far away.
    public static void particlesToAll(ServerLevel level, ParticleOptions particle, double x, double y, double z, int count, double dx, double dy, double dz, double speed) {
        for (ServerPlayer player : level.players()) {
            level.sendParticles(player, particle, true, false, x, y, z, count, dx, dy, dz, speed);
        }
    }
}
