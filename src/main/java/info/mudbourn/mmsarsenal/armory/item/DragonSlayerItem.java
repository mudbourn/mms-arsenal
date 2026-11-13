package info.mudbourn.mmsarsenal.armory.item;

import info.mudbourn.mmsarsenal.armory.ArmoryComponents;
import info.mudbourn.mmsarsenal.armory.ArmorySounds;
import info.mudbourn.mmsarsenal.armory.WeaponStats;
import info.mudbourn.mmsweapons.focus.Focus;
import net.bettercombat.logic.AnimatedHand;
import net.bettercombat.network.Packets;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

// Greatsword that spends a full bar of mms-weapons focus on a drawn-out focus sweep instead of the barrage.
public class DragonSlayerItem extends ArmoryWeaponItem {

    public static final int DRAW_TICKS = 20;
    private static final int MAX_HOLD_TICKS = 72000;
    public static final double FOCUS_MULTIPLIER = 2.0;
    public static final String SWEEP_ANIMATION = "bettercombat:two_handed_slash_horizontal_right";
    public static final float SWEEP_LENGTH = 16.0F;
    public static final float SWEEP_UPSWING = 0.5F;
    private static Consumer<Player> localSweep = player -> {
    };
    // Ticks from release to the blade coming through, when the client picks the sweep's targets.
    public static final int STRIKE_DELAY_TICKS = (int) (SWEEP_LENGTH * SWEEP_UPSWING);
    // How long after release the server accepts the client's hit report: 5 seconds.
    private static final int STRIKE_WINDOW_TICKS = 100;
    private static final Map<UUID, ArmedSweep> ARMED = new HashMap<>();

    public DragonSlayerItem(Properties properties) {
        this(new WeaponStats(11.0, -3.5745, 2.5, 3.0), properties);
    }

    protected DragonSlayerItem(WeaponStats stats, Properties properties) {
        super(stats, properties);
    }

    // Reach of the released focus sweep.
    public double chargedRange(Player player) {
        return player.entityInteractionRange() * FOCUS_MULTIPLIER;
    }

    // Better Combat animation the released focus attack plays.
    public String chargedAnimation() {
        return SWEEP_ANIMATION;
    }

    // Whether the released focus attack uses the weapon's forward stab hitbox instead of its first swing.
    public boolean chargedPierce() {
        return false;
    }

    // Sound played as the focus attack is released.
    protected SoundEvent chargedSound() {
        return ArmorySounds.DRAGON_SLAYER_SLASH;
    }

    // Plays the sweep animation for the local player, who gets no Better Combat packet for their own swing.
    public static void setLocalSweep(Consumer<Player> sweep) {
        localSweep = sweep;
    }

    // Moves focus saved under the old Armory component into the shared mms-weapons focus.
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity holder, EquipmentSlot slot) {
        super.inventoryTick(stack, level, holder, slot);
        Float legacy = stack.remove(ArmoryComponents.DAMAGE_DEALT);
        if (legacy != null) {
            stack.set(Focus.FOCUS, Math.min(Focus.MAX_FOCUS, Focus.focus(stack) + legacy));
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !Focus.focused(player.getItemInHand(hand))) {
            return InteractionResult.PASS;
        }
        // Crouching never starts the draw; mms-weapons also ignores right click after a crouch cancel until it is let go.
        if (player.isShiftKeyDown()) {
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

    // Crouching during the draw lowers the blade without spending the focus.
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (entity.isShiftKeyDown()) {
            entity.stopUsingItem();
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        // Crouching at release cancels the attack and keeps the focus.
        if (!(entity instanceof Player player) || player.isShiftKeyDown() || MAX_HOLD_TICKS - timeLeft < DRAW_TICKS || !Focus.focused(stack)) {
            return false;
        }
        if (!(level instanceof ServerLevel server)) {
            localSweep.accept(player);
            return true;
        }
        Packets.AttackAnimation animation = new Packets.AttackAnimation(
            player.getId(),
            AnimatedHand.TWO_HANDED,
            this.chargedAnimation(),
            SWEEP_LENGTH,
            SWEEP_UPSWING,
            (float) this.chargedRange(player),
            STRIKE_DELAY_TICKS,
            Packets.SwingParticles.EMPTY
        );
        for (ServerPlayer viewer : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(viewer, animation);
        }
        level.playSound(null, player.blockPosition(), this.chargedSound(), SoundSource.PLAYERS, 1.0F, 1.0F);
        float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * FOCUS_MULTIPLIER);
        ARMED.put(player.getUUID(), new ArmedSweep(server.getGameTime() + STRIKE_WINDOW_TICKS, damage));
        stack.set(Focus.FOCUS, 0.0F);
        return true;
    }

    // Lands a released sweep on every entity the attacker's client saw inside the arc as the blade came through.
    public static void strike(ServerPlayer player, List<Integer> targets) {
        ArmedSweep armed = ARMED.remove(player.getUUID());
        ServerLevel level = player.level();
        if (armed == null || level.getGameTime() > armed.until()) {
            return;
        }
        Focus.spend(() -> {
            for (int id : targets.stream().distinct().toList()) {
                if (level.getEntity(id) instanceof LivingEntity target && target != player && target.isAlive()) {
                    target.hurtServer(level, player.damageSources().playerAttack(player), armed.damage());
                }
            }
        });
        Vec3 front = player.getEyePosition().add(player.getLookAngle().scale(1.5));
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, front.x, front.y - 0.4, front.z, 3, 1.0, 0.1, 1.0, 0.0);
    }

    // A released sweep waiting on the client's hit report: the tick the report window closes and the damage fixed at release.
    private record ArmedSweep(long until, float damage) {
    }
}
