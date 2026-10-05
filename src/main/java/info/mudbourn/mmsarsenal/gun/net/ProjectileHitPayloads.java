package info.mudbourn.mmsarsenal.gun.net;

import info.mudbourn.mmsarsenal.MmsArsenal;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

// Server to client payloads for where projectiles land.
public final class ProjectileHitPayloads {

    private ProjectileHitPayloads() {
    }

    // A projectile struck a block face: bullet hole, sparks and impact sound.
    public record Block(Vec3 position, BlockPos pos, Direction face) implements CustomPacketPayload {

        public static final Type<Block> TYPE = new Type<>(MmsArsenal.id("projectile_hit_block"));

        public static final StreamCodec<ByteBuf, Block> CODEC = StreamCodec.composite(
            Vec3.STREAM_CODEC,
            Block::position,
            BlockPos.STREAM_CODEC,
            Block::pos,
            Direction.STREAM_CODEC,
            Block::face,
            Block::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // The shooter's projectile hit an entity: hit marker and hit sound, typed 0 normal, 1 headshot, 2 critical.
    public record Entity(Vec3 position, int hitType, boolean player) implements CustomPacketPayload {

        public static final Type<Entity> TYPE = new Type<>(MmsArsenal.id("projectile_hit_entity"));

        public static final StreamCodec<ByteBuf, Entity> CODEC = StreamCodec.composite(
            Vec3.STREAM_CODEC,
            Entity::position,
            ByteBufCodecs.VAR_INT,
            Entity::hitType,
            ByteBufCodecs.BOOL,
            Entity::player,
            Entity::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public boolean headshot() {
            return this.hitType == 1;
        }

        public boolean critical() {
            return this.hitType == 2;
        }
    }

    // A projectile is gone, so its client-side trail ends.
    public record Remove(int entityId) implements CustomPacketPayload {

        public static final Type<Remove> TYPE = new Type<>(MmsArsenal.id("remove_projectile"));

        public static final StreamCodec<ByteBuf, Remove> CODEC = ByteBufCodecs.VAR_INT.map(Remove::new, Remove::entityId);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
