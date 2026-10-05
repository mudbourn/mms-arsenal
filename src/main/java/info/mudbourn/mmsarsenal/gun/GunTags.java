package info.mudbourn.mmsarsenal.gun;

import info.mudbourn.mmsarsenal.MmsArsenal;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

// Tags that decide bullet damage multipliers, which blocks bullets break, and which impact sounds play.
public final class GunTags {

    public static final TagKey<EntityType<?>> HEAVY = entity("heavy");
    public static final TagKey<EntityType<?>> VERY_HEAVY = entity("very_heavy");
    public static final TagKey<EntityType<?>> UNDEAD = entity("undead");
    public static final TagKey<EntityType<?>> GHOST = entity("ghost");
    public static final TagKey<EntityType<?>> FIRE = entity("fire");

    public static final TagKey<Block> FRAGILE = block("fragile");
    public static final TagKey<Block> METAL = block("metal");
    public static final TagKey<Block> SQUISHY = block("squishy");
    public static final TagKey<Block> STONE = block("stone");
    public static final TagKey<Block> WOOD = block("wood");

    private GunTags() {
    }

    private static TagKey<EntityType<?>> entity(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, MmsArsenal.id(path));
    }

    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, MmsArsenal.id(path));
    }
}
