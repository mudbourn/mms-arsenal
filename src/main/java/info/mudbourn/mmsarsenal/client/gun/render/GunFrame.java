package info.mudbourn.mmsarsenal.client.gun.render;

import info.mudbourn.mmsarsenal.client.gun.GunSights;
import info.mudbourn.mmsarsenal.gun.Gun;
import info.mudbourn.mmsrendercommon.client.geo.BonePose;
import info.mudbourn.mmsrendercommon.client.geo.GeoModel;
import java.util.Map;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

// What one gun draws in one frame: its posed model, state for bone visibility, and the transforms its first-person view uses.
public record GunFrame(
    GeoModel model,
    RenderType renderType,
    Map<String, BonePose> poses,
    boolean firstPerson,
    ItemStack stack,
    Gun gun,
    int ammo,
    boolean aiming,
    GunSights.Scope scope,
    ItemTransform firstPersonTransform,
    ItemDisplayContext context
) {
}
