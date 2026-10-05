package info.mudbourn.mmsarsenal.gun;

import info.mudbourn.mmsarsenal.MmsArsenal;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

// Gamerules that govern what guns may do to the world.
public final class GunGameRules {

    // Whether bullets, burning rounds and rockets may break, melt or ignite blocks.
    public static final GameRule<Boolean> GUN_GRIEFING = GameRuleBuilder.forBoolean(true)
        .category(GameRuleCategory.MISC)
        .buildAndRegister(MmsArsenal.id("gun_griefing"));

    private GunGameRules() {
    }

    // Loads this class so its rules register at startup.
    public static void register() {
    }
}
