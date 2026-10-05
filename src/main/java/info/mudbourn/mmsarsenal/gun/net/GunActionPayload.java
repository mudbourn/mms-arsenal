package info.mudbourn.mmsarsenal.gun.net;

import info.mudbourn.mmsarsenal.MmsArsenal;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ByIdMap;
import java.util.function.IntFunction;

// Client to server: a one-shot gun action with no arguments.
public record GunActionPayload(Action action) implements CustomPacketPayload {

    public static final Type<GunActionPayload> TYPE = new Type<>(MmsArsenal.id("gun_action"));

    public static final StreamCodec<ByteBuf, GunActionPayload> CODEC = Action.STREAM_CODEC.map(GunActionPayload::new, GunActionPayload::action);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Action {
        UNLOAD,
        MELEE,
        PRE_FIRE_SOUND,
        OVERHEAT,
        CASING;

        private static final IntFunction<Action> BY_ID = ByIdMap.continuous(Action::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        static final StreamCodec<ByteBuf, Action> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Action::ordinal);
    }
}
