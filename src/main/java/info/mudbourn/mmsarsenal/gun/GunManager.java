package info.mudbourn.mmsarsenal.gun;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import info.mudbourn.mmsarsenal.MmsArsenal;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;

// Loads gun stats from data/<namespace>/guns/<item>.json, keyed by the item they belong to.
public final class GunManager extends SimpleJsonResourceReloadListener<JsonElement> {

    public static final Identifier ID = MmsArsenal.id("guns");

    private static final Gun DEFAULT = Gun.fromJson(new JsonObject());

    private static Map<Identifier, Gun> serverGuns = Map.of();
    private static Map<Identifier, String> serverSources = Map.of();
    private static Map<Identifier, Gun> clientGuns = Map.of();

    public GunManager() {
        super(ExtraCodecs.JSON, FileToIdConverter.json("guns"));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, Gun> guns = new HashMap<>();
        Map<Identifier, String> sources = new HashMap<>();
        files.forEach((id, json) -> {
            try {
                guns.put(id, Gun.fromJson(json.getAsJsonObject()));
                sources.put(id, json.toString());
            } catch (RuntimeException e) {
                MmsArsenal.LOG.error("Could not read gun {}", id, e);
            }
        });
        serverGuns = Map.copyOf(guns);
        serverSources = Map.copyOf(sources);
        MmsArsenal.LOG.info("Loaded {} guns", guns.size());
    }

    // The JSON each loaded gun was read from, sent to clients so both sides share the same stats.
    public static Map<Identifier, String> sources() {
        return serverSources;
    }

    // Replaces the client's guns with the ones the server sent.
    public static void acceptClient(Map<Identifier, String> sources) {
        Map<Identifier, Gun> guns = new HashMap<>();
        sources.forEach((id, json) -> guns.put(id, Gun.fromJson(JsonParser.parseString(json).getAsJsonObject())));
        clientGuns = Map.copyOf(guns);
    }

    public static Gun get(Item item, boolean client) {
        Gun gun = (client ? clientGuns : serverGuns).get(BuiltInRegistries.ITEM.getKey(item));
        return gun == null ? DEFAULT : gun;
    }
}
