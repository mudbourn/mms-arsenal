package info.mudbourn.mmsarsenal.armory.item;

import info.mudbourn.mmsarsenal.armory.ArmorySounds;
import info.mudbourn.mmsarsenal.armory.WeaponStats;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Long blade with 5 block reach that builds focus like the Dragon Slayer and releases an 8 block lunging pierce.
public class OdachiItem extends DragonSlayerItem {

    public static final double CHARGED_RANGE = 8.0;
    private static final String PIERCE_ANIMATION = "bettercombat:two_handed_stab_right";
    private static final double LUNGE_SPEED = 1.4;
    private static final double LUNGE_LIFT = 0.15;

    public OdachiItem(Properties properties) {
        super(new WeaponStats(9.0, -3.1, 0.0, 2.0), properties);
    }

    @Override
    public double chargedRange(Player player) {
        return CHARGED_RANGE;
    }

    @Override
    public String chargedAnimation() {
        return PIERCE_ANIMATION;
    }

    @Override
    public boolean chargedPierce() {
        return true;
    }

    @Override
    protected SoundEvent chargedSound() {
        return ArmorySounds.ODACHI_PIERCE;
    }

    // Throws the wielder forward along their facing as the pierce is released.
    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        boolean released = super.releaseUsing(stack, level, entity, timeLeft);
        if (released && level instanceof ServerLevel && entity instanceof Player player) {
            Vec3 look = player.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0.0, look.z).normalize().scale(LUNGE_SPEED);
            player.push(flat.x, LUNGE_LIFT, flat.z);
            player.hurtMarked = true;
        }
        return released;
    }
}
