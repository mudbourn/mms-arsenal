package info.mudbourn.mmsarsenal;

import info.mudbourn.mmsarsenal.armory.ArmoryComponents;
import info.mudbourn.mmsarsenal.armory.ArmoryEffects;
import info.mudbourn.mmsarsenal.armory.ArmoryEvents;
import info.mudbourn.mmsarsenal.armory.ArmoryItems;
import info.mudbourn.mmsarsenal.armory.ArmorySounds;
import info.mudbourn.mmsarsenal.armory.item.DragonSlayerItem;
import info.mudbourn.mmscombat.dummy.TrainingDummy;
import info.mudbourn.mmsweapons.focus.Focus;
import info.mudbourn.mmsweapons.focus.FocusSkill;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import info.mudbourn.mmsarsenal.gun.Guns;
import info.mudbourn.mmsarsenal.gun.throwable.SmokeGrenadeEntity;
import info.mudbourn.mmsarsenal.net.DragonSlayerSweepPayload;
import info.mudbourn.mmsarsenal.net.NonexistencePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Wires the Armory weapons and the guns.
public class MmsArsenal implements ModInitializer {
    public static final String MOD_ID = "mms_arsenal";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    private static final String LEGACY_MOD_ID = "mms_combat";

    @Override
    public void onInitialize() {
        ArmoryComponents.register();
        ArmorySounds.register();
        ArmoryEffects.register();
        ArmoryItems.register();
        ArmoryEvents.register();
        PayloadTypeRegistry.playS2C().register(NonexistencePayload.TYPE, NonexistencePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DragonSlayerSweepPayload.TYPE, DragonSlayerSweepPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(DragonSlayerSweepPayload.TYPE, (payload, context) ->
            DragonSlayerItem.strike(context.player(), payload.targets()));
        Guns.register();
        // The Training Dummy moved to mms-combat; keep old dummies loading and run Armory hit effects on it.
        BuiltInRegistries.ITEM.addAlias(id("training_dummy"), Identifier.fromNamespaceAndPath("mms_combat", "training_dummy"));
        TrainingDummy.addDamageHook(ArmoryEvents::onDamage);
        // mms-weapons cannot see the dummy, so its focus weapons are fed dummy hits from here.
        TrainingDummy.addDamageHook((dummy, source, dealt) -> Focus.onDamage(source, dealt));
        // Focus smoke for the Dragon Slayer and Odachi, drawn by mms-weapons from where the blade is rendered.
        // Geo models start at (0.5, 0.51, 0.5) in item space and run up in pixels; these follow each blade's height.
        // A crouch during the draw cancels it through mms-weapons, without firing or spending focus.
        FocusSkill.addSkill(stack -> stack.getItem() instanceof DragonSlayerItem && DragonSlayerItem.focused(stack));
        Focus.addSmokeSource(stack -> stack.getItem() instanceof DragonSlayerItem && DragonSlayerItem.focused(stack));
        Focus.registerBlade(ArmoryItems.DRAGON_SLAYER, new Focus.Blade(new Vec3(0.5, 0.51 + 1.0 / 16.0, 0.5), new Vec3(0.5, 0.51 + 111.5 / 16.0, 0.5)));
        // The Odachi's blade is straight to 41px, then its last two pieces tilt 80 and 60 degrees into the curve.
        Focus.registerBlade(ArmoryItems.ODACHI, new Focus.Blade(List.of(
            geoPoint(18.14, 0.0),
            geoPoint(41.18, 0.0),
            geoPoint(57.10, 2.83),
            geoPoint(62.83, 6.23))));
        SmokeGrenadeEntity.register();

        LOG.info("MMS Arsenal loaded");
    }

    // A point on a geo item model's centre line, in model pixels up and forward, carried into item space.
    private static Vec3 geoPoint(double up, double forward) {
        return new Vec3(0.5, 0.51 + up / 16.0, 0.5 + forward / 16.0);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    // Maps the mms_combat id an entry was saved under before the Armory moved here onto its current id.
    public static void aliasLegacy(Registry<?> registry, String path) {
        registry.addAlias(Identifier.fromNamespaceAndPath(LEGACY_MOD_ID, path), id(path));
    }
}
