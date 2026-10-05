package info.mudbourn.mmsarsenal.gun.net;

import info.mudbourn.mmsarsenal.MmsArsenal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Client to server: fire the held gun, aimed where the client was looking.
public record ShootPayload(float yaw, float pitch) implements CustomPacketPayload {

    public static final Type<ShootPayload> TYPE = new Type<>(MmsArsenal.id("shoot"));

    public static final StreamCodec<ByteBuf, ShootPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT,
        ShootPayload::yaw,
        ByteBufCodecs.FLOAT,
        ShootPayload::pitch,
        ShootPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
