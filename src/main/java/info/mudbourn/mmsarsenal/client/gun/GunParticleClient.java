package info.mudbourn.mmsarsenal.client.gun;

import info.mudbourn.mmsarsenal.gun.GunParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ExplodeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;

// Client particles: casings, scrap, sparks, burning round trails, ghost glints, sonic rings, explosions, smoke and bullet holes.
public final class GunParticleClient {

    private GunParticleClient() {
    }

    public static void register() {
        ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();
        registry.register(GunParticles.CASING, sprites -> simple(sprites, Casing::new));
        registry.register(GunParticles.SPECTRE_CASING, sprites -> simple(sprites, Casing::new));
        registry.register(GunParticles.SCRAP, sprites -> simple(sprites, Scrap::new));
        registry.register(GunParticles.SPARK, sprites -> (type, level, x, y, z, dx, dy, dz, random) -> {
            Spark spark = new Spark(level, x, y, z, sprites.get(random));
            spark.setColor(1.0F, 1.0F, 0.5F);
            return spark;
        });
        registry.register(GunParticles.SOUL_LAVA, sprites -> simple(sprites, Lava::new));
        registry.register(GunParticles.FIRE, sprites -> simple(sprites, Fire::new));
        registry.register(GunParticles.GHOST_GLINT, sprites -> (type, level, x, y, z, dx, dy, dz, random) -> {
            Glint glint = new Glint(level, x, y, z, dx, dy, dz, sprites.get(random));
            glint.setLifetime(5 + level.getRandom().nextInt(5));
            return glint;
        });
        registry.register(GunParticles.SONIC_RING, sprites -> (type, level, x, y, z, dx, dy, dz, random) ->
            new SonicRing(level, x, y, z, dx, dy, dz, sprites.get(random)).scale(3.0F));
        registry.register(GunParticles.BIG_SONIC_RING, sprites -> (type, level, x, y, z, dx, dy, dz, random) ->
            new SonicRing(level, x, y, z, dx, dy, dz, sprites.get(random)).scale(17.0F));
        registry.register(GunParticles.BIG_EXPLOSION, sprites -> (type, level, x, y, z, dx, dy, dz, random) ->
            new Explosion(level, x, y, z, dx, dy, dz, sprites, 7.5F));
        registry.register(GunParticles.SMALL_EXPLOSION, sprites -> (type, level, x, y, z, dx, dy, dz, random) ->
            new Explosion(level, x, y, z, dx, dy, dz, sprites, 2.0F));
        registry.register(GunParticles.SMOKE, sprites -> (type, level, x, y, z, dx, dy, dz, random) ->
            new Smoke(level, x, y, z, dx, dy, dz, sprites.get(random)));
    }

    // Adds a bullet hole decal on the face a projectile struck, tinted and textured like the block.
    public static void addBulletHole(ClientLevel level, double x, double y, double z, Direction face, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getBlockModelShaper().getParticleIcon(state);
        Minecraft.getInstance().particleEngine.add(new BulletHole(level, x, y, z, face, pos, sprite));
    }

    private interface QuadFactory {
        SingleQuadParticle create(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite);
    }

    private static ParticleProvider<SimpleParticleType> simple(SpriteSet sprites, QuadFactory factory) {
        return (type, level, x, y, z, dx, dy, dz, random) -> factory.create(level, x, y, z, sprites.get(random));
    }

    // A spent casing: hops up, falls and bounces, shrinking away.
    private static final class Casing extends SingleQuadParticle {

        Casing(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
            super(level, x, y, z, 0.0, 0.0, 0.0, sprite);
            this.gravity = 0.75F;
            this.friction = 0.999F;
            this.hasPhysics = true;
            this.xd *= 0.8F;
            this.zd *= 0.8F;
            this.yd = this.random.nextFloat() * 0.225F + 0.22F;
            this.quadSize = 0.35F;
            this.lifetime = (int) (16.0 / (Math.random() * 0.8 + 0.2));
        }

        @Override
        protected Layer getLayer() {
            return Layer.OPAQUE;
        }

        @Override
        public float getQuadSize(float partialTick) {
            float progress = (this.age + partialTick) / this.lifetime;
            return this.quadSize * (1.0F - progress * progress);
        }
    }

    // Bits of a worn-out gun: small casings of scrap metal.
    private static final class Scrap extends SingleQuadParticle {

        Scrap(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
            super(level, x, y, z, 0.0, 0.0, 0.0, sprite);
            this.gravity = 0.75F;
            this.friction = 0.999F;
            this.hasPhysics = true;
            this.xd *= 0.8F;
            this.zd *= 0.8F;
            this.yd = this.random.nextFloat() * 0.225F + 0.22F;
            this.quadSize = 0.1F;
            this.lifetime = (int) (16.0 / (Math.random() * 0.8 + 0.2));
        }

