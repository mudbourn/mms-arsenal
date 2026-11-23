package info.mudbourn.mmsarsenal.armory.item;

import info.mudbourn.mmsarsenal.armory.ArmoryComponents;
import info.mudbourn.mmsarsenal.armory.ArmorySounds;
import info.mudbourn.mmsarsenal.armory.WeaponStats;
import info.mudbourn.mmsarsenal.net.SwordsRacePayload;
import info.mudbourn.mmsweapons.focus.Engage;
import info.mudbourn.mmsweapons.focus.Focus;
import net.bettercombat.logic.AnimatedHand;
import net.bettercombat.network.Packets;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
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

// Long blade with 5 block reach whose skills are a focus-spending 8 block lunging pierce and Sword's Race, a placed heavy slash swung for the wielder that hurls victims back.
public class OdachiItem extends DragonSlayerItem {

    public static final double CHARGED_RANGE = 8.0;
    private static final String PIERCE_ANIMATION = "bettercombat:two_handed_stab_right";
    private static final double LUNGE_SPEED = 1.4;
    // Edge-to-edge spacing a close enemy is stabbed from, so the long blade reaches it instead of passing over it.
    private static final double PIERCE_GAP = 2.0;
    private static final double LUNGE_LIFT = 0.15;
    private static final int RACE_ARMED = 1;
    private static final double RACE_GAP = 2.5;
    private static final double RACE_REACH = 5.0;
    private static final double RACE_ARC_COS = Math.cos(Math.toRadians(70.0));
    private static final double RACE_BONUS_DAMAGE = 4.0;
    private static final int RACE_STRIKE_TICKS = STRIKE_DELAY_TICKS;
    private static final double RACE_THROW = 1.6;
    private static final double RACE_LIFT = 0.45;

    public OdachiItem(Properties properties) {
        super(new WeaponStats(9.0, -3.1, 0.0, 2.0), properties);
    }

    // Whether Sword's Race can be cast: not already mid-slash.
    public boolean raceReady(ItemStack stack) {
        return !stack.has(ArmoryComponents.ABILITY_TICK);
    }

    // Sword's Race: places the wielder at slashing range from the enemy in front (or lunges them forward), then
    // swings a heavy horizontal slash for them that lands after the upswing and hurls everything in its arc back.
    public void readyRace(ServerPlayer player, ItemStack stack) {
        ServerLevel level = player.level();
        if (!Engage.snap(player, RACE_GAP)) {
            Vec3 look = player.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0.0, look.z).normalize().scale(LUNGE_SPEED);
            player.push(flat.x, LUNGE_LIFT, flat.z);
            player.needsSync = true;
        }
        // Steps up one-block gaps through the slash, as the barrage does.
        info.mudbourn.mmsweapons.focus.DashStep.boost(player, RACE_STRIKE_TICKS + 12);
        stack.set(ArmoryComponents.ABILITY_TICK, RACE_ARMED);
        stack.set(ArmoryComponents.ANIMATION_START, level.getGameTime());
        Packets.AttackAnimation animation = new Packets.AttackAnimation(player.getId(), AnimatedHand.TWO_HANDED,
            SWEEP_ANIMATION, SWEEP_LENGTH, SWEEP_UPSWING, (float) RACE_REACH, RACE_STRIKE_TICKS, Packets.SwingParticles.EMPTY);
        for (ServerPlayer viewer : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(viewer, animation);
        }
        ServerPlayNetworking.send(player, new SwordsRacePayload());
        level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    // Lands the slash on everything in front of the wielder within reach, hurling each victim back and up.
    private static void strikeRace(ServerPlayer player) {
        ServerLevel level = player.level();
        float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) + RACE_BONUS_DAMAGE);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        AABB box = player.getBoundingBox().inflate(RACE_REACH, 1.5, RACE_REACH);
        Focus.spend(() -> {
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive() && e.isPickable())) {
                Vec3 to = target.getBoundingBox().getCenter().subtract(eye);
                if (to.length() > RACE_REACH + target.getBbWidth() / 2.0 || to.normalize().dot(look) < RACE_ARC_COS || !player.hasLineOfSight(target)) {
                    continue;
                }
                target.damageCooldownTime = 0;
                target.hurtServer(level, player.damageSources().playerAttack(player), damage);
                Vec3 away = target.position().subtract(player.position());
                Vec3 flat = new Vec3(away.x, 0.0, away.z);
                flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(look.x, 0.0, look.z) : flat;
                Vec3 push = flat.normalize().scale(RACE_THROW);
                target.push(push.x, RACE_LIFT, push.z);
                target.needsSync = true;
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.5), target.getZ(), 2, 0.3, 0.2, 0.3, 0.0);
            }
        });
        level.playSound(null, player.blockPosition(), ArmorySounds.DRAGON_SLAYER_SLASH, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    // Lands Sword's Race once its upswing is done, then clears it.
    @Override
    protected void tickAbility(ItemStack stack, ServerLevel level, Entity holder) {
        Integer state = stack.get(ArmoryComponents.ABILITY_TICK);
        Long start = stack.get(ArmoryComponents.ANIMATION_START);
        if (state == null || start == null) {
            return;
        }
        long age = level.getGameTime() - start;
        if (age >= RACE_STRIKE_TICKS) {
            stack.remove(ArmoryComponents.ABILITY_TICK);
            stack.remove(ArmoryComponents.ANIMATION_START);
            if (state == RACE_ARMED && holder instanceof ServerPlayer player && player.getMainHandItem() == stack) {
                strikeRace(player);
            }
        }
    }

    @Override
    protected WeaponStats bonus(ItemStack stack) {
        return WeaponStats.NONE;
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

    // Places the wielder at stabbing distance from a close enemy, or else throws them forward along their facing, as the pierce is released.
    @Override
    public void release(ServerPlayer player, ItemStack stack) {
        boolean placed = Engage.snap(player, PIERCE_GAP);
        super.release(player, stack);
        if (placed) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z).normalize().scale(LUNGE_SPEED);
        player.push(flat.x, LUNGE_LIFT, flat.z);
        info.mudbourn.mmsweapons.focus.DashStep.boost(player, 12);
        player.needsSync = true;
    }
}
