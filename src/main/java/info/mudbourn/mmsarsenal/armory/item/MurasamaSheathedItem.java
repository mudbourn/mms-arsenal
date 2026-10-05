package info.mudbourn.mmsarsenal.armory.item;

import info.mudbourn.mmsarsenal.armory.ArmoryItems;
import info.mudbourn.mmsarsenal.armory.ArmorySounds;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Murasama in its Gun Sheath; using it with a free off hand fires the blade out for a quickdraw.
public class MurasamaSheathedItem extends Item {

    private static final double DASH_FORCE = 2.0;
    private static final Identifier PERISHABLE_OWNER = Identifier.fromNamespaceAndPath("mms_combat", "perishable_owner");

    public MurasamaSheathedItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !player.getOffhandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        ItemStack drawn = ArmoryWeaponItem.transmute(player.getItemInHand(hand), ArmoryItems.MURASAMA);
        MurasamaItem.startQuickdraw(drawn, level);
        ItemStack sheath = new ItemStack(ArmoryItems.GUN_SHEATH);
        copyBinding(drawn, sheath);
        player.setItemInHand(InteractionHand.OFF_HAND, sheath);
        player.setItemInHand(InteractionHand.MAIN_HAND, drawn);
        level.playSound(player, player.blockPosition(), ArmorySounds.MURASAMA_SHOOT, SoundSource.PLAYERS, 0.5F, 1.0F);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 1.0, player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        }

        if (player.isShiftKeyDown()) {
            Vec3 push = player.getLookAngle().scale(DASH_FORCE).add(0.0, 0.2, 0.0);
            player.push(push.x, push.y, push.z);
            player.hurtMarked = true;
        }
        return InteractionResult.SUCCESS;
    }

    // Carries an mms-combat killstreak binding, when present, from the drawn blade onto its sheath.
    private static void copyBinding(ItemStack from, ItemStack to) {
        DataComponentType<?> owner = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(PERISHABLE_OWNER);
        if (owner != null && from.has(owner)) {
            to.copyFrom(owner, from);
            to.copyFrom(DataComponents.LORE, from);
        }
    }
}
