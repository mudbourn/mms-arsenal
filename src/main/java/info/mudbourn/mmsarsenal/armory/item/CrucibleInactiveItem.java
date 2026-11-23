package info.mudbourn.mmsarsenal.armory.item;

import info.mudbourn.mmsarsenal.armory.ArmoryClock;
import info.mudbourn.mmsarsenal.armory.ArmoryItems;
import info.mudbourn.mmsarsenal.armory.ArmorySounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// The unlit Crucible, refilling its energy while it rests and relit once enough is stored.
public class CrucibleInactiveItem extends Item {

    private static final int RELIGHT_ENERGY = 3;

    public CrucibleInactiveItem(Properties properties) {
        super(properties);
    }

    // Whether enough energy has refilled to relight the blade.
    public static boolean skillReady(Player player, ItemStack stack) {
        return CrucibleItem.energy(stack, player.level().getGameTime()) >= RELIGHT_ENERGY;
    }

    // Relights the blade.
    public static void relight(ServerPlayer player, ItemStack stack) {
        ItemStack lit = ArmoryWeaponItem.transmute(stack, ArmoryItems.CRUCIBLE);
        player.setItemInHand(InteractionHand.MAIN_HAND, lit);
        // A short wait before right click can douse it again, so one long press does not flick it back.
        player.getCooldowns().addCooldown(lit, 10);
        player.level().playSound(null, player.blockPosition(), ArmorySounds.CRUCIBLE_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // Right click relights the blade once enough energy has refilled; it is not a skill.
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !skillReady(player, stack)) {
            return InteractionResult.FAIL;
        }
        if (player instanceof ServerPlayer server) {
            relight(server, stack);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return CrucibleItem.energy(stack, ArmoryClock.display()) < CrucibleItem.MAX_ENERGY;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(CrucibleItem.energy(stack, ArmoryClock.display()) / (float) CrucibleItem.MAX_ENERGY * 13.0F);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return ARGB.colorFromFloat(1.0F, 0.5F, 0.5F, 0.5F);
    }
}
