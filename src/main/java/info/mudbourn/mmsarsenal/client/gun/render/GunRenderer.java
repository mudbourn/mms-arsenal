package info.mudbourn.mmsarsenal.client.gun.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import info.mudbourn.mmsarsenal.MmsArsenal;
import info.mudbourn.mmsarsenal.client.gun.AimHandler;
import info.mudbourn.mmsarsenal.client.gun.GunSights;
import info.mudbourn.mmsarsenal.client.gun.GunView;
import info.mudbourn.mmsarsenal.client.gun.RecoilClient;
import info.mudbourn.mmsarsenal.client.gun.ShootHandler;
import info.mudbourn.mmsarsenal.gun.Gun;
import info.mudbourn.mmsarsenal.gun.GunItems;
import info.mudbourn.mmsarsenal.gun.GripType;
import info.mudbourn.mmsrendercommon.client.geo.GeoAssets;
import info.mudbourn.mmsrendercommon.client.geo.GeoModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

// Draws a gun frame: in the local first-person hand with Just Enough Guns' bob, recoil, aim, sway and sight transforms, arms and muzzle flash.
public final class GunRenderer {

    private static final Identifier MUZZLE_FLASH = MmsArsenal.id("textures/effect/muzzle_flash.png");
    private static final Identifier SCOPE_TEXTURE = MmsArsenal.id("textures/attachment/telescopic_sight.png");
    private static final Identifier SCOPE_GEO = MmsArsenal.id("geo/item/attachment/telescopic_sight.geo.json");
    private static final double SWAY_SENSITIVITY = 0.3;

    private GunRenderer() {
    }

