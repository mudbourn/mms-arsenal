package info.mudbourn.mmsarsenal.gun;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;

// A gun whose stats come from its data file, holding its loaded rounds on the stack.
public class GunItem extends Item {

    private static BooleanSupplier shiftDown = () -> false;

    private final String tooltipKey;

    public GunItem(Properties properties, String tooltipKey) {
        super(properties.stacksTo(1));
        this.tooltipKey = tooltipKey;
    }

    // Lets the client tell the tooltip whether shift is held.
    public static void setShiftDown(BooleanSupplier supplier) {
        shiftDown = supplier;
    }

    public Gun getGun(boolean client) {
        return GunManager.get(this, client);
    }

    public static Gun gun(ItemStack stack, boolean client) {
        return stack.getItem() instanceof GunItem gunItem ? gunItem.getGun(client) : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        Gun gun = this.getGun(true);
        if (!shiftDown.getAsBoolean()) {
            tooltip.accept(Component.translatable("info.mms_arsenal.shift_tooltip").withStyle(ChatFormatting.WHITE));
        } else {
            tooltip.accept(Component.translatable("info.mms_arsenal.fire_mode")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.translatable("fire_mode.mms_arsenal." + gun.general().fireMode().getName()).withStyle(ChatFormatting.WHITE)));
            Item ammo = BuiltInRegistries.ITEM.getValue(gun.reloads().reloadType() == ReloadType.SINGLE_ITEM
                ? gun.reloads().reloadItem()
                : gun.projectile().item());
            tooltip.accept(Component.translatable(
                    "info.mms_arsenal.ammo_type",
                    Component.translatable(ammo.getDescriptionId()).withStyle(ChatFormatting.WHITE))
                .withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable(
                    "info.mms_arsenal.damage",
                    ChatFormatting.WHITE + ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(gun.projectile().damage()))
                .withStyle(ChatFormatting.GRAY));
            String advantage = gun.projectile().advantage().getPath();
            if (!advantage.equals("none")) {
                tooltip.accept(Component.translatable("info.mms_arsenal.advantage")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.translatable("advantage.mms_arsenal." + advantage).withStyle(ChatFormatting.GOLD)));
            }
        }
        if (GunComponents.ignoresAmmo(stack)) {
            tooltip.accept(Component.translatable("info.mms_arsenal.ignore_ammo").withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.accept(Component.translatable(
                    "info.mms_arsenal.ammo",
                    ChatFormatting.WHITE.toString() + GunComponents.ammo(stack) + "/" + gun.reloads().maxAmmo())
                .withStyle(ChatFormatting.GRAY));
        }
        if (this.tooltipKey != null) {
            tooltip.accept(Component.translatable(this.tooltipKey).withStyle(ChatFormatting.GRAY));
        }
        if (gun.general().fireTimer() != 0) {
            tooltip.accept(Component.literal(""));
            tooltip.accept(Component.translatable("info.mms_arsenal.hold_fire").withStyle(ChatFormatting.WHITE));
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.isDamaged();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F - stack.getDamageValue() * 13.0F / stack.getMaxDamage());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if (stack.getDamageValue() >= stack.getMaxDamage() / 1.5) {
            return ChatFormatting.RED.getColor();
        }
        float maxDamage = stack.getMaxDamage();
        float f = Math.max(0.0F, (maxDamage - stack.getDamageValue()) / maxDamage);
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }
}