        @Override
        protected Layer getLayer() {
            return Layer.OPAQUE;
        }

        @Override
        public float getQuadSize(float partialTick) {
            float progress = (this.age + partialTick) / this.lifetime;
            return this.quadSize * (1.0F - progress * progress);
        }
    }

    // A bright spark off metal or stone.
    private static final class Spark extends SingleQuadParticle {

        Spark(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
            super(level, x, y, z, 0.0, 0.0, 0.0, sprite);
            this.gravity = 0.75F;
            this.friction = 0.999F;
            this.xd *= 0.8F;
            this.zd *= 0.8F;
            this.yd = this.random.nextFloat() * 0.4F + 0.05F;
            this.quadSize *= this.random.nextFloat();
            this.lifetime = (int) (16.0 / (Math.random() * 0.8 + 0.2));
        }

        @Override
        protected Layer getLayer() {
            return Layer.OPAQUE;
        }

        @Override
        public int getLightColor(float partialTick) {
            return 240 | (super.getLightColor(partialTick) >> 16 & 0xFF) << 16;
        }

        @Override
        public float getQuadSize(float partialTick) {
            float progress = (this.age + partialTick) / this.lifetime;
            return this.quadSize * (1.0F - progress * progress);
        }
    }

    // A glowing ember that trails smoke, like lava's.
    private static class Lava extends SingleQuadParticle {

        Lava(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
            this(level, x, y, z, sprite, 2.0F);
        }

        Lava(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite, float sizeSpread) {
            super(level, x, y, z, 0.0, 0.0, 0.0, sprite);
            this.gravity = 0.75F;
            this.friction = 0.999F;
            this.xd *= 0.8F;
            this.zd *= 0.8F;
            this.yd = this.random.nextFloat() * 0.4F + 0.05F;
            this.quadSize *= this.random.nextFloat() * sizeSpread + 0.2F;
            this.lifetime = (int) (16.0 / (Math.random() * 0.8 + 0.2));
        }

        @Override
        protected Layer getLayer() {
            return Layer.OPAQUE;
        }

        @Override
        public int getLightColor(float partialTick) {
            return 240 | (super.getLightColor(partialTick) >> 16 & 0xFF) << 16;
        }

        @Override
        public float getQuadSize(float partialTick) {
            float progress = (this.age + partialTick) / this.lifetime;
            return this.quadSize * (1.0F - progress * progress);
        }

        @Override
        public void tick() {
            super.tick();
            if (!this.removed && this.random.nextFloat() > (float) this.age / this.lifetime) {
                this.level.addParticle(ParticleTypes.SMOKE, this.x, this.y, this.z, this.xd, this.yd, this.zd);
            }
        }
    }

    // A large burning ember for rocket trails and blasts.
    private static final class Fire extends Lava {

        Fire(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
            super(level, x, y, z, sprite, 10.0F);
        }
    }

    // A small full-bright glint that drifts and slows.
    private static final class Glint extends SingleQuadParticle {

        Glint(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, TextureAtlasSprite sprite) {
            super(level, x, y, z, dx, dy, dz, sprite);
            this.setSize(0.02F, 0.02F);
            this.quadSize *= this.random.nextFloat() * 0.6F + 0.5F;
            this.xd *= 0.02F;
            this.yd *= 0.02F;
            this.zd *= 0.02F;
            this.lifetime = (int) (10.0 / (Math.random() * 0.8 + 0.2));
        }

        @Override
        protected Layer getLayer() {
            return Layer.OPAQUE;
        }

        @Override
        public int getLightColor(float partialTick) {
            return 240;
        }

        @Override
        public void move(double dx, double dy, double dz) {
            this.setBoundingBox(this.getBoundingBox().move(dx, dy, dz));
            this.setLocationFromBoundingbox();
        }

        @Override
        public void tick() {
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
            if (this.lifetime-- <= 0) {
                this.remove();
            } else {
                this.move(this.xd, this.yd, this.zd);
                this.xd *= 0.99;
                this.yd *= 0.99;
                this.zd *= 0.99;
            }
        }
    }

    // An expanding, fading ring of sound.
    private static final class SonicRing extends SingleQuadParticle {

        SonicRing(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, TextureAtlasSprite sprite) {
            super(level, x, y, z, dx, dy, dz, sprite);
            this.friction = 0.96F;
            this.xd = this.xd * 0.01F + dx;
            this.yd = this.yd * 0.01F + dy;
            this.zd = this.zd * 0.01F + dz;
            this.x += (this.random.nextFloat() - this.random.nextFloat()) * 0.05F;
            this.y += (this.random.nextFloat() - this.random.nextFloat()) * 0.05F;
            this.z += (this.random.nextFloat() - this.random.nextFloat()) * 0.05F;
            this.lifetime = (int) (8.0 / (this.random.nextFloat() * 0.8 + 0.2)) + 4;
            this.hasPhysics = false;
            this.alpha = 0.75F;
        }

