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

// The loot-only relic trinkets, one per fashion armour kit.
public final class RelicItems {

    private record Row(String id, int tier, List<RelicStat> stats) {
    }

    private static final List<Row> ROWS = List.of(
        row("axolotl_knights_gill_charm", 3, swim(0.05), health(2)),
        row("brets_lucky_coin", 1, luck(1)),
        row("corsairs_compass", 2, speed(0.05)),
        row("scaphanders_brass_valve", 1, armor(1)),
        row("executioners_black_hood", 2, attackDamage(0.06)),
        row("furnace_masters_ember", 1, toughness(0.5)),
        row("gamekeepers_whistle", 2, rangedDamage(0.06)),
        row("gate_sentrys_key", 4, knockbackResistance(0.1), armor(1)),
        row("captains_signet", 1, armor(1)),
        row("highlanders_clan_brooch", 2, health(2)),
        row("masquerade_mask", 1, speed(0.05)),
        row("mercenarys_purse", 2, attackSpeed(0.08)),
        row("mountainmans_horn", 1, knockbackResistance(0.1)),
        row("netherborn_pirates_doubloon", 2, luck(1)),
        row("norsian_knights_rune_ring", 1, health(2)),
        row("penitents_rosary", 2, health(2)),
        row("plague_doctors_censer", 1, armor(1)),
        row("ratcatchers_bell", 2, attackDamage(0.06)),
        row("siege_engineers_plumb_bob", 1, toughness(0.5)),
        row("snow_tigers_fang", 2, speed(0.05)),
        row("swamplanders_reed_charm", 1, health(2)),
        row("wald_knights_oak_medallion", 3, armor(1), health(2)),
        row("winged_cavalry_plume", 2, speed(0.05)),
        row("astolfos_hippogriff_feather", 1, speed(0.05)),
        row("bridgets_yo_yo_charm", 2, attackSpeed(0.08)),
        row("felixs_cat_bell", 1, luck(1)),
        row("gabriels_halo", 2, health(2)),
        row("giselles_ribbon", 1, speed(0.05)),
        row("griffiths_hawk_pendant", 2, attackDamage(0.06)),
        row("nikos_lightbulb", 1, luck(1)),
        row("mikkelas_sewing_thimble", 2, armor(1)),
        row("anubiss_scale_weight", 1, attackDamage(0.06)),
        row("centurions_phalera", 4, armor(1), knockbackResistance(0.1)),
        row("exalted_aurum_crown_ring", 3, luck(1), health(2)),
        row("holy_reliquary", 2, health(2)),
        row("iron_plate_rivet", 4, armor(1), toughness(0.5)),
        row("do_maru_lacing_cord", 1, attackSpeed(0.08)),
        row("o_yoroi_kabuto_crest", 2, attackDamage(0.06)),
        row("pharaohs_ankh", 1, health(2)),
        row("quetzalcoatls_plume", 2, speed(0.05)),
        row("raijins_drum", 1, attackSpeed(0.08)),
        row("eclipse_legion_standard", 2, armor(1)),
        row("dragonslayers_scale", 1, attackDamage(0.06)),
        row("heros_laurel", 2, health(2)),
        row("golden_horn_idol", 1, luck(1)),
        row("thiefs_lockpick", 2, speed(0.05)),
        row("wanderers_star_chart", 1, spellPower(0.1)),
        row("white_knight_chess_piece", 2, attackSpeed(0.08)),
        row("dark_lords_signet", 1, attackDamage(0.06)),
        row("sunset_wing_feather", 2, speed(0.05)),
        row("fog_guard_lantern", 1, armor(1)),
        row("dark_cover_veil_pin", 2, attackDamage(0.06)),
        row("spark_of_dawn_ember", 1, health(2)),
        row("golden_execution_axe_charm", 2, attackDamage(0.06)),
        row("forgotten_trace_locket", 1, health(2)),
        row("redeemers_censer", 2, armor(1)),
        row("twinned_rings", 1, attackSpeed(0.08)),
        row("gilded_hunt_trophy", 2, rangedDamage(0.06)),
        row("hunters_cameo", 1, attackDamage(0.06)),
        row("crucible_knights_seal", 2, toughness(0.5)),
        row("evening_ghost_lantern", 1, speed(0.05)),
        row("ronins_tsuba", 2, attackSpeed(0.08)),
        row("scarlet_rot_blossom", 1, attackDamage(0.06)),
        row("old_knights_gorget", 3, armor(1), knockbackResistance(0.1)),
        row("silver_knights_moon_medal", 2, luck(1)),
        row("gladiators_chain", 1, health(2)),
        row("feasters_tooth", 2, health(2)),
        row("wind_worshippers_chime", 1, speed(0.05)),
        row("grave_sentinels_candle", 2, armor(1)),
        row("lion_knights_spearhead", 1, attackSpeed(0.08)),
        row("dawncrest_sun_medal", 2, health(2))
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
