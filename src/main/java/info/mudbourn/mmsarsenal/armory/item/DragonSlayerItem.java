package info.mudbourn.mmsarsenal.armory.item;

import info.mudbourn.mmsarsenal.armory.ArmoryComponents;
import info.mudbourn.mmsarsenal.armory.ArmorySounds;
import info.mudbourn.mmsarsenal.armory.WeaponStats;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
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
    private static final int DRAW_TICKS = 20;
    private static final int MAX_HOLD_TICKS = 72000;
    private static final double FOCUS_MULTIPLIER = 2.0;
    private static final int PARTICLE_INTERVAL = 2;
    private static final double SWEEP_HALF_ANGLE = Math.toRadians(75.0);

    public DragonSlayerItem(Properties properties) {
        super(new WeaponStats(11.0, -3.6, 2.5, 3.0), properties);
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
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_HOLD_TICKS;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof Player player)) {
            return false;
        }
        if (MAX_HOLD_TICKS - timeLeft < DRAW_TICKS || !focused(stack)) {
            return false;
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        level.playSound(null, player.blockPosition(), ArmorySounds.DRAGON_SLAYER_SWING, SoundSource.PLAYERS, 1.0F, 0.6F);
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
            Vec3 blade = player.getEyePosition().add(player.getLookAngle().scale(0.8)).add(0.0, -0.5, 0.0);
            level.sendParticles(ParticleTypes.SQUID_INK, blade.x, blade.y, blade.z, 2, 0.25, 0.4, 0.25, 0.01);
        }
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