    public static void submit(GunFrame frame, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay) {
        poseStack.pushPose();
        int drawLight = light;
        if (frame.firstPerson()) {
            if (frame.aiming() && frame.scope() != null && frame.scope().overlay()) {
                poseStack.popPose();
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
            applyFirstPersonTransforms(frame, poseStack, partialTick);
            LocalPlayer player = mc.player;
            boolean flashing = ShootHandler.get().isShooting() && GunView.get().hasMuzzleFlash(player.getId());
            if (flashing) {
                drawMuzzleFlash(frame, poseStack, collector, GunView.get().muzzleFlashRandom(player.getId()), partialTick);
            }
            drawLight = eyeLight(player, partialTick, flashing);
        }
        poseStack.translate(0.5F, 0.51F, 0.5F);
        int finalLight = drawLight;
        GunBoneStyle style = new GunBoneStyle(frame);
        collector.submitCustomGeometry(
            poseStack,
            frame.renderType(),
            (pose, consumer) -> frame.model().draw(pose, consumer, finalLight, overlay, -1, frame.poses(), style)
        );
        if (frame.firstPerson()) {
            drawArms(frame, poseStack, collector, finalLight);
        }
        if (frame.scope() != null) {
            drawScope(frame, poseStack, collector, finalLight, overlay);
        }
        poseStack.popPose();
    }

    private static void applyFirstPersonTransforms(GunFrame frame, PoseStack poseStack, float partialTick) {
        ItemTransform transform = frame.firstPersonTransform();
        float tx = transform.translation().x();
        float ty = transform.translation().y();
        float tz = transform.translation().z();
        applyBobbing(poseStack, partialTick);
        applyRecoil(frame, poseStack);
        applyAiming(poseStack, frame, tx, ty, tz);
        applySway(poseStack, frame.gun(), tx, ty, tz, partialTick);
        double ads = AimHandler.get().normalisedProgress();
        if (ads > 0.0 && frame.gun().canAimDownSight()) {
            applySight(frame, poseStack, transform, ads);
        }
    }

    private static void applyBobbing(PoseStack poseStack, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.bobView().get() || !(mc.getCameraEntity() instanceof AbstractClientPlayer player)) {
            return;
        }
        ClientAvatarState state = player.avatarState();
        float walked = state.getBackwardsInterpolatedWalkDistance(partialTick);
        float bob = state.getInterpolatedBob(partialTick);
        poseStack.mulPose(Axis.XP.rotationDegrees(-(Math.abs(Mth.cos(walked * Mth.PI - 0.2F) * bob) * 5.0F)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-(Mth.sin(walked * Mth.PI) * bob * 3.0F)));
        poseStack.translate(-(Mth.sin(walked * Mth.PI) * bob * 0.5F), Math.abs(Mth.cos(walked * Mth.PI) * bob), 0.0F);
        bob *= player.isSprinting() ? 8.0F : 4.0F;
        double invertZoom = 1.0 - AimHandler.get().normalisedProgress() * GunView.get().sprintIntensity();
        if (!AimHandler.get().isAiming()) {
            poseStack.mulPose(Axis.XP.rotationDegrees(Math.abs(Mth.cos(walked * Mth.PI - 0.2F) * bob) * -2.0F * (float) invertZoom));
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(walked * Mth.PI) * bob * 3.0F * (float) invertZoom));
        }
    }

    private static void applyRecoil(GunFrame frame, PoseStack poseStack) {
        Gun gun = frame.gun();
        RecoilClient recoil = RecoilClient.get();
        double normal = recoil.gunRecoilNormal();
        if (frame.scope() != null) {
            normal -= normal * (0.5 * AimHandler.get().normalisedProgress());
        }
        double kick = gun.general().recoilKick() * 0.0625 * normal * recoil.adsRecoilReduction(gun);
        float lift = (float) (gun.general().recoilAngle() * normal * recoil.adsRecoilReduction(gun));
        float swayAmount = (float) (2.0 + (1.0 - AimHandler.get().normalisedProgress()));
        float sway = (float) ((recoil.gunRecoilRandom() * swayAmount - swayAmount / 2.0F) * normal);
        poseStack.translate(0.0, 0.0, kick);
        poseStack.translate(0.0, 0.0, 0.15);
        poseStack.mulPose(Axis.YP.rotationDegrees(sway / 5.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(sway / 5.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(lift / 5.0F));
        poseStack.translate(0.0, 0.0, -0.15);
    }

    private static void applyAiming(PoseStack poseStack, GunFrame frame, float x, float y, float z) {
        poseStack.translate(x, y, z);
        poseStack.translate(0.0, -0.25, 0.25);
        float aiming = (float) Math.sin(Math.toRadians(AimHandler.get().normalisedProgress() * 180.0));
        aiming = (float) GunSights.sight(frame.stack()).aimTransformCurve().apply(aiming / 2.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(aiming * 10.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(aiming * 8.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(aiming * 8.0F));
        poseStack.translate(0.0, 0.25, -0.25);
        poseStack.translate(-x, -y, -z);
    }

    private static void applySway(PoseStack poseStack, Gun gun, float x, float y, float z, float partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        poseStack.translate(x, y, z);
        double zOffset = gun.general().gripType() == GripType.ONE_HANDED ? 0.5 : 0.35;
        poseStack.translate(0.0, -0.25, zOffset);
        poseStack.mulPose(Axis.XP.rotationDegrees(GunView.get().fallSway(partialTick) / 2.0F));
        poseStack.translate(0.0, 0.25, -zOffset);
        double aimDamping = 1.0 - 0.5 * AimHandler.get().normalisedProgress();
        float bobPitch = Mth.rotLerp(partialTick, player.xBobO, player.xBob);
        float headPitch = Mth.rotLerp(partialTick, player.xRotO, player.getXRot());
        float swayPitch = (float) ((headPitch - bobPitch) * aimDamping);
        poseStack.mulPose(Axis.XP.rotationDegrees(swayPitch * (float) SWAY_SENSITIVITY / 2.0F));
        float bobYaw = Mth.rotLerp(partialTick, player.yBobO, player.yBob);
        float headYaw = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
        float swayYaw = (float) ((headYaw - bobYaw) * aimDamping);
        poseStack.mulPose(Axis.YP.rotationDegrees(swayYaw * (float) SWAY_SENSITIVITY / 2.0F));
        poseStack.translate(-x, -y, -z);
    }

    // Moves the gun so the sight, or the scope's eyepiece, lines up with the camera as aim progresses.
    private static void applySight(GunFrame frame, PoseStack poseStack, ItemTransform transform, double ads) {
        Vector3f scale = new Vector3f(transform.scale());
        double x = transform.translation().x() - 0.5 * scale.x();
        double y = transform.translation().y() - 0.5 * scale.y();
        double z = transform.translation().z() - 0.5 * scale.z();
        Vec3 origin = GunSights.GUN_ORIGIN;
        x += origin.x * 0.0625 * scale.x();
        y += origin.y * 0.0625 * scale.y();
        z += origin.z * 0.0625 * scale.z();
        GunSights.Scope scope = frame.scope();
        if (scope != null) {
            Vec3 mount = GunSights.scopePosition(frame.gun()).subtract(origin);
            x += mount.x * 0.0625 * scale.x();
            y += mount.y * 0.0625 * scale.y();
            z += mount.z * 0.0625 * scale.z();
            Vec3 camera = scope.camera().subtract(scope.origin());
            double scopeScale = GunSights.scopeScale(frame.gun());
            x += camera.x * 0.0625 * scale.x() * scopeScale;
            y += (camera.y * 0.0625 * scale.y() + 0.54) * scopeScale;
            z += (camera.z * 0.0625 * scale.z() - 0.16) * scopeScale;
        } else {
            Vec3 iron = GunSights.ironSightCamera(frame.gun()).subtract(origin);
            x += iron.x * 0.0625 * scale.x();
            y += iron.y * 0.0625 * scale.y() + 0.6059;
            z += iron.z * 0.0625 * scale.z() - 0.16 + 0.72;
        }
        double transition = GunSights.sight(frame.stack()).sightCurve().apply(ads);
        poseStack.translate(-0.56 * transition, 0.52 * transition, 0.72 * transition);
        poseStack.translate(-x * transition, -y * transition, -z * transition);
    }

    // The muzzle flash quad, a random spin each shot, scaled up over the frame.
    private static void drawMuzzleFlash(GunFrame frame, PoseStack poseStack, SubmitNodeCollector collector, float random, float partialTick) {
        Gun.Flash flash = frame.gun().display().flash();
        if (flash == null) {
            return;
        }
        poseStack.pushPose();
        Vec3 origin = GunSights.GUN_ORIGIN;
        Vec3 position = flash.offset();
        poseStack.translate(origin.x * 0.0625, origin.y * 0.0625, origin.z * 0.0625);
        poseStack.translate(position.x * 0.0625 + 0.5, position.y * 0.0625 + 1.025, position.z * 0.0625 + 0.525);
        poseStack.translate(-0.5, -0.5, -0.5);
        poseStack.mulPose(Axis.ZP.rotationDegrees(360.0F * random));
        poseStack.mulPose(Axis.XP.rotationDegrees(random >= 0.5F ? 180.0F : 0.0F));
        float size = (float) flash.size();
        float scale = size / 2.0F * partialTick * 1.5F;
        poseStack.scale(scale, scale, 1.0F);
        poseStack.translate(-0.5, -0.5, 0.0);
        boolean alternate = frame.stack().is(GunItems.HYPERSONIC_CANNON)
            || frame.stack().is(GunItems.SOULHUNTER_MK2)
            || frame.stack().is(GunItems.BLOSSOM_RIFLE)
            || frame.stack().isEnchanted();
        float minU = alternate ? 0.5F : 0.0F;
        float maxU = alternate ? 1.0F : 0.5F;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucentEmissive(MUZZLE_FLASH), (pose, consumer) -> {
            vertex(consumer, pose, 0.0F, 0.0F, maxU, 1.0F);
            vertex(consumer, pose, 1.0F, 0.0F, minU, 1.0F);
            vertex(consumer, pose, 1.0F, 1.0F, minU, 0.0F);
            vertex(consumer, pose, 0.0F, 1.0F, maxU, 0.0F);
        });
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v) {
        consumer.addVertex(pose, x, y, 0.0F)
            .setColor(-1)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

    // Light at the player's eyes, brightened while the muzzle flashes.
    private static int eyeLight(LocalPlayer player, float partialTick, boolean flashing) {
        BlockPos eye = BlockPos.containing(player.getEyePosition(partialTick));
        int block = player.isOnFire() ? 15 : player.level().getBrightness(LightLayer.BLOCK, eye);
        if (flashing) {
            block += 3;
        }
        return LightTexture.pack(Math.min(block, 15), player.level().getBrightness(LightLayer.SKY, eye));
    }

    // The player's own arms in their skin, placed on the gun's arm bones.
    private static void drawArms(GunFrame frame, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        AvatarRenderer<AbstractClientPlayer> renderer = mc.getEntityRenderDispatcher().getPlayerRenderer(player);
        PlayerModel model = renderer.getModel();
        Identifier skin = player.getSkin().body().texturePath();
        drawArm(frame, poseStack, collector, light, skin, model.leftArm, model.leftSleeve, "left_arm", -0.25, player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE));
        drawArm(frame, poseStack, collector, light, skin, model.rightArm, model.rightSleeve, "right_arm", 0.25, player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE));
    }

    private static void drawArm(GunFrame frame, PoseStack poseStack, SubmitNodeCollector collector, int light, Identifier skin, ModelPart arm, ModelPart sleeve, String bone, double xShift, boolean sleeveShown) {
        GeoModel model = frame.model();
        Matrix4f matrix = model.boneMatrix(bone, frame.poses());
        Vector3f pivot = model.pivot(bone);
        if (matrix == null || pivot == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(matrix);
        poseStack.scale(0.67F, 0.8F, 0.67F);
        poseStack.translate(xShift, -0.1, 0.1625);
        arm.resetPose();
        arm.setPos(pivot.x(), pivot.y(), pivot.z());
        arm.setRotation(0.0F, 0.0F, 0.0F);
        arm.visible = true;
        sleeve.visible = sleeveShown;
        collector.submitModelPart(arm, poseStack, RenderTypes.entityTranslucent(skin), light, OverlayTexture.NO_OVERLAY, null);
        poseStack.popPose();
    }

    // The scope a gun comes with, mounted at its attachment bone.
    private static void drawScope(GunFrame frame, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay) {
        GeoModel scopeModel = GeoAssets.model(SCOPE_GEO);
        Matrix4f matrix = frame.model().boneMatrix("attachment_bone", frame.poses());
        if (scopeModel == null || matrix == null) {
            return;
        }
        GunSights.Scope scope = frame.scope();
        poseStack.pushPose();
        poseStack.mulPose(matrix);
        Vec3 scopeOrigin = scope.origin();
        Vec3 gunOrigin = GunSights.GUN_ORIGIN;
        Vec3 mount = GunSights.scopePosition(frame.gun()).subtract(gunOrigin);
        poseStack.translate(-scopeOrigin.x * 0.0625, -scopeOrigin.y * 0.0625, -scopeOrigin.z * 0.0625);
        poseStack.translate(gunOrigin.x * 0.0625, gunOrigin.y * 0.0625, gunOrigin.z * 0.0625);
        poseStack.translate(mount.x * 0.0625, mount.y * 0.0625, mount.z * 0.0625);
        Vec3 center = scopeOrigin.subtract(8.0, 8.0, 8.0).scale(0.0625);
        float scale = (float) GunSights.scopeScale(frame.gun());
        poseStack.translate(center.x, center.y, center.z);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-center.x, -center.y, -center.z);
        collector.submitCustomGeometry(
            poseStack,
            RenderTypes.entityTranslucent(SCOPE_TEXTURE),
            (pose, consumer) -> scopeModel.draw(pose, consumer, light, overlay, -1)
        );
        poseStack.popPose();
    }
}
