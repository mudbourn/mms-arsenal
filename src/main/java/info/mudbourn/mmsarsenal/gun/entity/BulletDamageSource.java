package info.mudbourn.mmsarsenal.gun.entity;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

// Bullet damage, whose death message is picked at random from a handful of verbs.
public class BulletDamageSource extends DamageSource {

    private static final String[] MESSAGES = {
        "mms_arsenal.bullet.killed",
        "mms_arsenal.bullet.eliminated",
        "mms_arsenal.bullet.executed",
        "mms_arsenal.bullet.annihilated",
        "mms_arsenal.bullet.decimated"
    };

    public BulletDamageSource(Holder<DamageType> type, Entity directEntity, Entity causingEntity) {
        super(type, directEntity, causingEntity);
    }

    @Override
    public String getMsgId() {
        return MESSAGES[ThreadLocalRandom.current().nextInt(MESSAGES.length)];
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity victim) {
        String key = "death.attack." + this.getMsgId();
        if (this.getEntity() == null && this.getDirectEntity() == null) {
            LivingEntity credit = victim.getKillCredit();
            return credit != null
                ? Component.translatable(key + ".player", victim.getDisplayName(), credit.getDisplayName())
                : Component.translatable(key, victim.getDisplayName());
        }
        Component killer = this.getEntity() == null ? this.getDirectEntity().getDisplayName() : this.getEntity().getDisplayName();
        ItemStack weapon = this.getEntity() instanceof LivingEntity living ? living.getMainHandItem() : ItemStack.EMPTY;
        return !weapon.isEmpty() && weapon.has(DataComponents.CUSTOM_NAME)
            ? Component.translatable(key + ".item", victim.getDisplayName(), killer, weapon.getDisplayName())
            : Component.translatable(key, victim.getDisplayName(), killer);
    }
}
