package info.mudbourn.mmsarsenal.client.gun.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import info.mudbourn.mmsarsenal.MmsArsenal;
import info.mudbourn.mmsarsenal.client.gun.AimHandler;
import info.mudbourn.mmsarsenal.client.gun.GunAnimationDriver;
import info.mudbourn.mmsarsenal.client.gun.GunSights;
import info.mudbourn.mmsarsenal.gun.Gun;
import info.mudbourn.mmsarsenal.gun.GunComponents;
import info.mudbourn.mmsarsenal.gun.GunItem;
import info.mudbourn.mmsrendercommon.client.geo.BonePose;
import info.mudbourn.mmsrendercommon.client.geo.GeoAssets;
import info.mudbourn.mmsrendercommon.client.geo.GeoModel;
import info.mudbourn.mmsrendercommon.client.geo.GeoTexture;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3fc;

// The mms_arsenal:gun item model: a gun's Bedrock geo, animated in the local player's first-person hand and idle everywhere else.
public final class GunItemModel implements ItemModel {

    public static final Identifier TYPE = MmsArsenal.id("gun");

    // The item definition's fields: the block model for display transforms, the geo, its texture and its animations.
    public record Unbaked(Identifier base, Identifier geo, GeoTexture texture, Identifier animations) implements ItemModel.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("base").forGetter(Unbaked::base),
            Identifier.CODEC.fieldOf("geo").forGetter(Unbaked::geo),
            GeoTexture.CODEC.fieldOf("texture").forGetter(Unbaked::texture),
            Identifier.CODEC.fieldOf("animations").forGetter(Unbaked::animations)
        ).apply(instance, Unbaked::new));

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(this.base);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context) {
            ModelBaker baker = context.blockModelBaker();
            ResolvedModel resolved = baker.getModel(this.base);
            ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(baker, resolved, resolved.getTopTextureSlots());
            return new GunItemModel(this, properties);
        }
    }

    private static final SpecialModelRenderer<GunFrame> RENDERER = new SpecialModelRenderer<>() {

        @Override
        public void submit(GunFrame frame, ItemDisplayContext context, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean glint, int outline) {
            if (frame != null) {
                GunRenderer.submit(frame, poseStack, collector, light, overlay);
            }
        }

        @Override
        public void getExtents(Consumer<Vector3fc> output) {
        }

        @Override
        public GunFrame extractArgument(ItemStack stack) {
            return null;
        }
    };

    private final Unbaked source;
    private final ModelRenderProperties properties;

    private GunItemModel(Unbaked source, ModelRenderProperties properties) {
        this.source = source;
        this.properties = properties;
    }

    @Override
    public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver, ItemDisplayContext context, ClientLevel level, ItemOwner owner, int seed) {
        GeoModel model = GeoAssets.model(this.source.geo());
        if (model == null || !(stack.getItem() instanceof GunItem gunItem)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        boolean firstPerson = context.firstPerson() && player != null && owner == player;
        if (firstPerson && player.getOffhandItem() == stack) {
            return;
        }
        state.appendModelIdentityElement(this);
        state.setAnimated();
        ItemStackRenderState.LayerRenderState layer = state.newLayer();
        double nowTicks = System.nanoTime() / 50_000_000.0;
        Map<String, BonePose> poses = firstPerson
            ? GunAnimationDriver.get().pose(player, stack, gunItem, this.source.animations(), nowTicks)
            : GunAnimationDriver.idlePose(this.source.animations(), nowTicks);
        long ticks = level != null ? level.getGameTime() : 0L;
        RenderType renderType = RenderTypes.entityCutoutNoCull(this.source.texture().at(ticks));
        Gun gun = gunItem.getGun(true);
        GunFrame frame = new GunFrame(
            model,
            renderType,
            poses,
            firstPerson,
            stack.copy(),
            gun,
            GunComponents.ammo(stack),
            AimHandler.get().isAiming(),
            GunSights.scope(stack),
            this.properties.transforms().firstPersonRightHand(),
            context
        );
        layer.setupSpecialModel(RENDERER, frame);
        this.properties.applyToLayer(layer, context);
    }
}
