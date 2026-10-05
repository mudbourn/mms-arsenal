package info.mudbourn.mmsarsenal.client.gun;

import info.mudbourn.mmsarsenal.gun.Gun;
import info.mudbourn.mmsarsenal.gun.GunItem;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

// Recoil: the camera kick spread over frames after a shot, and the gun model's kick read from the fire-rate cooldown.
public final class RecoilClient {

    private static final RecoilClient INSTANCE = new RecoilClient();

    private final Random random = new Random();
    private double gunRecoilNormal;
    private float gunRecoilRandom;
    private float cameraRecoil;
    private float progressCameraRecoil;
    private int recoilDirection;

    public static RecoilClient get() {
        return INSTANCE;
    }

    public void onFire(ItemStack held, Gun gun) {
        this.recoilDirection = new Random().nextInt(2);
        this.cameraRecoil = gun.general().recoilAngle() * (float) this.adsRecoilReduction(gun);
        this.progressCameraRecoil = 0.0F;
        this.gunRecoilRandom = this.random.nextFloat();
    }

    // Applies this frame's share of the camera kick: up quickly over the first fifth, then settling back down.
    public void onRenderFrame(float frameDelta) {
        if (this.cameraRecoil <= 0.0F) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        float amount = this.cameraRecoil * frameDelta * 0.15F;
        float startProgress = this.progressCameraRecoil / this.cameraRecoil;
        float endProgress = (this.progressCameraRecoil + amount) / this.cameraRecoil;
        float pitch = player.getXRot();
        float yaw = player.getYRot();
        float sideways = this.recoilDirection == 1 ? -1.0F : 1.0F;
        if (startProgress < 0.2F) {
            float step = (endProgress - startProgress) / 0.2F * this.cameraRecoil;
            player.setXRot(pitch - step);
            player.setYRot(yaw + sideways * step / 2.0F);
        } else {
            float step = (endProgress - startProgress) / 0.8F * this.cameraRecoil;
            player.setXRot(pitch + step);
            player.setYRot(yaw - sideways * step / 2.0F);
        }
        this.progressCameraRecoil += amount;
        if (this.progressCameraRecoil >= this.cameraRecoil) {
            this.cameraRecoil = 0.0F;
            this.progressCameraRecoil = 0.0F;
        }
    }

    // Reads the gun's kick for this frame from how far through its fire-rate cooldown it is.
    public void updateGunRecoil(LocalPlayer player, float partialTick) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof GunItem gunItem)) {
            return;
        }
        Gun gun = gunItem.getGun(true);
        float cooldown = player.getCooldowns().getCooldownPercent(held, partialTick);
        float offset = gun.general().recoilDurationOffset();
        cooldown = cooldown >= offset ? (cooldown - offset) / (1.0F - offset) : 0.0F;
        if (cooldown >= 0.8F) {
            float amount = (1.0F - cooldown) / 0.2F;
            amount--;
            this.gunRecoilNormal = 1.0F - amount * amount * amount * amount;
        } else {
            float amount = cooldown / 0.8F;
            this.gunRecoilNormal = amount < 0.5F ? 2.0F * amount * amount : -1.0F + (4.0F - 2.0F * amount) * amount;
        }
    }

    public double adsRecoilReduction(Gun gun) {
        return 1.0 - gun.general().recoilAdsReduction() * AimHandler.get().normalisedProgress();
    }

    public double gunRecoilNormal() {
        return this.gunRecoilNormal;
    }

    public float gunRecoilRandom() {
        return this.gunRecoilRandom;
    }
}
