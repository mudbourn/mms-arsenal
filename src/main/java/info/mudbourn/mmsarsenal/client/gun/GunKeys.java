package info.mudbourn.mmsarsenal.client.gun;

import com.mojang.blaze3d.platform.InputConstants;
import info.mudbourn.mmsarsenal.MmsArsenal;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

// Gun key bindings, and which vanilla buttons shoot and aim.
public final class GunKeys {

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(MmsArsenal.id("guns"));

    public static final KeyMapping RELOAD = register("reload", InputConstants.KEY_R);
    public static final KeyMapping UNLOAD = register("unload", InputConstants.KEY_U);
    public static final KeyMapping MELEE = register("melee", InputConstants.KEY_V);
    public static final KeyMapping INSPECT = register("inspect", InputConstants.KEY_Y);

    private GunKeys() {
    }

    public static void register() {
    }

    public static KeyMapping shoot() {
        return Minecraft.getInstance().options.keyAttack;
    }

    public static KeyMapping aim() {
        return Minecraft.getInstance().options.keyUse;
    }

    private static KeyMapping register(String name, int key) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.mms_arsenal." + name,
            key,
            CATEGORY
        ));
    }
}
