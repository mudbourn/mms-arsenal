package info.mudbourn.mmsarsenal.gun;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// Counts the ticks since a gun was drawn; a gun still being drawn cannot fire, aim, melee or inspect.
public final class DrawTracker {

    private static final Map<Player, DrawTracker> TRACKERS = new WeakHashMap<>();

    private int slot = -1;
    private Item item;
    private int ticks;

    public static DrawTracker get(Player player) {
        return TRACKERS.computeIfAbsent(player, key -> new DrawTracker());
    }

    // Restarts the draw when the selected slot or held gun changes, then counts toward the gun's draw time.
    public void tick(Player player, boolean client) {
        ItemStack held = player.getMainHandItem();
        int selected = player.getInventory().getSelectedSlot();
        if (!(held.getItem() instanceof GunItem gunItem)) {
            this.slot = -1;
            this.item = null;
            this.ticks = 0;
            return;
        }
        if (selected != this.slot || held.getItem() != this.item) {
            this.slot = selected;
            this.item = held.getItem();
            this.ticks = 0;
        }
        if (this.ticks < gunItem.getGun(client).general().drawTimer()) {
            this.ticks++;
        }
    }

    public boolean isDrawing(Player player, boolean client) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GunItem gunItem) || held.getItem() != this.item) {
            return false;
        }
        return this.ticks < gunItem.getGun(client).general().drawTimer();
    }

    public int ticks() {
        return this.ticks;
    }
}
