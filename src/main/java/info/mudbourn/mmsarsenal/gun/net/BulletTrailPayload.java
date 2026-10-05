package info.mudbourn.mmsarsenal.gun.net;

import info.mudbourn.mmsarsenal.MmsArsenal;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

// Server to client: the trails of every projectile one shot fired, simulated on the client.
public record BulletTrailPayload(
    List<Trail> trails,
    int trailColor,
    double trailLengthMultiplier,
    int life,
    double gravity,
    int shooterId
) implements CustomPacketPayload {

    public static final Type<BulletTrailPayload> TYPE = new Type<>(MmsArsenal.id("bullet_trail"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BulletTrailPayload> CODEC = StreamCodec.composite(
        Trail.CODEC.apply(ByteBufCodecs.list()),
        BulletTrailPayload::trails,
        ByteBufCodecs.VAR_INT,
        BulletTrailPayload::trailColor,
        ByteBufCodecs.DOUBLE,
        BulletTrailPayload::trailLengthMultiplier,
        ByteBufCodecs.VAR_INT,
        BulletTrailPayload::life,
        ByteBufCodecs.DOUBLE,
        BulletTrailPayload::gravity,
        ByteBufCodecs.VAR_INT,
        BulletTrailPayload::shooterId,
        BulletTrailPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // One projectile's entity id, start position and velocity.
    public record Trail(int entityId, Vec3 position, Vec3 motion) {

        static final StreamCodec<RegistryFriendlyByteBuf, Trail> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            Trail::entityId,
            Vec3.STREAM_CODEC,
            Trail::position,
            Vec3.STREAM_CODEC,
            Trail::motion,
            Trail::new
        );
    }
}
