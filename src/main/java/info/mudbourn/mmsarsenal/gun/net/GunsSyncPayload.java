package info.mudbourn.mmsarsenal.gun.net;

import info.mudbourn.mmsarsenal.MmsArsenal;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Server to client: every loaded gun's data file, by item id.
public record GunsSyncPayload(Map<Identifier, String> guns) implements CustomPacketPayload {

    public static final Type<GunsSyncPayload> TYPE = new Type<>(MmsArsenal.id("guns_sync"));

    public static final StreamCodec<ByteBuf, GunsSyncPayload> CODEC = ByteBufCodecs.<ByteBuf, Identifier, String, Map<Identifier, String>>map(
            HashMap::new,
            Identifier.STREAM_CODEC,
            ByteBufCodecs.stringUtf8(Integer.MAX_VALUE)
        )
        .map(GunsSyncPayload::new, GunsSyncPayload::guns);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