        @Override
        protected Layer getLayer() {
            return Layer.TRANSLUCENT;
        }

        @Override
        public void tick() {
            super.tick();
            if (this.age <= 10) {
                this.setAlpha(0.75F * (1.0F - this.age / 10.0F));
            }
        }

        @Override
        public float getQuadSize(float partialTick) {
            float progress = (this.age + partialTick) / this.lifetime;
            return this.quadSize * (1.0F + progress * progress);
        }

        @Override
        public int getLightColor(float partialTick) {
            return 240;
        }
    }

    // An animated explosion burst at a set size.
    private static final class Explosion extends ExplodeParticle {

        Explosion(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites, float size) {
            super(level, x, y, z, dx, dy, dz, sprites);
            this.quadSize = size;
            this.lifetime = 16;
        }

        @Override
        public int getLightColor(float partialTick) {
            return 240;
        }
    }

    // Rocket smoke: billows out large, fading over three seconds.
    private static final class Smoke extends SingleQuadParticle {

        Smoke(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, TextureAtlasSprite sprite) {
            super(level, x, y, z, dx, dy, dz, sprite);
            this.friction = 0.8F;
            this.xd = dx;
            this.yd = dy;
            this.zd = dz;
            this.quadSize *= this.random.nextFloat() + 0.2F;
            this.lifetime = 60;
        }

        @Override
        public void tick() {
            super.tick();
            this.alpha = -(1.0F / this.lifetime) * this.age + 1.0F;
        }

        @Override
        protected Layer getLayer() {
            return Layer.TRANSLUCENT;
        }

        @Override
        public int getLightColor(float partialTick) {
            float progress = Mth.clamp((this.age + partialTick) / this.lifetime, 0.0F, 1.0F);
            int light = super.getLightColor(partialTick);
            int block = Math.max(240, (light & 0xFF) - (int) (progress * 15.0F * 16.0F));
            return block | (light >> 16 & 0xFF) << 16;
        }

        @Override
        public float getQuadSize(float partialTick) {
            float progress = (this.age + partialTick) / this.lifetime;
            return this.quadSize * ((1.0F + progress * progress) * 30.0F);
        }
    }

    // A bullet hole: a darkened scrap of the block's own texture laid flat on the struck face, fading near the end of its life.
    private static final class BulletHole extends SingleQuadParticle {

        private static final int LIFE_MIN = 150;
        private static final int LIFE_MAX = 200;
        private static final float FADE_THRESHOLD = 0.98F;

        private final Direction face;
        private final BlockPos pos;
        private final int uOffset;
        private final int vOffset;
        private final float density;

        BulletHole(ClientLevel level, double x, double y, double z, Direction face, BlockPos pos, TextureAtlasSprite sprite) {
            super(level, x, y, z, sprite);
            this.face = face;
            this.pos = pos;
            this.uOffset = this.random.nextInt(16);
            this.vOffset = this.random.nextInt(16);
            this.density = (sprite.getU1() - sprite.getU0()) / 16.0F;
            this.lifetime = (int) (LIFE_MIN + level.getRandom().nextFloat() * (LIFE_MAX - LIFE_MIN));
            this.hasPhysics = false;
            this.gravity = 0.0F;
            this.quadSize = 0.05F;
            BlockState state = level.getBlockState(pos);
            int color = state.is(Blocks.GRASS_BLOCK) ? Integer.MAX_VALUE : Minecraft.getInstance().getBlockColors().getColor(state, level, pos, 0);
            this.rCol = (color >> 16 & 0xFF) / 255.0F / 3.0F;
            this.gCol = (color >> 8 & 0xFF) / 255.0F / 3.0F;
            this.bCol = (color & 0xFF) / 255.0F / 3.0F;
            this.alpha = 0.9F;
        }

        @Override
        protected Layer getLayer() {
            return Layer.TERRAIN;
        }

        @Override
        protected float getU0() {
            return this.sprite.getU0() + this.uOffset * this.density;
        }

        @Override
        protected float getV0() {
            return this.sprite.getV0() + this.vOffset * this.density;
        }

        @Override
        protected float getU1() {
            return this.getU0() + this.density;
        }

        @Override
        protected float getV1() {
            return this.getV0() + this.density;
        }

        @Override
        public void tick() {
            super.tick();
            if (this.level.getBlockState(this.pos).isAir()) {
                this.remove();
            }
            float fadeStart = this.lifetime * FADE_THRESHOLD;
            this.alpha = 0.9F * (1.0F - Math.max(this.age - fadeStart, 0.0F) / (this.lifetime - fadeStart));
        }

        @Override
        public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
            Quaternionf rotation = new Quaternionf(this.face.getRotation()).rotateX(-Mth.HALF_PI);
            this.extractRotatedQuad(state, camera, rotation, partialTick);
        }
    }
}
