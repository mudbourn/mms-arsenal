package info.mudbourn.mmsarsenal.armory;

import info.mudbourn.mmsarsenal.MmsArsenal;
import info.mudbourn.mmsarsenal.armory.item.BloodVesselItem;
import info.mudbourn.mmsarsenal.armory.item.BloodletterItem;
import info.mudbourn.mmsarsenal.armory.item.CrucibleInactiveItem;
import info.mudbourn.mmsarsenal.armory.item.CrucibleItem;
import info.mudbourn.mmsarsenal.armory.item.DragonSlayerItem;
import info.mudbourn.mmsarsenal.armory.item.EdgeOfExistenceItem;
import info.mudbourn.mmsarsenal.armory.item.MurasamaItem;
import info.mudbourn.mmsarsenal.armory.item.OdachiItem;
import info.mudbourn.mmsarsenal.armory.item.MurasamaSheathedItem;
import info.mudbourn.mmsarsenal.armory.item.OriginiumCatalystItem;
import info.mudbourn.mmsarsenal.armory.item.OriginiumKatanaItem;
import info.mudbourn.mmsarsenal.armory.item.PunisherItem;
import info.mudbourn.mmscombat.dummy.TrainingDummy;
import info.mudbourn.mmsweapons.spellengine.SpellEngineBarrage;
import info.mudbourn.mmsarsenal.gun.GunItems;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Weapon;
import net.spell_engine.api.spell.SpellDataComponents;

import java.util.List;
import java.util.function.Function;

// The Armory of Destiny melee weapons and their companion items.
public final class ArmoryItems {

    public static final Item DRAGON_SLAYER = register(
        "dragon_slayer",
        DragonSlayerItem::new,
        heavyWeapon(2048).repairable(Items.NETHERITE_INGOT)
            .component(SpellDataComponents.SPELL_CONTAINER, ArmorySkills.container(ArmorySkills.DRAGON_SLAYER_SWEEP))
    );
    public static final Item BLOODLETTER = register(
        "bloodletter",
        BloodletterItem::new,
        weapon(1024).repairable(Items.NETHERITE_INGOT)
            .component(SpellDataComponents.SPELL_CONTAINER, ArmorySkills.container(ArmorySkills.BLOODLETTER_INFUSE, SpellEngineBarrage.SPELL))
    );
    public static final Item CRUCIBLE = register(
        "crucible",
        CrucibleItem::new,
        epic()
            .enchantable(15)
            .component(DataComponents.WEAPON, new Weapon(1, 5.0F))
            .component(ArmoryComponents.USAGES, CrucibleItem.MAX_ENERGY)
            .component(SpellDataComponents.SPELL_CONTAINER, ArmorySkills.container(ArmorySkills.CRUCIBLE_DOUSE, SpellEngineBarrage.SPELL))
    );
    public static final Item CRUCIBLE_INACTIVE = register(
        "crucible_inactive",
        CrucibleInactiveItem::new,
        epic().overrideDescription("item.mms_arsenal.crucible")
            .component(SpellDataComponents.SPELL_CONTAINER, ArmorySkills.container(ArmorySkills.CRUCIBLE_RELIGHT))
    );
    public static final Item EDGE_OF_EXISTENCE = register(
        "edge_of_existence",
        EdgeOfExistenceItem::new,
        weapon(1150).repairable(Items.NETHERITE_INGOT)
            .component(SpellDataComponents.SPELL_CONTAINER, ArmorySkills.container(ArmorySkills.NON_EXISTENCE, SpellEngineBarrage.SPELL))
    );
    public static final Item MURASAMA = register(
        "murasama",
        MurasamaItem::new,
        weapon(1300).repairable(Items.ECHO_SHARD)
            .component(SpellDataComponents.SPELL_CONTAINER, ArmorySkills.container(ArmorySkills.MURASAMA_SHEATHE, SpellEngineBarrage.SPELL))
    );
    public static final Item MURASAMA_SHEATHED = register(
        "murasama_sheathed",
        MurasamaSheathedItem::new,
        epic().overrideDescription("item.mms_arsenal.murasama")
            .component(SpellDataComponents.SPELL_CONTAINER, ArmorySkills.container(ArmorySkills.MURASAMA_QUICKDRAW))
    );
    public static final Item GUN_SHEATH = register(
        "gun_sheath",
        Item::new,
        epic()
    );
    public static final Item ORIGINIUM_KATANA = register(
        "originium_katana",
        OriginiumKatanaItem::new,
        weapon(64)
    );
    public static final Item ORIGINIUM_CATALYST = register(
        "originium_catalyst",
        OriginiumCatalystItem::new,
        new Item.Properties().rarity(Rarity.EPIC).stacksTo(16)
    );
    public static final Item PUNISHER = register(
        "punisher",
        PunisherItem::new,
        heavyWeapon(1800).repairable(Items.DIAMOND)
            .component(SpellDataComponents.SPELL_CONTAINER, ArmorySkills.container(ArmorySkills.PUNISHER_IGNITE, SpellEngineBarrage.SPELL))
    );
    public static final Item ODACHI = register(
        "odachi",
        OdachiItem::new,
        weapon(2031).repairable(Items.NETHERITE_INGOT)
            .component(SpellDataComponents.SPELL_CONTAINER, ArmorySkills.container(ArmorySkills.ODACHI_PIERCE, ArmorySkills.SWORDS_RACE))
    );
    public static final Item BLOOD_VESSEL_EMPTY = register(
        "blood_vessel_empty",
        Item::new,
        new Item.Properties().stacksTo(8)
    );
    public static final Item BLOOD_VESSEL_FULL = register(
        "blood_vessel_full",
        BloodVesselItem::new,
        new Item.Properties().stacksTo(8)
    );

    private static final List<Item> CREATIVE_ORDER = List.of(
        DRAGON_SLAYER,
        CRUCIBLE_INACTIVE,
        BLOODLETTER,
        BLOOD_VESSEL_EMPTY,
        BLOOD_VESSEL_FULL,
        EDGE_OF_EXISTENCE,
        MURASAMA,
        GUN_SHEATH,
        ORIGINIUM_CATALYST,
        ORIGINIUM_KATANA,
        PUNISHER,
        ODACHI
    );

    private ArmoryItems() {
    }

    public static void register() {
        Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(MmsArsenal.MOD_ID, "armory"),
            FabricCreativeModeTab.builder()
                .title(Component.translatable("itemGroup.mms_arsenal.armory"))
                .icon(() -> new ItemStack(DRAGON_SLAYER))
                .displayItems((parameters, output) -> {
                    CREATIVE_ORDER.forEach(output::accept);
                    output.acceptAll(GunItems.creativeStacks());
                    output.accept(TrainingDummy.ITEM);
                })
                .build()
        );
    }

    private static Item.Properties epic() {
        return new Item.Properties().rarity(Rarity.EPIC).stacksTo(1);
    }

    private static Item.Properties weapon(int durability) {
        return epic()
            .durability(durability)
            .enchantable(15)
            .component(DataComponents.WEAPON, new Weapon(1));
    }

    private static Item.Properties heavyWeapon(int durability) {
        return weapon(durability).component(DataComponents.WEAPON, new Weapon(1, 5.0F));
    }

    private static Item register(String path, Function<Item.Properties, Item> factory, Item.Properties properties) {
        Identifier id = Identifier.fromNamespaceAndPath(MmsArsenal.MOD_ID, path);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        Item item = factory.apply(properties.setId(key));

        Registry.register(BuiltInRegistries.ITEM, key, item);
        MmsArsenal.aliasLegacy(BuiltInRegistries.ITEM, path);
        return item;
    }
}
