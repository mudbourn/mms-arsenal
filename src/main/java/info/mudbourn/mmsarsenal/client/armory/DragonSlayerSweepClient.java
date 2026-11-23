package info.mudbourn.mmsarsenal.client.armory;

import info.mudbourn.mmsarsenal.armory.item.DragonSlayerItem;
import info.mudbourn.mmsarsenal.net.DragonSlayerReleasePayload;
import info.mudbourn.mmsarsenal.net.DragonSlayerSweepPayload;
import info.mudbourn.mmsweapons.focus.FocusSkill;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.client.animation.PlayerAttackAnimatable;
import net.bettercombat.client.collision.TargetFinder;
import net.bettercombat.logic.AnimatedHand;
import net.bettercombat.logic.WeaponRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

// Plays the Dragon Slayer focus sweep on the local player and, as the blade comes through, reports what its arc hit on this client.
public final class DragonSlayerSweepClient {

    private static WeaponAttributes.Attack pendingAttack;
    private static int ticksUntilStrike;

    private DragonSlayerSweepClient() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(DragonSlayerReleasePayload.TYPE, (payload, context) -> {
            Player player = context.player();
            if (!(player.getMainHandItem().getItem() instanceof DragonSlayerItem slayer)) {
                return;
            }
            if (player instanceof PlayerAttackAnimatable animatable) {
                animatable.playAttackAnimation(
                    slayer.chargedAnimation(),
                    AnimatedHand.TWO_HANDED,
                    DragonSlayerItem.SWEEP_LENGTH,
                    DragonSlayerItem.SWEEP_UPSWING
                );
            }
            FocusSkill.execute((int) DragonSlayerItem.SWEEP_LENGTH);
            WeaponAttributes attributes = WeaponRegistry.getAttributes(player.getMainHandItem());
            if (attributes != null && attributes.attacks().length > 0) {
                pendingAttack = chargedAttack(attributes, slayer.chargedPierce());
                ticksUntilStrike = DragonSlayerItem.STRIKE_DELAY_TICKS;
            }
        });
        // Sword's Race: the odachi's placed slash, swung for the local player; the server lands its hits.
        ClientPlayNetworking.registerGlobalReceiver(info.mudbourn.mmsarsenal.net.SwordsRacePayload.TYPE, (payload, context) -> {
            if (context.player() instanceof PlayerAttackAnimatable animatable) {
                animatable.playAttackAnimation(DragonSlayerItem.SWEEP_ANIMATION, AnimatedHand.TWO_HANDED,
                    DragonSlayerItem.SWEEP_LENGTH, DragonSlayerItem.SWEEP_UPSWING);
            }
            FocusSkill.execute((int) DragonSlayerItem.SWEEP_LENGTH);
        });
        ClientTickEvents.END_CLIENT_TICK.register(DragonSlayerSweepClient::tick);
    }

    // The weapon's first forward stab for a piercing focus attack, otherwise its first swing.
    private static WeaponAttributes.Attack chargedAttack(WeaponAttributes attributes, boolean pierce) {
        for (WeaponAttributes.Attack attack : attributes.attacks()) {
            if (pierce && attack.hitbox() == WeaponAttributes.HitBoxShape.FORWARD_BOX) {
                return attack;
            }
        }
        return attributes.attacks()[0];
    }

    private static void tick(Minecraft client) {
        if (pendingAttack == null || --ticksUntilStrike > 0) {
            return;
        }
        WeaponAttributes.Attack attack = pendingAttack;
        pendingAttack = null;
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        if (!(player.getMainHandItem().getItem() instanceof DragonSlayerItem slayer)) {
            return;
        }
        double range = slayer.chargedRange(player);
        List<Integer> targets = TargetFinder.findAttackTargets(player, client.crosshairPickEntity, attack, range)
            .stream()
            .map(Entity::getId)
            .toList();
        ClientPlayNetworking.send(new DragonSlayerSweepPayload(targets));
    }
}
