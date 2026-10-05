package info.mudbourn.mmsarsenal.client;

import info.mudbourn.mmsarsenal.armory.ArmoryClock;
import info.mudbourn.mmsarsenal.client.armory.ComponentCounterProperty;
import info.mudbourn.mmsarsenal.client.armory.ElapsedTicksProperty;
import info.mudbourn.mmsarsenal.client.armory.NonexistenceClientState;
import info.mudbourn.mmsarsenal.client.gun.GunClient;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

// Registers the Armory item model properties, Non-Existence rendering, and the gun client.
public class MmsArsenalClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ComponentCounterProperty.register();
        ElapsedTicksProperty.register();
        NonexistenceClientState.register();
        ArmoryClock.setDisplay(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            return level == null ? ArmoryClock.UNKNOWN : level.getGameTime();
        });
        GunClient.register();
    }
}
