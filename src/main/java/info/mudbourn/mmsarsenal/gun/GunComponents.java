package info.mudbourn.mmsarsenal.gun;

import com.mojang.serialization.Codec;
import info.mudbourn.mmsarsenal.MmsArsenal;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;

// Gun stack state: rounds loaded, and whether the gun fires without using any.
public final class GunComponents {

    public static final DataComponentType<Integer> AMMO_COUNT = register(
        "ammo_count",
        DataComponentType.<Integer>builder()
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .build()
    );
    public static final DataComponentType<Boolean> IGNORE_AMMO = register(
        "ignore_ammo",
        DataComponentType.<Boolean>builder()
            .persistent(Codec.BOOL)
            .networkSynchronized(ByteBufCodecs.BOOL)
            .build()
    );

    private GunComponents() {
    }

    public static void register() {
    }

    public static int ammo(ItemStack stack) {
        return stack.getOrDefault(AMMO_COUNT, 0);
    }

    public static void setAmmo(ItemStack stack, int ammo) {
        stack.set(AMMO_COUNT, ammo);
    }

    public static boolean ignoresAmmo(ItemStack stack) {
        return stack.getOrDefault(IGNORE_AMMO, false);
    }

    public static boolean hasAmmo(ItemStack stack) {
        return ignoresAmmo(stack) || ammo(stack) > 0;
    }

    private static <T> DataComponentType<T> register(String path, DataComponentType<T> type) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, MmsArsenal.id(path), type);
    }
}
