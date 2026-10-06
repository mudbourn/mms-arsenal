package info.mudbourn.mmsarsenal.client.gun;

import info.mudbourn.mmsarsenal.MmsArsenal;
import info.mudbourn.mmsarsenal.gun.DrawTracker;
import info.mudbourn.mmsarsenal.gun.GunItem;
import info.mudbourn.mmsarsenal.gun.entity.GunEntities;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

// Wires the gun client: keys, payloads, particles, per-tick and per-frame handlers, HUD layers and trails.
public final class GunClient {

    private GunClient() {
    }

    public static void register() {
        GunKeys.register();
        GunClientNetwork.register();
        GunParticleClient.register();
        SmokeFogEnvironment.register();
        GunItem.setShiftDown(() -> Minecraft.getInstance().hasShiftDown());
        EntityRendererRegistry.register(GunEntities.PROJECTILE, NoopRenderer::new);
        EntityRendererRegistry.register(GunEntities.BLAZE_PROJECTILE, NoopRenderer::new);
        EntityRendererRegistry.register(GunEntities.SPECTRE_PROJECTILE, NoopRenderer::new);
        EntityRendererRegistry.register(GunEntities.ROCKET, NoopRenderer::new);
        EntityRendererRegistry.register(GunEntities.GRENADE, ThrownItemRenderer::new);
        EntityRendererRegistry.register(GunEntities.STUN_GRENADE, ThrownItemRenderer::new);
        EntityRendererRegistry.register(GunEntities.SMOKE_GRENADE, ThrownItemRenderer::new);
        EntityRendererRegistry.register(GunEntities.MOLOTOV_COCKTAIL, ThrownItemRenderer::new);

        ClientTickEvents.START_CLIENT_TICK.register(GunClient::startTick);
        ClientTickEvents.END_CLIENT_TICK.register(GunClient::endTick);
        WorldRenderEvents.START_MAIN.register(context -> startFrame(Minecraft.getInstance()));
        WorldRenderEvents.AFTER_ENTITIES.register(context -> BulletTrails.get().render(
            context.matrices(),
            context.consumers(),
            Minecraft.getInstance().gameRenderer.getMainCamera().position(),
            Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true)
        ));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> BulletTrails.get().clear());

        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, vanilla -> CrosshairClient.get().wrap(vanilla));
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, MmsArsenal.id("gun_scope"), GunHud::renderScope);
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, MmsArsenal.id("gun_hud"), GunHud::renderHud);
        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, MmsArsenal.id("flash_blind"), StunEffectsClient::renderBlindness);
    }

    private static void startTick(Minecraft mc) {
        AimHandler.get().tick(mc);
        ShootHandler.get().tickStart(mc);
    }

    private static void endTick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player != null) {
            DrawTracker.get(player).tick(player, true);
            if (player.getMainHandItem().getItem() instanceof GunItem && !mc.options.getCameraType().isFirstPerson()) {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            }
            while (GunKeys.INSPECT.consumeClick()) {
                GunAnimationDriver.get().onInspect(player);
            }
        }
        ShootHandler.get().tickEnd(mc);
        ReloadClient.get().handleKeys(mc);
        ReloadClient.get().tick(mc);
        GunView.get().tick(mc);
        GunAnimationDriver.get().tick(mc);
        CrosshairClient.get().tick();
        AimHandler.get().tickOthers(mc);
        BulletTrails.get().tick(mc);
        StunEffectsClient.tick(mc);
    }

    // Per-frame updates: smoothed aim, the camera's recoil kick and the gun model's recoil.
    private static void startFrame(Minecraft mc) {
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        AimHandler.get().updateFrame(partialTick);
        RecoilClient.get().onRenderFrame(mc.getDeltaTracker().getGameTimeDeltaTicks());
        if (mc.player != null) {
            RecoilClient.get().updateGunRecoil(mc.player, partialTick);
        }
    }
}
