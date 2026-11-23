package info.mudbourn.mmsarsenal.net;

import info.mudbourn.mmsarsenal.MmsArsenal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

// Server to client: the caster's own Sword's Race slash started, so its swing plays on their screen.
public record SwordsRacePayload() implements CustomPacketPayload {

    public static final Type<SwordsRacePayload> TYPE = new Type<>(MmsArsenal.id("swords_race_swing"));

    public static final StreamCodec<ByteBuf, SwordsRacePayload> CODEC = StreamCodec.unit(new SwordsRacePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
