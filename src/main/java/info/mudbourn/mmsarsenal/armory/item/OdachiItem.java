package info.mudbourn.mmsarsenal.armory.item;

import info.mudbourn.mmsarsenal.armory.ArmoryComponents;
import info.mudbourn.mmsarsenal.armory.ArmorySounds;
import info.mudbourn.mmsarsenal.armory.WeaponStats;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

// Long blade with 5 block reach whose skills are a focus-spending 8 block lunging pierce and Sword's Race, a heavy slash that hurls victims back.
public class OdachiItem extends DragonSlayerItem {

    public static final double CHARGED_RANGE = 8.0;
    private static final String PIERCE_ANIMATION = "bettercombat:two_handed_stab_right";
    private static final double LUNGE_SPEED = 1.4;
    private static final double LUNGE_LIFT = 0.15;
    private static final int RACE_ARMED = 1;
    private static final int RACE_STRUCK = 2;
    private static final int RACE_WINDOW_TICKS = 100;
    private static final double RACE_THROW = 1.6;
    private static final double RACE_LIFT = 0.45;
    private static final WeaponStats RACE = new WeaponStats(4.0, -0.6, 2.0, 0.0);

    public OdachiItem(Properties properties) {
        super(new WeaponStats(9.0, -3.1, 0.0, 2.0), properties);
    }

    // Whether Sword's Race can be readied: not already readied.
    public boolean raceReady(ItemStack stack) {
        return !stack.has(ArmoryComponents.ABILITY_TICK);
    }

    // Readies Sword's Race on the next swing.
    public void readyRace(ServerPlayer player, ItemStack stack) {
        stack.set(ArmoryComponents.ABILITY_TICK, RACE_ARMED);
        stack.set(ArmoryComponents.ANIMATION_START, player.level().getGameTime());
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    // Hurls every victim of the readied slash back and up.
    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!stack.has(ArmoryComponents.ABILITY_TICK) || !(attacker.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 away = target.position().subtract(attacker.position());
        Vec3 flat = new Vec3(away.x, 0.0, away.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(attacker.getLookAngle().x, 0.0, attacker.getLookAngle().z) : flat;
        Vec3 push = flat.normalize().scale(RACE_THROW);
        target.push(push.x, RACE_LIFT, push.z);
        target.needsSync = true;
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.5), target.getZ(), 2, 0.3, 0.2, 0.3, 0.0);
        if (stack.getOrDefault(ArmoryComponents.ABILITY_TICK, 0) == RACE_ARMED) {
            level.playSound(null, attacker.blockPosition(), ArmorySounds.DRAGON_SLAYER_SLASH, SoundSource.PLAYERS, 1.0F, 1.2F);
            stack.set(ArmoryComponents.ABILITY_TICK, RACE_STRUCK);
            stack.set(ArmoryComponents.ANIMATION_START, level.getGameTime());
        }
    }

    // Ends Sword's Race the tick after its slash lands, or once the readied window lapses.
    @Override
    protected void tickAbility(ItemStack stack, ServerLevel level, Entity holder) {
        Integer state = stack.get(ArmoryComponents.ABILITY_TICK);
        Long start = stack.get(ArmoryComponents.ANIMATION_START);
        if (state == null || start == null) {
            return;
        }
        long age = level.getGameTime() - start;
        if ((state == RACE_STRUCK && age > 0) || (state == RACE_ARMED && age >= RACE_WINDOW_TICKS)) {
            stack.remove(ArmoryComponents.ABILITY_TICK);
            stack.remove(ArmoryComponents.ANIMATION_START);
        }
    }

    @Override
    protected WeaponStats bonus(ItemStack stack) {
        return stack.has(ArmoryComponents.ABILITY_TICK) ? RACE : WeaponStats.NONE;
    }

    @Override
    public double chargedRange(Player player) {
        return CHARGED_RANGE;
    }

    @Override
    public String chargedAnimation() {
        return PIERCE_ANIMATION;
    }

    @Override
    public boolean chargedPierce() {
        return true;
    }

    @Override
    protected SoundEvent chargedSound() {
        return ArmorySounds.ODACHI_PIERCE;
    }

    // Throws the wielder forward along their facing as the pierce is released.
    @Override
    public void release(ServerPlayer player, ItemStack stack) {
        super.release(player, stack);
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z).normalize().scale(LUNGE_SPEED);
        player.push(flat.x, LUNGE_LIFT, flat.z);
        player.needsSync = true;
    }
}
