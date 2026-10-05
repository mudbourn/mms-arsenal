package info.mudbourn.mmsarsenal.gun.net;

import info.mudbourn.mmsarsenal.MmsArsenal;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

// Server to client: a gun sound for the shooter or a nearby listener, optionally flashing the shooter's muzzle.
public record GunSoundPayload(
    Identifier sound,
    Vec3 position,
    float volume,
    float pitch,
    int shooterId,
    boolean muzzleFlash,
    boolean reload
) implements CustomPacketPayload {

    public static final Type<GunSoundPayload> TYPE = new Type<>(MmsArsenal.id("gun_sound"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GunSoundPayload> CODEC = StreamCodec.composite(
        Identifier.STREAM_CODEC,
        GunSoundPayload::sound,
        Vec3.STREAM_CODEC,
        GunSoundPayload::position,
        ByteBufCodecs.FLOAT,
        GunSoundPayload::volume,
        ByteBufCodecs.FLOAT,
        GunSoundPayload::pitch,
        ByteBufCodecs.VAR_INT,
        GunSoundPayload::shooterId,
        ByteBufCodecs.BOOL,
        GunSoundPayload::muzzleFlash,
        ByteBufCodecs.BOOL,
        GunSoundPayload::reload,
        GunSoundPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
