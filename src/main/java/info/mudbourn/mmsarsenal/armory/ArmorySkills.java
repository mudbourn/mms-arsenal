package info.mudbourn.mmsarsenal.armory;

import info.mudbourn.mmsarsenal.MmsArsenal;
import info.mudbourn.mmsarsenal.armory.item.BloodletterItem;
import info.mudbourn.mmsarsenal.armory.item.CrucibleInactiveItem;
import info.mudbourn.mmsarsenal.armory.item.CrucibleItem;
import info.mudbourn.mmsarsenal.armory.item.DragonSlayerItem;
import info.mudbourn.mmsarsenal.armory.item.EdgeOfExistenceItem;
import info.mudbourn.mmsarsenal.armory.item.MurasamaItem;
import info.mudbourn.mmsarsenal.armory.item.MurasamaSheathedItem;
import info.mudbourn.mmsarsenal.armory.item.OdachiItem;
import info.mudbourn.mmsarsenal.armory.item.PunisherItem;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.spell_engine.api.spell.container.SpellContainer;
import net.spell_engine.api.spell.container.SpellContainers;
import net.spell_engine.api.spell.event.SpellEvents;
import net.spell_engine.api.spell.event.SpellHandlers;
import net.spell_engine.internals.casting.SpellCast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

// Armory weapon skills as Spell Engine spells, each castable only while its weapon is in the main hand and ready.
public final class ArmorySkills {

    public static final Identifier DRAGON_SLAYER_SWEEP = MmsArsenal.id("dragon_slayer_sweep");
    public static final Identifier ODACHI_PIERCE = MmsArsenal.id("odachi_pierce");
    public static final Identifier SWORDS_RACE = MmsArsenal.id("swords_race");
    public static final Identifier PUNISHER_IGNITE = MmsArsenal.id("punisher_ignite");
    public static final Identifier BLOODLETTER_INFUSE = MmsArsenal.id("bloodletter_infuse");
    public static final Identifier CRUCIBLE_DOUSE = MmsArsenal.id("crucible_douse");
    public static final Identifier CRUCIBLE_RELIGHT = MmsArsenal.id("crucible_relight");
    public static final Identifier NON_EXISTENCE = MmsArsenal.id("non_existence");
    public static final Identifier MURASAMA_SHEATHE = MmsArsenal.id("murasama_sheathe");
    public static final Identifier MURASAMA_QUICKDRAW = MmsArsenal.id("murasama_quickdraw");

    private static final Map<Identifier, Skill> SKILLS = new HashMap<>();

    private ArmorySkills() {
    }

    // A weapon's skill container holding exactly these spells.
    public static SpellContainer container(Identifier... spells) {
        return SpellContainers.forWeapon(SpellContainer.ContentType.MAGIC, List.of(spells));
    }

    // Binds a skill to the item that casts it, when it may be cast, and what the cast does.
    private static void define(Identifier spell, Supplier<Item> item, BiPredicate<Player, ItemStack> ready, BiConsumer<ServerPlayer, ItemStack> cast) {
        SKILLS.put(spell, new Skill(item, ready, cast));
        SpellHandlers.registerCustomDelivery(spell, (level, data, caster, targets, context, position) -> {
            if (!(caster instanceof ServerPlayer player)) {
                return false;
            }
            ItemStack stack = player.getMainHandItem();
            Skill skill = SKILLS.get(spell);
            if (!skill.matches(player, stack)) {
                return false;
            }
            skill.cast().accept(player, stack);
            return true;
        });
    }

    public static void register() {
        define(DRAGON_SLAYER_SWEEP, () -> ArmoryItems.DRAGON_SLAYER,
            (player, stack) -> ((DragonSlayerItem) stack.getItem()).ready(stack),
            (player, stack) -> ((DragonSlayerItem) stack.getItem()).release(player, stack));
        define(ODACHI_PIERCE, () -> ArmoryItems.ODACHI,
            (player, stack) -> ((OdachiItem) stack.getItem()).ready(stack),
            (player, stack) -> ((OdachiItem) stack.getItem()).release(player, stack));
        define(SWORDS_RACE, () -> ArmoryItems.ODACHI,
            (player, stack) -> ((OdachiItem) stack.getItem()).raceReady(stack),
            (player, stack) -> ((OdachiItem) stack.getItem()).readyRace(player, stack));
        define(PUNISHER_IGNITE, () -> ArmoryItems.PUNISHER,
            (player, stack) -> ((PunisherItem) stack.getItem()).skillReady(stack),
            (player, stack) -> ((PunisherItem) stack.getItem()).ignite(player, stack));
        define(BLOODLETTER_INFUSE, () -> ArmoryItems.BLOODLETTER,
            (player, stack) -> ((BloodletterItem) stack.getItem()).skillReady(player, stack),
            (player, stack) -> ((BloodletterItem) stack.getItem()).toggle(player, stack));
        define(CRUCIBLE_DOUSE, () -> ArmoryItems.CRUCIBLE, (player, stack) -> true, CrucibleItem::douse);
        define(CRUCIBLE_RELIGHT, () -> ArmoryItems.CRUCIBLE_INACTIVE, CrucibleInactiveItem::skillReady, CrucibleInactiveItem::relight);
        define(NON_EXISTENCE, () -> ArmoryItems.EDGE_OF_EXISTENCE, (player, stack) -> true, EdgeOfExistenceItem::vanish);
        define(MURASAMA_SHEATHE, () -> ArmoryItems.MURASAMA, MurasamaItem::skillReady, MurasamaItem::sheathe);
        define(MURASAMA_QUICKDRAW, () -> ArmoryItems.MURASAMA_SHEATHED, MurasamaSheathedItem::skillReady, MurasamaSheathedItem::quickdraw);
        SpellEvents.CASTING_ATTEMPT.PRE.register(args -> {
            for (Map.Entry<Identifier, Skill> entry : SKILLS.entrySet()) {
                if (args.spell().is(entry.getKey()) && !entry.getValue().matches(args.caster(), args.caster().getMainHandItem())) {
                    return SpellCast.Attempt.failOnCooldown(new SpellCast.Attempt.OnCooldownInfo());
                }
            }
            return null;
        });
    }

    private record Skill(Supplier<Item> item, BiPredicate<Player, ItemStack> ready, BiConsumer<ServerPlayer, ItemStack> cast) {

        boolean matches(Player player, ItemStack stack) {
            return stack.is(this.item.get()) && this.ready.test(player, stack);
        }
    }
}
