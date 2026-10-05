package info.mudbourn.mmsarsenal.gun;

import info.mudbourn.mmsarsenal.MmsArsenal;
import info.mudbourn.mmsarsenal.gun.throwable.GrenadeItem;
import info.mudbourn.mmsarsenal.gun.throwable.MolotovCocktailItem;
import info.mudbourn.mmsarsenal.gun.throwable.SmokeGrenadeItem;
import info.mudbourn.mmsarsenal.gun.throwable.StunGrenadeItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

// The guns, the ammo they fire and the throwables.
public final class GunItems {

    private static final List<Item> CREATIVE_ORDER = new ArrayList<>();

    public static final GunItem REVOLVER = gun("revolver", 128, Rarity.COMMON, null);
    public static final GunItem ASSAULT_RIFLE = gun("assault_rifle", 384, Rarity.RARE, null);
    public static final GunItem BOLT_ACTION_RIFLE = gun("bolt_action_rifle", 384, Rarity.EPIC, null);
    public static final GunItem BLOSSOM_RIFLE = gun("blossom_rifle", 512, Rarity.EPIC, null);
    public static final GunItem SOULHUNTER_MK2 = gun("soulhunter_mk2", 400, Rarity.EPIC, null);
    public static final GunItem HYPERSONIC_CANNON = gun("hypersonic_cannon", 100, Rarity.EPIC, "info.mms_arsenal.echo_shard");
    public static final GunItem ROCKET_LAUNCHER = gun("rocket_launcher", 32, Rarity.EPIC, null);
    public static final GunItem LIGHT_MACHINE_GUN = gun("light_machine_gun", 1024, Rarity.EPIC, null);
    public static final GunItem MINIGUN = gun("minigun", 2048, Rarity.EPIC, null);

    public static final Item PISTOL_AMMO = ammo("pistol_ammo", new Item.Properties());
    public static final Item RIFLE_AMMO = ammo("rifle_ammo", new Item.Properties());
    public static final Item ROCKET = ammo("rocket", new Item.Properties().rarity(Rarity.EPIC).stacksTo(4));
    public static final Item SPECTRE_ROUND = ammo("spectre_round", new Item.Properties());
    public static final Item BLAZE_ROUND = ammo("blaze_round", new Item.Properties());

    public static final Item GRENADE = register("grenade", GrenadeItem::new, new Item.Properties().stacksTo(16));
    public static final Item STUN_GRENADE = register("stun_grenade", StunGrenadeItem::new, new Item.Properties().stacksTo(16));
    public static final Item SMOKE_GRENADE = register("smoke_grenade", SmokeGrenadeItem::new, new Item.Properties().stacksTo(16));
    public static final Item MOLOTOV_COCKTAIL = register("molotov_cocktail", MolotovCocktailItem::new, new Item.Properties().stacksTo(16));

    private GunItems() {
    }

    // Loads this class so its items register before the registries freeze.
    public static void register() {
    }

    // The guns and ammo in creative order, each gun with a full magazine.
    public static List<ItemStack> creativeStacks() {
        return CREATIVE_ORDER.stream().map(GunItems::fullyLoaded).toList();
    }

    // A creative stack of a gun with a full magazine, or the item itself for anything else.
    private static ItemStack fullyLoaded(Item item) {
        ItemStack stack = new ItemStack(item);
        if (item instanceof GunItem gunItem) {
            GunComponents.setAmmo(stack, gunItem.getGun(true).reloads().maxAmmo());
        }
        return stack;
    }

    private static GunItem gun(String path, int durability, Rarity rarity, String tooltipKey) {
        return register(
            path,
            properties -> new GunItem(properties, tooltipKey),
            new Item.Properties().durability(durability).rarity(rarity)
        );
    }

    private static Item ammo(String path, Item.Properties properties) {
        return register(path, Item::new, properties);
    }

    private static <T extends Item> T register(String path, Function<Item.Properties, T> factory, Item.Properties properties) {
        Identifier id = MmsArsenal.id(path);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        T item = factory.apply(properties.setId(key));
        Registry.register(BuiltInRegistries.ITEM, key, item);
        CREATIVE_ORDER.add(item);
        return item;
    }
}
