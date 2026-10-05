package info.mudbourn.mmsarsenal.gun;

import net.minecraft.resources.Identifier;

// How a gun takes ammo back in.
public enum ReloadType {
    MANUAL("manual"),
    MAG_FED("mag_fed"),
    SINGLE_ITEM("single_item"),
    INVENTORY_FED("inventory_fed");

    private final String name;

    ReloadType(String name) {
        this.name = name;
    }

    // Reads a reload type id by its path, falling back to manual like an unknown type does.
    public static ReloadType byId(String id) {
        Identifier parsed = Identifier.tryParse(id);
        String path = parsed == null ? id : parsed.getPath();
        for (ReloadType type : values()) {
            if (type.name.equals(path)) {
                return type;
            }
        }
        return MANUAL;
    }
}
