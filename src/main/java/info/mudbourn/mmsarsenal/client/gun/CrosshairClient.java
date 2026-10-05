package info.mudbourn.mmsarsenal.client.gun;

import info.mudbourn.mmsarsenal.MmsArsenal;
import info.mudbourn.mmsarsenal.gun.Gun;
import info.mudbourn.mmsarsenal.gun.GunItem;
import info.mudbourn.mmsarsenal.gun.SpreadTracker;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

// The gun crosshair: four bars that open with spread and kick out on each shot, a centre dot when tight, and hit markers.
public final class CrosshairClient {

    private static final CrosshairClient INSTANCE = new CrosshairClient();
    private static final Identifier HORIZONTAL = texture("dynamic_horizontal");
    private static final Identifier VERTICAL = texture("dynamic_vertical");
    private static final Identifier DOT = texture("dot");
    private static final Identifier HIT_MARKER = texture("special_hit_marker");
    private static final Identifier CRIT_HIT_MARKER = texture("special_crit_hit_marker");
    private static final float DOT_THRESHOLD = 0.8F;

    private float scale;
    private float prevScale;
    private float fireBloom;
    private float prevFireBloom;

    public static CrosshairClient get() {
        return INSTANCE;
    }

    public void tick() {
        this.prevScale = this.scale;
        this.scale *= 0.5F;
        this.prevFireBloom = this.fireBloom;
        if (this.fireBloom > 0.0F) {
            float threshold = SpreadTracker.SPREAD_THRESHOLD / 50.0F;
            this.fireBloom -= Math.min(3.0F / Math.max(threshold, 1.0F), this.fireBloom);
        }
    }

    public void onFire() {
        this.prevScale = 0.0F;
        this.scale = 0.6F;
        this.fireBloom = 3.0F;
    }

    // Wraps the vanilla crosshair: draws the hit marker, hides the crosshair while aimed in, and swaps in the gun crosshair.
    public HudElement wrap(HudElement vanilla) {
        return (graphics, delta) -> {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) {
                vanilla.render(graphics, delta);
                return;
            }
            float partialTick = delta.getGameTimeDeltaPartialTick(true);
            if (GunView.get().isRenderingHitMarker()) {
                this.renderHitMarker(graphics, partialTick);
            }
            if (AimHandler.get().normalisedProgress() > 0.5 && mc.options.getCameraType().isFirstPerson()) {
                return;
            }
            ItemStack held = player.getMainHandItem();
            if (!(held.getItem() instanceof GunItem gunItem)) {
                vanilla.render(graphics, delta);
                return;
            }
            if (mc.options.getCameraType() == CameraType.FIRST_PERSON && !(player.getUseItem().getItem() instanceof ShieldItem)) {
                this.renderDynamic(graphics, gunItem.getGun(true), partialTick);
            }
        };
    }

    private void renderDynamic(GuiGraphics graphics, Gun gun, float partialTick) {
        float aiming = (float) AimHandler.get().normalisedProgress();
        float sprint = GunView.get().sprintTransition(partialTick);
        float bloom = Math.min(Mth.lerp(partialTick, this.prevFireBloom, this.fireBloom), 1.0F);
        float spreadModifier = (1.0F / SpreadTracker.MAX_COUNT) * bloom;
        spreadModifier = (float) Mth.lerp(sprint * 0.5, spreadModifier, 1.0);
        float baseSpread = gun.general().spread();
        float minSpread = gun.general().alwaysSpread() ? baseSpread : 0.0F;
        float spread = Mth.clamp(Mth.lerp(spreadModifier, minSpread, baseSpread) * Mth.lerp(aiming, 1.0F, 0.5F), 0.0F, 32.0F);
        boolean dot = spread <= DOT_THRESHOLD;

        float baseScale = 1.0F + Mth.lerp(partialTick, this.prevScale, this.scale) * 2.0F;
        float scale = baseScale + spread * 2.0F;
        float scaleSize = scale / 6.0F + 1.15F;
        float tightness = 0.8F - 1.0F / 2.0F;
        float translate = (float) (Mth.lerp(0.95, scaleSize - 1.0F, Math.log(scaleSize)) * 2.8F);
        float centerX = Math.round(graphics.guiWidth() / 2.0F) - 0.5F;
        float centerY = Math.round(graphics.guiHeight() / 2.0F) - 0.5F;
        float length = 7.0F;
        float width = 1.0F;

        this.bar(graphics, HORIZONTAL, centerX, centerY, scaleSize, 1.0F, -length / 2.0F - translate + tightness, -width / 2.0F, 7, 1, 0, 0, 9, 1);
        this.bar(graphics, HORIZONTAL, centerX, centerY, scaleSize, 1.0F, -length / 2.0F + translate - tightness, -width / 2.0F, 7, 1, 0, 8, 9, 1);
        this.bar(graphics, VERTICAL, centerX, centerY, 1.0F, scaleSize, -width / 2.0F, -length / 2.0F - translate + tightness, 1, 7, 0, 0, 1, 9);
        this.bar(graphics, VERTICAL, centerX, centerY, 1.0F, scaleSize, -width / 2.0F, -length / 2.0F + translate - tightness, 1, 7, 8, 0, 1, 9);
        if (dot) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(centerX - 4.5F, centerY - 4.5F);
            graphics.pose().scale(9.0F / 16.0F, 9.0F / 16.0F);
            graphics.blit(RenderPipelines.CROSSHAIR, DOT, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
            graphics.pose().popMatrix();
        }
    }

    // One crosshair bar: a strip of the 9x9 bar texture, stretched along the bar's length about the screen centre.
    private void bar(GuiGraphics graphics, Identifier texture, float centerX, float centerY, float scaleX, float scaleY, float offsetX, float offsetY, int width, int height, int u, int v, int regionWidth, int regionHeight) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(scaleX, scaleY);
        graphics.pose().translate(offsetX, offsetY);
        graphics.blit(RenderPipelines.CROSSHAIR, texture, 0, 0, u, v, width, height, regionWidth, regionHeight, 9, 9);
        graphics.pose().popMatrix();
    }

    private void renderHitMarker(GuiGraphics graphics, float partialTick) {
        float size = 9.0F;
        float scale = 1.5F + GunView.get().hitMarkerProgress(partialTick);
        Identifier texture = GunView.get().hitMarkerCrit() ? CRIT_HIT_MARKER : HIT_MARKER;
        graphics.pose().pushMatrix();
        graphics.pose().translate(Math.round(graphics.guiWidth() / 2.0F) - 0.5F, Math.round(graphics.guiHeight() / 2.0F) - 0.5F);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(-size / 2.0F, -size / 2.0F);
        graphics.pose().scale(size / 16.0F, size / 16.0F);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
        graphics.pose().popMatrix();
    }

    private static Identifier texture(String name) {
        return MmsArsenal.id("textures/crosshair/" + name + ".png");
    }
}
