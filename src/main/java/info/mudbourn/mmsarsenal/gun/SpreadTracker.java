package info.mudbourn.mmsarsenal.gun;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

// Spread that grows with sustained fire and resets after a pause, per player and gun.
public final class SpreadTracker {

    public static final int SPREAD_THRESHOLD = 300;
    public static final int MAX_COUNT = 10;

    private static final Map<Player, SpreadTracker> TRACKERS = new WeakHashMap<>();

    private final Map<Item, long[]> entries = new HashMap<>();

    public static SpreadTracker get(Player player) {
        return TRACKERS.computeIfAbsent(player, key -> new SpreadTracker());
    }

    // Counts a shot: quick follow-ups widen spread, twice as fast when not aiming.
    public void update(Player player, Item item) {
        long[] entry = this.entries.computeIfAbsent(item, key -> new long[] {-1L, 0L});
        if (entry[0] != -1L) {
            long delta = System.currentTimeMillis() - entry[0];
            if (delta < SPREAD_THRESHOLD) {
                if (entry[1] < MAX_COUNT) {
                    entry[1]++;
                    if (entry[1] < MAX_COUNT && !GunState.isAiming(player)) {
                        entry[1]++;
                    }
                }
            } else {
                entry[1] = 0L;
            }
        }
        entry[0] = System.currentTimeMillis();
    }

    // The spread fraction the next shot would use, for the dynamic crosshair.
    public float nextSpread(Item item, float aim) {
        long[] entry = this.entries.get(item);
        return entry == null ? 0.0F : (entry[1] + 1.0F + aim) / MAX_COUNT;
    }

    public float spread(Item item) {
        long[] entry = this.entries.get(item);
        return entry == null ? 0.0F : (float) entry[1] / MAX_COUNT;
    }
}
