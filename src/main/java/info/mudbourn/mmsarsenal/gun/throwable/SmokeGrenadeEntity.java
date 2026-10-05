package info.mudbourn.mmsarsenal.gun.throwable;

import info.mudbourn.mmsarsenal.config.ArsenalConfig;
import info.mudbourn.mmsarsenal.gun.GunItems;
import info.mudbourn.mmsarsenal.gun.GunMobEffects;
import info.mudbourn.mmsarsenal.gun.GunParticles;
import info.mudbourn.mmsarsenal.gun.GunSounds;
import info.mudbourn.mmsarsenal.gun.entity.GunEntities;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// A smoke grenade: bursts into a column of thick smoke that blinds anyone inside and snuffs out any fire the smoke touches.
public class SmokeGrenadeEntity extends TimedThrowableEntity {

    // How far above the grenade the smoke column reaches.
    private static final double COLUMN_HEIGHT = 4.0;
    // Wide smoke puffs spawned across the column each tick; each lives up to sixteen seconds, so hundreds overlap and one fading never leaves a hole.
    private static final int PLUMES_PER_TICK = 3;
    // Puffs that fill the column the moment it bursts, so it starts solid instead of building up.
    private static final int BURST_PLUMES = 300;
    // How often the Smoked effect is refreshed on everyone inside, in ticks.
    private static final int EFFECT_INTERVAL = 10;
    // How long a player's Smoked lasts between refreshes; it is removed outright once they leave.
    private static final int PLAYER_SMOKE_TICKS = 40;

    // Step between sight samples along a line through the smoke, in blocks.
    private static final double SIGHT_STEP = 0.5;

    private static final List<Column> columns = new ArrayList<>();

    // One burst's smoke: where it sits, how wide it is and how long it has left.
    private static final class Column {
        private final ServerLevel level;
        private final Vec3 base;
        private final double radius;
        private int ticksLeft;

        private Column(ServerLevel level, Vec3 base, double radius, int ticksLeft) {
            this.level = level;
            this.base = base;
            this.radius = radius;
            this.ticksLeft = ticksLeft;
        }

        private boolean contains(Vec3 pos) {
            double dy = pos.y - this.base.y;
            return dy >= -1.0 && dy <= COLUMN_HEIGHT + 1.0 && pos.subtract(this.base).horizontalDistanceSqr() <= this.radius * this.radius;
        }
    }

