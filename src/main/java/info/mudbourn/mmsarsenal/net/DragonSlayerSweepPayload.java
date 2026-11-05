package info.mudbourn.mmsarsenal.net;

import info.mudbourn.mmsarsenal.MmsArsenal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

// Client to server: the entities the client saw inside the Dragon Slayer focus sweep arc when the blade came through.
public record DragonSlayerSweepPayload(List<Integer> targets) implements CustomPacketPayload {

    public static final Type<DragonSlayerSweepPayload> TYPE = new Type<>(MmsArsenal.id("dragon_slayer_sweep"));

    public static final StreamCodec<ByteBuf, DragonSlayerSweepPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()),
        DragonSlayerSweepPayload::targets,
        DragonSlayerSweepPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
