package info.mudbourn.mmsarsenal.gun;

import net.minecraft.resources.Identifier;

// How holding the trigger turns into shots.
public enum FireMode {
    SEMI_AUTOMATIC("semi_automatic"),
    AUTOMATIC("automatic"),
    BURST("burst"),
    PULSE("pulse"),
    RELEASE_FIRE("release_fire");

    private final String name;

    FireMode(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    // Reads a fire mode id by its path, falling back to semi-automatic like an unknown mode does.
    public static FireMode byId(String id) {
        Identifier parsed = Identifier.tryParse(id);
        String path = parsed == null ? id : parsed.getPath();
        for (FireMode mode : values()) {
            if (mode.name.equals(path)) {
                return mode;
            }
        }
        return SEMI_AUTOMATIC;
    }
}
