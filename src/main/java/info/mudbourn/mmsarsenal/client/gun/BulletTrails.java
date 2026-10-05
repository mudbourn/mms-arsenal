package info.mudbourn.mmsarsenal.client.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import info.mudbourn.mmsarsenal.gun.net.BulletTrailPayload;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

// Bullet trails simulated on the client from each shot's start, drawn as glowing streaks that lengthen as they fly.
public final class BulletTrails {

    private static final BulletTrails INSTANCE = new BulletTrails();
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/misc/white.png");

    private final Map<Integer, Trail> trails = new HashMap<>();

    public static BulletTrails get() {
        return INSTANCE;
    }

    public void add(BulletTrailPayload payload) {
        for (BulletTrailPayload.Trail trail : payload.trails()) {
            this.trails.put(trail.entityId(), new Trail(
                trail.position(),
                trail.motion(),
                payload.trailColor(),
                payload.life(),
                payload.gravity(),
                payload.shooterId()
            ));
        }
    }

    public void remove(int entityId) {
        this.trails.remove(entityId);
    }

    public void clear() {
        this.trails.clear();
    }

    public void tick(Minecraft mc) {
        if (mc.level == null) {
            this.trails.clear();
            return;
        }
        this.trails.values().forEach(trail -> trail.tick(mc));
        this.trails.values().removeIf(trail -> trail.dead);
    }

    public void render(PoseStack poseStack, MultiBufferSource buffers, Vec3 camera, float partialTick) {
        if (this.trails.isEmpty()) {
            return;
        }
        VertexConsumer consumer = buffers.getBuffer(RenderTypes.energySwirl(TEXTURE, 0.0F, 0.15625F));
        for (Trail trail : this.trails.values()) {
            trail.render(poseStack, consumer, camera, partialTick);
        }
    }

    // One projectile's trail: moves by its velocity with gravity and grows up to a fixed length.
    private static final class Trail {

        private Vec3 position;
        private Vec3 motion;
        private final int color;
        private final int maxAge;
        private final double gravity;
        private final int shooterId;
        private float yaw;
        private float pitch;
        private int age;
        private boolean dead;

        Trail(Vec3 position, Vec3 motion, int color, int maxAge, double gravity, int shooterId) {
            this.position = position;
            this.motion = motion;
            this.color = color;
            this.maxAge = maxAge;
            this.gravity = gravity;
            this.shooterId = shooterId;
            this.updateYawPitch();
        }

        private void updateYawPitch() {
            float horizontal = Mth.sqrt((float) (this.motion.x * this.motion.x + this.motion.z * this.motion.z));
            this.yaw = (float) Math.toDegrees(Mth.atan2(this.motion.x, this.motion.z));
            this.pitch = (float) Math.toDegrees(Mth.atan2(this.motion.y, horizontal));
        }

        void tick(Minecraft mc) {
            this.age++;
            this.position = this.position.add(this.motion);
            if (this.gravity != 0.0) {
                this.motion = this.motion.add(0.0, this.gravity, 0.0);
                this.updateYawPitch();
            }
            Entity shooter = mc.level.getEntity(this.shooterId);
            if (shooter != null && shooter == mc.player) {
                mc.level.addAlwaysVisibleParticle(ParticleTypes.MYCELIUM, true, this.position.x, this.position.y, this.position.z, this.motion.x, this.motion.y, this.motion.z);
            }
            Entity camera = mc.getCameraEntity();
            double distance = camera != null ? Math.sqrt(camera.distanceToSqr(this.position)) : Double.MAX_VALUE;
            if (this.age >= this.maxAge || distance > 256.0) {
                this.dead = true;
            }
        }

        void render(PoseStack poseStack, VertexConsumer consumer, Vec3 camera, float partialTick) {
            if (this.dead) {
                return;
            }
            poseStack.pushPose();
            poseStack.translate(
                this.position.x + this.motion.x * partialTick - camera.x,
                this.position.y + this.motion.y * partialTick - camera.y,
                this.position.z + this.motion.z * partialTick - camera.z
            );
            poseStack.mulPose(Axis.YP.rotationDegrees(this.yaw - 90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(this.pitch));
            poseStack.mulPose(Axis.XP.rotationDegrees(45.0F));
            poseStack.scale(0.05625F, 0.05625F, 0.05625F);
            poseStack.translate(-4.0F, 0.0F, 0.0F);
            int size = Math.min((this.age + 1) * 30, 200);
            int red = this.color >> 16 & 0xFF;
            int green = this.color >> 8 & 0xFF;
            int blue = this.color & 0xFF;
            PoseStack.Pose pose = poseStack.last();
            vertex(consumer, pose, -1 - size, -1, -1, 0.0F, 0.15625F, -1, 0, 0, red, green, blue);
            vertex(consumer, pose, -1 - size, -1, 1, 0.15625F, 0.15625F, -1, 0, 0, red, green, blue);
            vertex(consumer, pose, -1 - size, 1, 1, 0.15625F, 0.3125F, -1, 0, 0, red, green, blue);
            vertex(consumer, pose, -1 - size, 1, -1, 0.0F, 0.3125F, -1, 0, 0, red, green, blue);
            vertex(consumer, pose, 1, 1, -1, 0.0F, 0.15625F, 1, 0, 0, red, green, blue);
            vertex(consumer, pose, 1, 1, 1, 0.15625F, 0.15625F, 1, 0, 0, red, green, blue);
            vertex(consumer, pose, 1, -1, 1, 0.15625F, 0.3125F, 1, 0, 0, red, green, blue);
            vertex(consumer, pose, 1, -1, -1, 0.0F, 0.3125F, 1, 0, 0, red, green, blue);
            for (int side = 0; side < 4; side++) {
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                vertex(consumer, pose, -1 - size, -1, 1, 0.0F, 0.0F, 0, 1, 0, red, green, blue);
                vertex(consumer, pose, 1, -1, 1, 0.5F, 0.0F, 0, 1, 0, red, green, blue);
                vertex(consumer, pose, 1, 1, 1, 0.5F, 0.15625F, 0, 1, 0, red, green, blue);
                vertex(consumer, pose, -1 - size, 1, 1, 0.0F, 0.15625F, 0, 1, 0, red, green, blue);
            }
            poseStack.popPose();
        }

        private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int x, int y, int z, float u, float v, int nx, int nz, int ny, int red, int green, int blue) {
            consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(15728880)
                .setNormal(pose, nx, ny, nz);
        }
    }
}
