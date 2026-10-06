package info.mudbourn.mmsarsenal.armory.item;

import info.mudbourn.mmsarsenal.armory.ArmoryComponents;
import info.mudbourn.mmsarsenal.armory.ArmorySounds;
import info.mudbourn.mmsarsenal.armory.WeaponStats;
import net.bettercombat.logic.AnimatedHand;
import net.bettercombat.network.Packets;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Consumer;

// Greatsword that builds focus from damage dealt and spends a full bar on a drawn-out focus sweep.
public class DragonSlayerItem extends ArmoryWeaponItem {

    public static final float MAX_FOCUS = 100.0F;
    public static final int DRAW_TICKS = 20;
    private static final int MAX_HOLD_TICKS = 72000;
    private static final double FOCUS_MULTIPLIER = 2.0;
    private static final int PARTICLE_INTERVAL = 2;
    public static final String SWEEP_ANIMATION = "bettercombat:two_handed_slash_horizontal_right";
    public static final float SWEEP_LENGTH = 16.0F;
    public static final float SWEEP_UPSWING = 0.5F;
    private static Consumer<Player> localSweep = player -> {
    };
    private static final DustParticleOptions FOCUS_PARTICLE = new DustParticleOptions(0x101010, 0.6F);
    private static final double SWEEP_HALF_ANGLE = Math.toRadians(75.0);

    public DragonSlayerItem(Properties properties) {
        super(new WeaponStats(11.0, -3.5745, 2.5, 3.0), properties);
    }

    // Plays the sweep animation for the local player, who gets no Better Combat packet for their own swing.
    public static void setLocalSweep(Consumer<Player> sweep) {
        localSweep = sweep;
    }

    public static float focus(ItemStack stack) {
        return Math.min(MAX_FOCUS, stack.getOrDefault(ArmoryComponents.DAMAGE_DEALT, 0.0F));
    }

    public static boolean focused(ItemStack stack) {
        return focus(stack) >= MAX_FOCUS;
    }

    // Adds damage dealt with this stack to its focus, up to a full bar.
    public static void recordDamage(ItemStack stack, float amount) {
        stack.set(ArmoryComponents.DAMAGE_DEALT, Math.min(MAX_FOCUS, focus(stack) + amount));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !focused(player.getItemInHand(hand))) {
            return InteractionResult.PASS;
        }
        player.startUsingItem(hand);
        level.playSound(player, player.blockPosition(), ArmorySounds.DRAGON_SLAYER_CHARGE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_HOLD_TICKS;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player) || MAX_HOLD_TICKS - timeLeft < DRAW_TICKS || !focused(stack)) {
            return false;
        }
        if (!(level instanceof ServerLevel server)) {
            localSweep.accept(player);
            return true;
        }
        Packets.AttackAnimation animation = new Packets.AttackAnimation(
            player.getId(),
            AnimatedHand.TWO_HANDED,
            SWEEP_ANIMATION,
            SWEEP_LENGTH,
            SWEEP_UPSWING,
            (float) (player.entityInteractionRange() * FOCUS_MULTIPLIER),
            (int) (SWEEP_LENGTH * SWEEP_UPSWING),
            Packets.SwingParticles.EMPTY
        );
        for (ServerPlayer viewer : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(viewer, animation);
        }
        level.playSound(null, player.blockPosition(), ArmorySounds.DRAGON_SLAYER_SLASH, SoundSource.PLAYERS, 1.0F, 1.0F);
        float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * FOCUS_MULTIPLIER);
        for (LivingEntity target : sweepTargets(player, player.entityInteractionRange() * FOCUS_MULTIPLIER)) {
            target.hurtServer(server, player.damageSources().playerAttack(player), damage);
        }
        Vec3 front = player.getEyePosition().add(player.getLookAngle().scale(1.5));
        server.sendParticles(ParticleTypes.SWEEP_ATTACK, front.x, front.y - 0.4, front.z, 3, 1.0, 0.1, 1.0, 0.0);
        stack.set(ArmoryComponents.DAMAGE_DEALT, 0.0F);
        return true;
    }

    // Living entities in front of the player within range and in sight, all struck by the sweep.
    private static List<LivingEntity> sweepTargets(Player player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double minDot = Math.cos(SWEEP_HALF_ANGLE);
        return player.level().getEntitiesOfClass(
            LivingEntity.class,
            player.getBoundingBox().inflate(range),
            target -> target != player
                && target.isAlive()
                && !target.isSpectator()
                && target.distanceTo(player) <= range
                && target.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) >= minDot
                && player.hasLineOfSight(target)
        );
    }

    @Override
    protected void tickAbility(ItemStack stack, ServerLevel level, Entity holder) {
        if (!focused(stack) || !(holder instanceof Player player) || player.getMainHandItem() != stack) {
            return;
        }
        if (level.getGameTime() % PARTICLE_INTERVAL == 0) {
            Vec3 blade = bladePosition(player);
            level.sendParticles(FOCUS_PARTICLE, blade.x, blade.y, blade.z, 2, 0.08, 0.35, 0.08, 0.0);
        }
    }

    // Midpoint of the blade held at the player's side, following body rotation rather than the camera.
    private static Vec3 bladePosition(Player player) {
        double yaw = Math.toRadians(player.yBodyRot);
        double side = player.getMainArm() == HumanoidArm.RIGHT ? -1.0 : 1.0;
        double x = -Math.sin(yaw) * 0.3 + Math.cos(yaw) * side * 0.45;
        double z = Math.cos(yaw) * 0.3 + Math.sin(yaw) * side * 0.45;
        return new Vec3(player.getX() + x, player.getY() + 1.0, player.getZ() + z);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return focus(stack) > 0.0F;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(focus(stack) / MAX_FOCUS * 13.0F);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return ARGB.colorFromFloat(1.0F, 0.9F, 0.1F, 0.1F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.mms_arsenal.dragon_slayer.focus", (int) focus(stack), (int) MAX_FOCUS)
            .withStyle(ChatFormatting.GRAY));
    }
}
