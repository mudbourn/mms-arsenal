package info.mudbourn.mmsarsenal.client.armory;

import info.mudbourn.mmsarsenal.armory.item.DragonSlayerItem;
import info.mudbourn.mmsarsenal.net.DragonSlayerSweepPayload;
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

import java.util.List;

// Plays the Dragon Slayer focus sweep on the local player and, as the blade comes through, reports what its arc hit on this client.
public final class DragonSlayerSweepClient {

    private static WeaponAttributes.Attack pendingAttack;
    private static int ticksUntilStrike;

    private DragonSlayerSweepClient() {
    }

    public static void register() {
        DragonSlayerItem.setLocalSweep(player -> {
            if (player instanceof PlayerAttackAnimatable animatable) {
                animatable.playAttackAnimation(
                    DragonSlayerItem.SWEEP_ANIMATION,
                    AnimatedHand.TWO_HANDED,
                    DragonSlayerItem.SWEEP_LENGTH,
                    DragonSlayerItem.SWEEP_UPSWING
                );
            }
            WeaponAttributes attributes = WeaponRegistry.getAttributes(player.getMainHandItem());
            if (attributes != null && attributes.attacks().length > 0) {
                pendingAttack = attributes.attacks()[0];
                ticksUntilStrike = DragonSlayerItem.STRIKE_DELAY_TICKS;
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(DragonSlayerSweepClient::tick);
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
        double range = player.entityInteractionRange() * DragonSlayerItem.FOCUS_MULTIPLIER;
        List<Integer> targets = TargetFinder.findAttackTargets(player, client.crosshairPickEntity, attack, range)
            .stream()
            .map(Entity::getId)
            .toList();
        ClientPlayNetworking.send(new DragonSlayerSweepPayload(targets));
    }
}