    public SmokeGrenadeEntity(EntityType<? extends SmokeGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public SmokeGrenadeEntity(Level level, LivingEntity owner, int fuseTicks) {
        super(GunEntities.SMOKE_GRENADE, level, owner, fuseTicks);
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(SmokeGrenadeEntity::tickColumns);
    }

    @Override
    protected Item getDefaultItem() {
        return GunItems.SMOKE_GRENADE;
    }

    @Override
    protected void spawnFlightParticles() {
        this.level().addParticle(
            ParticleTypes.CAMPFIRE_COSY_SMOKE,
            this.getX(),
            this.getY() + 0.2,
            this.getZ(),
            (this.random.nextDouble() - 0.5) * 0.08,
            0.08,
            (this.random.nextDouble() - 0.5) * 0.08
        );
    }

    @Override
    protected void explode() {
        ServerLevel level = (ServerLevel) this.level();
        ArsenalConfig config = ArsenalConfig.get();
        level.playSound(null, this.getX(), this.getY(), this.getZ(), GunSounds.SMOKE_GRENADE_EXPLOSION, SoundSource.NEUTRAL, 2.0F, 1.0F);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 60, 0.8, 1.2, 0.8, 0.02);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 20, 0.6, 0.8, 0.6, 0.01);
        Column column = new Column(level, this.position(), config.smokeGrenadeCloudDiameter / 2.0, (int) (config.smokeGrenadeCloudDuration * 20.0));
        columns.add(column);
        for (int i = 0; i < BURST_PLUMES; i++) {
            spawnPlume(column);
        }
        extinguishFire(column);
    }

    // Whether a line of sight from one point to another passes into any live smoke column in the level.
    public static boolean blocksSight(Level level, Vec3 from, Vec3 to) {
        if (columns.isEmpty()) {
            return false;
        }
        Vec3 path = to.subtract(from);
        int steps = Math.max(1, Mth.ceil(path.length() / SIGHT_STEP));
        for (Column column : columns) {
            if (column.level != level) {
                continue;
            }
            for (int i = 0; i <= steps; i++) {
                if (column.contains(from.add(path.scale(i / (double) steps)))) {
                    return true;
                }
            }
        }
        return false;
    }

    // Billows smoke through each live column and keeps everyone inside it Smoked.
    private static void tickColumns(MinecraftServer server) {
        Iterator<Column> it = columns.iterator();
        while (it.hasNext()) {
            Column column = it.next();
            if (--column.ticksLeft <= 0 || server.getLevel(column.level.dimension()) != column.level) {
                it.remove();
                continue;
            }
            for (int i = 0; i < PLUMES_PER_TICK; i++) {
                spawnPlume(column);
            }
            if (column.ticksLeft % EFFECT_INTERVAL == 0) {
                extinguishFire(column);
                AABB box = new AABB(column.base, column.base).inflate(column.radius, 0.0, column.radius).expandTowards(0.0, COLUMN_HEIGHT, 0.0);
                for (LivingEntity entity : column.level.getEntitiesOfClass(LivingEntity.class, box)) {
                    if (!(entity instanceof ServerPlayer) && entity.position().subtract(column.base).horizontalDistanceSqr() <= column.radius * column.radius) {
                        entity.addEffect(new MobEffectInstance(GunMobEffects.SMOKED, 40, 0, false, false, true));
                    }
                }
            }
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            smokePlayer(player);
        }
    }

    // Keeps a player Smoked exactly while their eyes are in a column, so the fog lifts the tick they step out.
    private static void smokePlayer(ServerPlayer player) {
        Vec3 eyes = player.getEyePosition();
        boolean inSmoke = columns.stream().anyMatch(column -> column.level == player.level() && column.contains(eyes));
        MobEffectInstance current = player.getEffect(GunMobEffects.SMOKED);
        if (inSmoke && (current == null || current.getDuration() < PLAYER_SMOKE_TICKS / 2)) {
            player.addEffect(new MobEffectInstance(GunMobEffects.SMOKED, PLAYER_SMOKE_TICKS, 0, false, false, true));
        } else if (!inSmoke && current != null) {
            player.removeEffect(GunMobEffects.SMOKED);
        }
    }

    // One smoke puff at a random spot in the column; its x speed carries the column's remaining ticks so the puff never outlasts the smoke.
    private static void spawnPlume(Column column) {
        double angle = column.level.getRandom().nextDouble() * Math.PI * 2.0;
        double distance = Math.sqrt(column.level.getRandom().nextDouble()) * column.radius;
        column.level.sendParticles(
            GunParticles.SMOKE_CLOUD,
            column.base.x + Math.cos(angle) * distance,
            column.base.y + column.level.getRandom().nextDouble() * COLUMN_HEIGHT,
            column.base.z + Math.sin(angle) * distance,
            0,
            column.ticksLeft,
            0.0,
            0.0,
            1.0
        );
    }

    // Snuffs out every fire, lit campfire and lit candle the smoke column touches, from a block below the grenade to the column's top.
    private static void extinguishFire(Column column) {
        ServerLevel level = column.level;
        BlockPos center = BlockPos.containing(column.base);
        int reach = Mth.ceil(column.radius);
        double radiusSqr = column.radius * column.radius;
        boolean extinguished = false;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-reach, -1, -reach), center.offset(reach, Mth.ceil(COLUMN_HEIGHT), reach))) {
            double dx = pos.getX() + 0.5 - column.base.x;
            double dz = pos.getZ() + 0.5 - column.base.z;
            if (dx * dx + dz * dz > radiusSqr) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BaseFireBlock) {
                level.removeBlock(pos, false);
                extinguished = true;
            } else if (CampfireBlock.isLitCampfire(state)) {
                CampfireBlock.dowse(null, level, pos, state);
                level.setBlock(pos, state.setValue(CampfireBlock.LIT, false), 11);
                extinguished = true;
            } else if (AbstractCandleBlock.isLit(state)) {
                AbstractCandleBlock.extinguish(null, state, level, pos);
                extinguished = true;
            }
        }
        if (extinguished) {
            level.playSound(null, center, SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.8F, 1.0F);
        }
    }
}
