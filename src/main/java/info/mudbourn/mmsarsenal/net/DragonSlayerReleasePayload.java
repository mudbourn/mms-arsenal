package info.mudbourn.mmsarsenal.net;

import info.mudbourn.mmsarsenal.MmsArsenal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Server to client: the caster's own focus attack was released, so its swing plays and its targets are picked.
public record DragonSlayerReleasePayload() implements CustomPacketPayload {

    public static final Type<DragonSlayerReleasePayload> TYPE = new Type<>(MmsArsenal.id("dragon_slayer_release"));

    public static final StreamCodec<ByteBuf, DragonSlayerReleasePayload> CODEC = StreamCodec.unit(new DragonSlayerReleasePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
