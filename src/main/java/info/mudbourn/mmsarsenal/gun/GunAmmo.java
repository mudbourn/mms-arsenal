package info.mudbourn.mmsarsenal.gun;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// Finds a player's ammo; creative players have an endless supply.
public final class GunAmmo {

    private GunAmmo() {
    }

    // The first stack of this ammo in the inventory, or an endless stack in creative, or empty.
    public static ItemStack find(Player player, Identifier id) {
        if (player.isCreative()) {
            return endless(id);
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (is(stack, id)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    // Every stack of this ammo in the inventory, or one endless stack in creative.
    public static List<ItemStack> findAll(Player player, Identifier id) {
        List<ItemStack> stacks = new ArrayList<>();
        if (!player.isAlive()) {
            return stacks;
        }
        if (player.isCreative()) {
            stacks.add(endless(id));
            return stacks;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (is(stack, id)) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    public static int total(List<ItemStack> stacks) {
        int total = 0;
        for (ItemStack stack : stacks) {
            total += stack.getCount();
        }
        return total;
    }

    // Whether the player has none of the ammo this gun reloads with.
    public static boolean lacksAmmo(Player player, Gun gun) {
        Identifier id = gun.reloads().reloadType() == ReloadType.SINGLE_ITEM
            ? gun.reloads().reloadItem()
            : gun.projectile().item();
        return find(player, id).isEmpty();
    }

    public static boolean is(ItemStack stack, Identifier id) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(id);
    }

    private static ItemStack endless(Identifier id) {
        Item item = BuiltInRegistries.ITEM.getValue(id);
        return new ItemStack(item, Integer.MAX_VALUE);
    }
}
