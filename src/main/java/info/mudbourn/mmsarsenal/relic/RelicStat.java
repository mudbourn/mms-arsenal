package info.mudbourn.mmsarsenal.relic;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

// One attribute line a relic grants while worn in a trinket slot.
public record RelicStat(Identifier attribute, double amount, Operation operation) {

    public static RelicStat health(double amount) {
        return flat("max_health", amount);
    }

    public static RelicStat armor(double amount) {
        return flat("armor", amount);
    }

    public static RelicStat toughness(double amount) {
        return flat("armor_toughness", amount);
    }

    public static RelicStat luck(double amount) {
        return flat("luck", amount);
    }

    public static RelicStat knockbackResistance(double amount) {
        return flat("knockback_resistance", amount);
    }

    public static RelicStat swim(double amount) {
        return flat("water_movement_efficiency", amount);
    }

    public static RelicStat speed(double fraction) {
        return percent("movement_speed", fraction);
    }

    public static RelicStat attackSpeed(double fraction) {
        return percent("attack_speed", fraction);
    }

    public static RelicStat attackDamage(double fraction) {
        return percent("attack_damage", fraction);
    }

    public static RelicStat spellPower(double fraction) {
        return new RelicStat(Identifier.fromNamespaceAndPath("spell_power", "generic"), fraction, Operation.ADD_MULTIPLIED_BASE);
    }

    public static RelicStat rangedDamage(double fraction) {
        return new RelicStat(Identifier.fromNamespaceAndPath("ranged_weapon", "damage"), fraction, Operation.ADD_MULTIPLIED_BASE);
    }

    public AttributeModifier modifier(Identifier id) {
        return new AttributeModifier(id, amount, operation);
    }

    private static RelicStat flat(String path, double amount) {
        return new RelicStat(Identifier.withDefaultNamespace(path), amount, Operation.ADD_VALUE);
    }

    private static RelicStat percent(String path, double fraction) {
        return new RelicStat(Identifier.withDefaultNamespace(path), fraction, Operation.ADD_MULTIPLIED_BASE);
    }
}
