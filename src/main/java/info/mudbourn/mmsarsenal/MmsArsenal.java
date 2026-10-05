package info.mudbourn.mmsarsenal;

import info.mudbourn.mmsarsenal.armory.ArmoryComponents;
import info.mudbourn.mmsarsenal.armory.ArmoryEffects;
import info.mudbourn.mmsarsenal.armory.ArmoryEvents;
import info.mudbourn.mmsarsenal.armory.ArmoryItems;
import info.mudbourn.mmsarsenal.armory.ArmorySounds;
import info.mudbourn.mmsarsenal.net.NonexistencePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Wires the Armory weapons and their Non-Existence payload.
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

        LOG.info("MMS Arsenal loaded");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    // Maps the mms_combat id an entry was saved under before the Armory moved here onto its current id.
    public static void aliasLegacy(Registry<?> registry, String path) {
        registry.addAlias(Identifier.fromNamespaceAndPath(LEGACY_MOD_ID, path), id(path));
    }
}
