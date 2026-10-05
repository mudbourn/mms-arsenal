package info.mudbourn.mmsarsenal.gun;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

// Server-side fire-rate check per player and gun, in wall-clock milliseconds, rejecting shots sent too early.
public final class ShootTracker {

    private static final Map<Player, ShootTracker> TRACKERS = new WeakHashMap<>();
    private static final int COOLDOWN_THRESHOLD = 0;

    private final Map<Item, long[]> cooldowns = new HashMap<>();

    public static ShootTracker get(Player player) {
        return TRACKERS.computeIfAbsent(player, key -> new ShootTracker());
    }

    public void putCooldown(Item item, int rate) {
        this.cooldowns.put(item, new long[] {Util.getMillis(), rate * 50L});
    }

    public boolean hasCooldown(Item item) {
        long[] entry = this.cooldowns.get(item);
        return entry != null && Util.getMillis() - entry[0] < entry[1] - 50L;
    }

    public long remaining(Item item) {
        long[] entry = this.cooldowns.get(item);
        return entry == null ? 0L : entry[1] - (Util.getMillis() - entry[0]);
    }

    // Whether a shot now arrives before the gun's cooldown is up by more than the allowed slack.
    public boolean tooEarly(Item item) {
        return this.hasCooldown(item) && this.remaining(item) > COOLDOWN_THRESHOLD;
    }
}
