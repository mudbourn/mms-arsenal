package info.mudbourn.mmsarsenal.client.gun;

import info.mudbourn.mmsarsenal.gun.GunMobEffects;
import info.mudbourn.mmsarsenal.gun.GunSounds;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;

// Client side of flashbangs: a ringing that fades with the deafened effect and a white-out that fades with the blinded one.
public final class StunEffectsClient {

    private static final float MAX_VOLUME = 0.85F;
    private static final int FADE_TICKS = 80;
    private static final int WHITE_FADE_TICKS = 30;

    private static StunRingingSound ringing;

    private StunEffectsClient() {
    }

    // Starts the ringing while the local player is deafened.
    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || !player.hasEffect(GunMobEffects.DEAFENED)) {
            ringing = null;
            return;
        }
        if (ringing == null || !mc.getSoundManager().isActive(ringing)) {
            ringing = new StunRingingSound();
            mc.getSoundManager().play(ringing);
        }
    }

    // Fills the screen white while the local player is blinded.
    public static void renderBlindness(GuiGraphics graphics, DeltaTracker delta) {
        LocalPlayer player = Minecraft.getInstance().player;
        MobEffectInstance effect = player == null ? null : player.getEffect(GunMobEffects.BLINDED);
        if (effect == null) {
            return;
        }
        int alpha = (int) (255.0F * Math.min(1.0F, effect.getDuration() / (float) WHITE_FADE_TICKS));
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), (alpha << 24) | 0xFFFFFF);
    }

    // A looping, non-positional whine that follows the player and dies with the deafened effect.
    private static final class StunRingingSound extends AbstractTickableSoundInstance {

        StunRingingSound() {
            super(GunSounds.STUN_GRENADE_RING, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.delay = 0;
        }

        @Override
        public void tick() {
            LocalPlayer player = Minecraft.getInstance().player;
            MobEffectInstance effect = player == null ? null : player.getEffect(GunMobEffects.DEAFENED);
            if (effect == null || !player.isAlive()) {
                this.stop();
                return;
            }
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
            this.volume = MAX_VOLUME * Math.min(1.0F, effect.getDuration() / (float) FADE_TICKS);
            this.pitch = 0.95F;
        }
    }
}
