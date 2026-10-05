package info.mudbourn.mmsarsenal.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import info.mudbourn.mmsarsenal.MmsArsenal;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

// The gun gameplay config at config/mms_arsenal.json, with the Just Enough Guns defaults.
public final class ArsenalConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ArsenalConfig instance = new ArsenalConfig();

    public boolean gunDurability = true;
    public boolean gunAdvantage = true;
    public boolean underwaterFiring = false;
    public boolean enableHeadShots = true;
    public double growBoundingBoxAmount = 0.3;
    public boolean ignoreLeaves = true;
    public boolean enableKnockback = true;
    public boolean mobsDropHelmets = true;
    public boolean playersDropHelmets = true;
    public boolean drawAnimation = true;
    public boolean dynamicLightsOnShooting = true;
    public boolean rocketRiding = true;
    public boolean entitiesDropEchoShards = true;

    public boolean aggroMobs = true;
    public double aggroUnsilencedRange = 20.0;
    public boolean fleeingMobs = true;
    public double fleeingUnsilencedRange = 50.0;
    public List<String> fleeingEntities = List.of(
        "minecraft:cow",
        "minecraft:sheep",
        "minecraft:pig",
        "minecraft:chicken",
        "minecraft:wolf",
        "minecraft:axolotl",
        "minecraft:cat",
        "minecraft:frog",
        "minecraft:fox",
        "minecraft:allay",
        "minecraft:rabbit",
        "minecraft:horse",
        "minecraft:villager",
        "minecraft:bee",
        "minecraft:parrot",
        "minecraft:turtle",
        "minecraft:donkey",
        "minecraft:mule",
        "minecraft:llama",
        "minecraft:panda",
        "minecraft:mooshroom",
        "minecraft:strider",
        "minecraft:ocelot",
        "minecraft:bat",
        "minecraft:squid",
        "minecraft:glow_squid",
        "minecraft:camel"
    );

    public boolean enableBlockRemovalOnExplosions = true;
    public boolean enableGlassBreaking = true;
    public boolean enableWoodBreaking = true;
    public boolean enableStoneBreaking = true;
    public boolean fragileBlockDrops = true;
    public double fragileBaseBreakChance = 1.0;
    public double woodBaseBreakChance = 0.1;
    public double stoneBaseBreakChance = 0.05;
    public boolean setFireToBlocks = true;

    public double missileExplosionRadius = 5.0;
    public double grenadeExplosionRadius = 5.0;
    public double smokeGrenadeCloudDiameter = 18.0;
    public double smokeGrenadeDamage = 0.0;
    public double smokeGrenadeCloudDuration = 20.0;

    public double gunShotMaxDistance = 100.0;
    public double reloadMaxDistance = 24.0;
    public int playerGunfireVolume = 8;
    public double projectileTrackingRange = 200.0;

    public static ArsenalConfig get() {
        return instance;
    }

    public static void load() {
        Path path = configPath();
        if (Files.exists(path)) {
            try {
                ArsenalConfig loaded = GSON.fromJson(Files.readString(path), ArsenalConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (IOException | RuntimeException e) {
                MmsArsenal.LOG.error("Failed to read {}, using defaults", path, e);
            }
        }
        save();
    }

    public static void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(instance));
        } catch (IOException e) {
            MmsArsenal.LOG.error("Failed to write {}", path, e);
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("mms_arsenal.json");
    }
}
