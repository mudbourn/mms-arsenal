package info.mudbourn.mmsarsenal.client.armory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import info.mudbourn.mmsarsenal.MmsArsenal;
import info.mudbourn.mmsarsenal.armory.ArmoryComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;

// The mms_arsenal:elapsed range property: 1-based ticks since the stack's animation start, looped by period, or 0 when idle.
public record ElapsedTicksProperty(int period) implements RangeSelectItemModelProperty {

    public static final MapCodec<ElapsedTicksProperty> MAP_CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            Codec.INT.optionalFieldOf("period", 0).forGetter(ElapsedTicksProperty::period)
        ).apply(instance, ElapsedTicksProperty::new)
    );

    public static void register() {
        RangeSelectItemModelProperties.ID_MAPPER.put(
            Identifier.fromNamespaceAndPath(MmsArsenal.MOD_ID, "elapsed"),
            MAP_CODEC
        );
    }

    @Override
    public float get(ItemStack stack, ClientLevel level, ItemOwner owner, int seed) {
        Long start = stack.get(ArmoryComponents.ANIMATION_START);
        ClientLevel clock = level != null ? level : Minecraft.getInstance().level;
        if (start == null || clock == null) {
            return 0.0F;
        }
        long elapsed = Math.max(0L, clock.getGameTime() - start);
        return (this.period > 0 ? elapsed % this.period : elapsed) + 1.0F;
    }

    @Override
    public MapCodec<ElapsedTicksProperty> type() {
        return MAP_CODEC;
    }
}
