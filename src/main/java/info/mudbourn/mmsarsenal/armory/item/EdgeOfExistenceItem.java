package info.mudbourn.mmsarsenal.armory.item;

import info.mudbourn.mmsarsenal.armory.ArmoryComponents;
import info.mudbourn.mmsarsenal.armory.ArmoryEffects;
import info.mudbourn.mmsarsenal.armory.ArmorySounds;
import info.mudbourn.mmsarsenal.armory.WeaponStats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

// Katana that grants Non-Existence on use and glows while its holder has it.
public class EdgeOfExistenceItem extends ArmoryWeaponItem {

    private static final int EFFECT_TICKS = 300;

    public EdgeOfExistenceItem(Properties properties) {
        super(new WeaponStats(6.0, -3.4, -1.0, 1.0), properties);
    }

    // Grants Non-Existence to the wielder.
    public static void vanish(ServerPlayer player, ItemStack stack) {
        ArmoryEffects.grantNonexistence(player, EFFECT_TICKS);
        player.level().playSound(null, player.blockPosition(), ArmorySounds.EDGE_OF_EXISTENCE_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    protected void tickAbility(ItemStack stack, ServerLevel level, Entity holder) {
        if (holder instanceof LivingEntity living) {
            int wanted = living.hasEffect(ArmoryEffects.NONEXISTENCE) ? 1 : 0;
            if (stack.getOrDefault(ArmoryComponents.ABILITY_TICK, 0) != wanted) {
                stack.set(ArmoryComponents.ABILITY_TICK, wanted);
            }
        }
    }
}
