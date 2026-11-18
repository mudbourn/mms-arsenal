package info.mudbourn.mmsarsenal.relic;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.callback.TrinketCallback;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.BiConsumer;

// A loot-only trinket whose stats apply only while it sits in a trinket slot.
public final class RelicItem extends Item implements TrinketCallback {
    private final List<RelicStat> stats;

    public RelicItem(Properties properties, List<RelicStat> stats) {
        super(properties);
        this.stats = stats;
    }

    @Override
    public void forEachTrinketModifier(
        ItemStack stack,
        TrinketSlotAccess slot,
        LivingEntity entity,
        Identifier slotId,
        BiConsumer<Holder<Attribute>, AttributeModifier> output
    ) {
        TrinketCallback.super.forEachTrinketModifier(stack, slot, entity, slotId, output);

        Identifier id = slotId.withSuffix("/" + BuiltInRegistries.ITEM.getKey(this).getPath());
        for (RelicStat stat : stats) {
            BuiltInRegistries.ATTRIBUTE.get(stat.attribute())
                .ifPresent(attribute -> output.accept(attribute, stat.modifier(id)));
        }
    }

    @Override
    public boolean canEquipFromUse(ItemStack stack, LivingEntity entity) {
        return true;
    }

    @Override
    public boolean canUnequip(ItemStack stack, TrinketSlotAccess slot, LivingEntity entity) {
        boolean locked = entity instanceof Player player
            && !player.isCreative()
            && player.getCooldowns().isOnCooldown(stack);

        return TrinketCallback.super.canUnequip(stack, slot, entity) && !locked;
    }
}
