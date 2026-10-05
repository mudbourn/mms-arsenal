package info.mudbourn.mmsarsenal.client.gun;

import info.mudbourn.mmsarsenal.config.ArsenalConfig;
import info.mudbourn.mmsarsenal.gun.DrawTracker;
import info.mudbourn.mmsarsenal.gun.FireMode;
import info.mudbourn.mmsarsenal.gun.Gun;
import info.mudbourn.mmsarsenal.gun.GunEffects;
import info.mudbourn.mmsarsenal.gun.GunParticles;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

// The client's half of a shot: the same checks the server makes, then recoil, pushback, flash and animation.
public final class GunFireClient {

    private GunFireClient() {
    }

    // Whether the local player may fire now; a gun about to break plays its snap and cools down instead.
    public static boolean canFire(LocalPlayer player, ItemStack held, Gun gun) {
        if (!gun.general().canFireUnderwater() && player.isUnderWater() && !ArsenalConfig.get().underwaterFiring) {
            return false;
        }
        if (player.getCooldowns().isOnCooldown(held) && gun.general().fireMode() == FireMode.PULSE) {
            return false;
        }
        if (DrawTracker.get(player).isDrawing(player, true)) {
            return false;
        }
        if (held.isDamageableItem() && held.getDamageValue() >= held.getMaxDamage() - 1) {
            player.level().playSound(player, player.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            brokenGunParticles(player);
            player.getCooldowns().addCooldown(held, gun.general().rate());
            return false;
        }
        return true;
    }

    // What follows a shot on the client: the shot animation, recoil, pushback, muzzle flash and the worn-gun warning.
    public static void afterFire(LocalPlayer player, ItemStack held, Gun gun) {
        GunAnimationDriver.get().onFire(held);
        RecoilClient.get().onFire(held, gun);
        GunView.get().onFire(player, gun);
        CrosshairClient.get().onFire();
        if (held.isDamageableItem() && held.getDamageValue() >= held.getMaxDamage() / 1.5) {
            float pitch = 1.5F + player.getRandom().nextFloat() * 0.25F;
            player.level().playSound(player, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.3F, pitch);
        }
        float pushback = gun.general().shooterPushback();
        if (player.isCrouching() && player.level().getBlockState(player.getOnPos()).isSolid()) {
            pushback /= 2.0F;
        }
        GunEffects.push(player, pushback);
    }

    // Bits of scrap off the side of a gun that has worn out.
    private static void brokenGunParticles(LocalPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0.0, look.x).normalize();
        Vec3 forward = new Vec3(look.x, 0.0, look.z).normalize();
        double divisor = AimHandler.get().isZooming() ? 0.4 : 0.5;
        Vec3 pos = player.position().add(right.x * divisor + forward.x * divisor, player.getEyeHeight() - 0.4, right.z * divisor + forward.z * divisor);
        player.level().addParticle(GunParticles.SCRAP, pos.x, pos.y, pos.z, 1.0, 1.0, 1.0);
    }
}
