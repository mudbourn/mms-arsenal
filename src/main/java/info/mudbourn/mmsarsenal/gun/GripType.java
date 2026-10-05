package info.mudbourn.mmsarsenal.gun;

import net.minecraft.resources.Identifier;

// How a gun is held, which picks its third-person pose.
public enum GripType {
    ONE_HANDED("one_handed"),
    TWO_HANDED("two_handed"),
    MINI_GUN("mini_gun"),
    BAZOOKA("bazooka");

    private final String name;

    GripType(String name) {
        this.name = name;
    }

    // Reads a grip type id by its path, falling back to one-handed like an unknown grip does.
    public static GripType byId(String id) {
        Identifier parsed = Identifier.tryParse(id);
        String path = parsed == null ? id : parsed.getPath();
        for (GripType type : values()) {
            if (type.name.equals(path)) {
                return type;
            }
        }
        return ONE_HANDED;
    }
}
