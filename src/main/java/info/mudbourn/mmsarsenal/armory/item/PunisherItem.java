package info.mudbourn.mmsarsenal.armory.item;

import info.mudbourn.mmsarsenal.armory.ArmoryComponents;
import info.mudbourn.mmsarsenal.armory.ArmorySounds;
import info.mudbourn.mmsarsenal.armory.WeaponStats;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

// Hammer whose ignited engines empower its next three strikes.
public class PunisherItem extends ArmoryWeaponItem {

    private static final int STRIKES = 3;
    private static final WeaponStats IGNITED = new WeaponStats(7.0, 0.9, 2.8, 0.0);

    public PunisherItem(Properties properties) {
        super(new WeaponStats(9.5, -3.6, -1.2, 0.0), properties);
    }

    // Whether the engines can be ignited: no empowered strikes left over.
    public boolean skillReady(ItemStack stack) {
        return stack.getOrDefault(ArmoryComponents.STRIKES_REMAINING, 0) <= 0;
    }

    // Ignites the engines, empowering the next three strikes.
    public void ignite(ServerPlayer player, ItemStack stack) {
        stack.set(ArmoryComponents.STRIKES_REMAINING, STRIKES);
        stack.set(ArmoryComponents.ANIMATION_START, player.level().getGameTime());
        player.level().playSound(null, player.blockPosition(), ArmorySounds.PUNISHER_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        int strikes = stack.getOrDefault(ArmoryComponents.STRIKES_REMAINING, 0);
        if (strikes <= 0) {
            return;
        }
        attacker.playSound(ArmorySounds.PUNISHER_ATTACK, 1.0F, 1.0F);
        stack.set(ArmoryComponents.STRIKES_REMAINING, strikes - 1);
        if (strikes == 1) {
            attacker.playSound(ArmorySounds.PUNISHER_DEACTIVATE, 1.0F, 1.0F);
            stack.remove(ArmoryComponents.ANIMATION_START);
        }
    }

    @Override
    protected WeaponStats bonus(ItemStack stack) {
        return stack.getOrDefault(ArmoryComponents.STRIKES_REMAINING, 0) > 0 ? IGNITED : WeaponStats.NONE;
    }
}
