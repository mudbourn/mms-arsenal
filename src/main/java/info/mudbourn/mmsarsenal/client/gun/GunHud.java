package info.mudbourn.mmsarsenal.client.gun;

import info.mudbourn.mmsarsenal.MmsArsenal;
import info.mudbourn.mmsarsenal.gun.Gun;
import info.mudbourn.mmsarsenal.gun.GunAmmo;
import info.mudbourn.mmsarsenal.gun.GunComponents;
import info.mudbourn.mmsarsenal.gun.GunItem;
import info.mudbourn.mmsarsenal.gun.GunState;
import info.mudbourn.mmsarsenal.gun.ReloadType;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

// The gun HUD: the long-scope overlay, the hold, power and overheat timers, and the ammo counter.
public final class GunHud {

    private static final Identifier SCOPE_OVERLAY = MmsArsenal.id("textures/effect/scope_long_overlay.png");
    private static final Identifier OVERHEAT = MmsArsenal.id("textures/gui/timer/overheat.png");
    private static final Identifier POWER = MmsArsenal.id("textures/gui/timer/power.png");
    private static final Identifier HOLD = MmsArsenal.id("textures/gui/timer/hold.png");

    private static float scopeScale;

    private GunHud() {
    }

    // The black-edged long scope over the screen while aiming a scoped gun in first person.
    public static void renderScope(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        scopeScale = Mth.lerp(0.5F * delta.getGameTimeDeltaTicks(), scopeScale, 1.125F);
        if (!mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        GunSights.Scope scope = GunSights.scope(player.getMainHandItem());
        if (!AimHandler.get().isAiming() || scope == null || !scope.overlay()) {
            scopeScale = 0.5F;
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        float shortest = Math.min(width, height);
        float factor = Math.min(width / shortest, height / shortest) * scopeScale;
        int size = Mth.floor(shortest * factor);
        int left = (width - size) / 2;
        int top = (height - size) / 2;
        int right = left + size;
        int bottom = top + size;
        graphics.blit(RenderPipelines.GUI_TEXTURED, SCOPE_OVERLAY, left, top, 0.0F, 0.0F, size, size, size, size);
        graphics.fill(RenderPipelines.GUI, 0, bottom, width, height, 0xFF000000);
        graphics.fill(RenderPipelines.GUI, 0, 0, width, top, 0xFF000000);
        graphics.fill(RenderPipelines.GUI, 0, top, left, bottom, 0xFF000000);
        graphics.fill(RenderPipelines.GUI, right, top, width, bottom, 0xFF000000);
    }

    // The timers under the crosshair and the ammo counter in the corner.
    public static void renderHud(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GunItem gunItem)) {
            return;
        }
        Gun gun = gunItem.getGun(true);
        boolean showTimers = !AimHandler.get().isAiming()
            || AimHandler.get().normalisedProgress() > 0.5 && !mc.options.getCameraType().isFirstPerson();
        if (showTimers) {
            int centerX = graphics.guiWidth() / 2 - 32;
            int centerY = graphics.guiHeight() / 2;
            ShootHandler shoot = ShootHandler.get();
            if (gun.general().fireTimer() != 0) {
                timer(graphics, HOLD, centerX, centerY + 38, (float) shoot.fireTimer() / gun.general().fireTimer());
            }
            if (gun.general().overheatTimer() != 0) {
                timer(graphics, OVERHEAT, centerX, centerY + 24, (float) shoot.overheatTimer() / gun.general().overheatTimer());
            }
        }
        if (!held.has(GunComponents.AMMO_COUNT)) {
            return;
        }
        List<ItemStack> ammo = gun.reloads().reloadType() == ReloadType.SINGLE_ITEM
            ? GunAmmo.findAll(player, gun.reloads().reloadItem())
            : GunAmmo.findAll(player, gun.projectile().item());
        int startX = (int) (graphics.guiWidth() * 0.95 - 120.0);
        int startY = graphics.guiHeight() - 65;
        int color = 0xFF000000 | held.getOrDefault(DataComponents.RARITY, net.minecraft.world.item.Rarity.COMMON).color().getColor();
        graphics.drawString(mc.font, held.getHoverName(), startX, startY - 10, color, true);
        boolean reloading = GunState.isReloading(player);
        String ammoText = reloading && !player.isCrouching() ? "Reloading" : String.format("%03d", GunComponents.ammo(held));
        graphics.pose().pushMatrix();
        graphics.pose().translate(startX, startY);
        graphics.pose().scale(2.0F, 2.0F);
        graphics.drawString(mc.font, ammoText, 0, 0, 0xFFFFFFFF, true);
        graphics.pose().popMatrix();
        if (!reloading || player.isCrouching()) {
            int reserve = gun.reloads().reloadType() == ReloadType.SINGLE_ITEM
                ? ammo.size() * gun.reloads().maxAmmo()
                : GunAmmo.total(ammo);
            String reserveText = player.isCreative() || GunComponents.ignoresAmmo(held) ? "9999" : String.format("%04d", reserve);
            graphics.drawString(mc.font, reserveText, startX + 38, startY, 0xFF878787, true);
        }
    }

    private static void timer(GuiGraphics graphics, Identifier texture, int x, int y, float progress) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, 64, 6, 64, 12);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 6.0F, (int) (64.0F * progress), 6, 64, 12);
    }
}
