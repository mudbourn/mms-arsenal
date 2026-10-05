package info.mudbourn.mmsarsenal.gun.net;

import info.mudbourn.mmsarsenal.MmsArsenal;
import io.netty.buffer.ByteBuf;
import java.util.function.IntFunction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ByIdMap;

// Client to server: a gun state the client switched on or off.
public record GunFlagPayload(Flag flag, boolean value) implements CustomPacketPayload {

    public static final Type<GunFlagPayload> TYPE = new Type<>(MmsArsenal.id("gun_flag"));

    public static final StreamCodec<ByteBuf, GunFlagPayload> CODEC = StreamCodec.composite(
        Flag.STREAM_CODEC,
        GunFlagPayload::flag,
        ByteBufCodecs.BOOL,
        GunFlagPayload::value,
        GunFlagPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Flag {
        AIMING,
        SHOOTING,
        RELOADING,
        FIRST_PERSON_RELOAD;

        private static final IntFunction<Flag> BY_ID = ByIdMap.continuous(Flag::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
        static final StreamCodec<ByteBuf, Flag> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, Flag::ordinal);
    }
}
