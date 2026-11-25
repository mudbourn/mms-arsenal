package info.mudbourn.mmsarsenal.relic;

import static info.mudbourn.mmsarsenal.relic.RelicStat.armor;
import static info.mudbourn.mmsarsenal.relic.RelicStat.attackDamage;
import static info.mudbourn.mmsarsenal.relic.RelicStat.attackSpeed;
import static info.mudbourn.mmsarsenal.relic.RelicStat.health;
import static info.mudbourn.mmsarsenal.relic.RelicStat.knockbackResistance;
import static info.mudbourn.mmsarsenal.relic.RelicStat.luck;
import static info.mudbourn.mmsarsenal.relic.RelicStat.rangedDamage;
import static info.mudbourn.mmsarsenal.relic.RelicStat.speed;
import static info.mudbourn.mmsarsenal.relic.RelicStat.spellPower;
import static info.mudbourn.mmsarsenal.relic.RelicStat.swim;
import static info.mudbourn.mmsarsenal.relic.RelicStat.toughness;

import info.mudbourn.mmsarsenal.MmsArsenal;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.spell_engine.api.spell.SpellDataComponents;
import net.spell_engine.api.spell.container.SpellContainers;

import java.util.ArrayList;
import java.util.List;

// The loot-only relic trinkets.
public final class RelicItems {

    private record Row(String id, int tier, List<RelicStat> stats) {
    }

    private static final List<Row> ROWS = List.of(
        row("white_feather", 1, speed(0.05)),
        row("rose_quartz", 3, health(2), armor(1)),
        row("lucky_coin", 3, luck(3), attackSpeed(0.08)),
        row("crimson_orb", 2, attackSpeed(0.08)),
        row("amber_signet", 1, armor(1)),
        row("garnet_necklace", 4, armor(1), knockbackResistance(0.1)),
        row("sea_pearl", 2, swim(0.25)),
        row("fire_opal", 2, toughness(0.5)),
        row("midnight_pendant", 2, attackDamage(0.06)),
        row("blackiron_ring", 1, attackDamage(0.06)),
        row("sunstone", 2, health(2)),
        row("sealed_orders", 1, attackSpeed(0.08)),
        row("fang_necklace", 1, attackDamage(0.06)),
        row("amethyst_gem", 2, spellPower(0.06)),
        row("unlit_candle", 1, luck(1)),
        row("gold_band", 3, health(2), luck(1)),
        row("bleached_skull", 2, attackDamage(0.06)),
        row("blood_skull", 2, health(2)),
        row("topaz", 1, luck(1)),
        row("stone_orb", 1, armor(1)),
        row("pearl_choker", 1, health(2)),
        row("cinder_ring", 1, toughness(0.5)),
        row("heartstone_pendant", 2, health(2)),
        row("bronze_key", 2, rangedDamage(0.06)),
        row("gate_key", 4, knockbackResistance(0.1), armor(1)),
        row("crystal_chalice", 2, health(2)),
        row("wrapped_gift", 1, speed(0.05)),
        row("iron_ring", 1, health(2)),
        row("yellow_diamond", 2, attackDamage(0.06)),
        row("lit_candle", 2, armor(1)),
        row("sapphire_pendant", 2, rangedDamage(0.06)),
        row("laurel_leaf", 2, health(2)),
        row("jade_cord", 2, health(2)),
        row("blessed_pendant", 2, health(2)),
        row("hunters_pendant", 1, rangedDamage(0.06)),
        row("tempered_iron_bar", 4, armor(1), toughness(0.5)),
        row("gilded_bar", 1, luck(2)),
        row("pink_diamond", 1, speed(0.05)),
        row("silver_band", 2, armor(1)),
        row("big_bone", 1, knockbackResistance(0.1)),
        row("copper_coin", 1, luck(1)),
        row("glow_orb", 1, luck(1)),
        row("rune_ring", 1, health(2)),
        row("bloodstone", 2, attackDamage(0.06)),
        row("worn_emerald_pendant", 3, armor(1), knockbackResistance(0.1)),
        row("rosary", 2, health(2)),
        row("desert_pendant", 1, health(2)),
        row("plague_vial", 1, armor(1)),
        row("jade", 2, speed(0.05)),
        row("storm_diamond", 1, attackSpeed(0.08)),
        row("silver_coin", 2, luck(2)),
        row("holy_water", 2, armor(1)),
        row("onyx", 2, attackSpeed(0.08)),
        row("deepwater_pendant", 1, swim(0.25)),
        row("scarlet_blossom", 1, attackDamage(0.06)),
        row("siege_brick", 1, toughness(0.5)),
        row("moonstone", 2, spellPower(0.06)),
        row("silver_cord_pendant", 2, speed(0.05)),
        row("ember_stone", 1, health(2)),
        row("sunset_diamond", 2, speed(0.05)),
        row("marsh_blossom", 1, health(2)),
        row("skeleton_key", 2, speed(0.05)),
        row("twin_rings", 1, attackSpeed(0.08)),
        row("oak_pendant", 3, armor(1), health(2)),
        row("star_chart", 1, spellPower(0.1)),
        row("white_diamond", 2, attackSpeed(0.08)),
        row("wind_gem", 1, speed(0.05)),
        row("sapphire", 2, speed(0.05))
    );

    private static final List<Item> ITEMS = new ArrayList<>();

    private RelicItems() {
    }

    public static void register() {
        ROWS.forEach(RelicItems::register);

        Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            MmsArsenal.id("relics"),
            FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.mms_arsenal.relics"))
                .icon(() -> new ItemStack(ITEMS.getFirst()))
                .displayItems((parameters, output) -> ITEMS.forEach(output::accept))
                .build()
        );
    }

    private static Row row(String id, int tier, RelicStat... stats) {
        return new Row(id, tier, List.of(stats));
    }

    private static void register(Row row) {
        Identifier id = MmsArsenal.id(row.id());
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        Item.Properties properties = new Item.Properties()
            .setId(key)
            .stacksTo(1)
            .rarity(rarity(row.tier()))
            .component(SpellDataComponents.SPELL_CONTAINER, SpellContainers.forRelic(id));

        Item item = Registry.register(BuiltInRegistries.ITEM, key, new RelicItem(properties, row.stats()));
        ITEMS.add(item);
    }

    private static Rarity rarity(int tier) {
        return switch (tier) {
            case 1 -> Rarity.COMMON;
            case 2 -> Rarity.UNCOMMON;
            case 3 -> Rarity.RARE;
            default -> Rarity.EPIC;
        };
    }
}
