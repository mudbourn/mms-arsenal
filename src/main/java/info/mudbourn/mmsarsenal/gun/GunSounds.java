package info.mudbourn.mmsarsenal.gun;

import info.mudbourn.mmsarsenal.MmsArsenal;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

// Gun, impact and grenade sound events, plus lookups for the per-gun sounds named in gun data.
public final class GunSounds {

    public static final SoundEvent BULLET_CLOSE = register("entity.bullet.close");
    public static final SoundEvent HIT_MARKER = register("entity.bullet.hit");
    public static final SoundEvent METAL_HIT = register("block.hit.metal");
    public static final SoundEvent STONE_HIT = register("block.hit.stone");
    public static final SoundEvent WOOD_HIT = register("block.hit.wood");
    public static final SoundEvent SQUISHY_HIT = register("block.hit.squishy");
    public static final SoundEvent HEADSHOT = register("ui.medal.headshot");
    public static final SoundEvent GUN_RUSTLE = register("item.gun_rustle");
    public static final SoundEvent GUN_SCREW = register("item.gun_screw");
    public static final SoundEvent GRENADE_PIN = register("item.grenade.pin");
    public static final SoundEvent STUN_GRENADE_EXPLOSION = register("entity.stun_grenade.explosion");
    public static final SoundEvent STUN_GRENADE_RING = register("entity.stun_grenade.ring");
    public static final SoundEvent MOLOTOV_EXPLOSION = register("entity.molotov.explosion");
    public static final SoundEvent SMOKE_GRENADE_EXPLOSION = register("entity.smoke_grenade.explosion");

    private static final List<String> GUN_SOUNDS = List.of(
        "item.assault_rifle.ejector_pull",
        "item.assault_rifle.ejector_release",
        "item.assault_rifle.enchanted_fire",
        "item.assault_rifle.fire",
        "item.assault_rifle.reload_ejector",
        "item.assault_rifle.reload_magazine_in",
        "item.assault_rifle.reload_magazine_out",
        "item.assault_rifle.silenced_fire",
        "item.blossom_rifle.enchanted_fire",
        "item.blossom_rifle.fire",
        "item.blossom_rifle.silenced_fire",
        "item.bolt_action_rifle.bolt_pull",
        "item.bolt_action_rifle.bolt_release",
        "item.bolt_action_rifle.bullet_in",
        "item.bolt_action_rifle.enchanted_fire",
        "item.bolt_action_rifle.fire",
        "item.bolt_action_rifle.silenced_fire",
        "item.combat_rifle.ejector_pull",
        "item.combat_rifle.ejector_release",
        "item.hypersonic_cannon.charge",
        "item.light_machine_gun.enchanted_fire",
        "item.light_machine_gun.fire",
        "item.light_machine_gun.silenced_fire",
        "item.revolver.chamber_spin",
        "item.revolver.enchanted_fire",
        "item.revolver.fire",
        "item.revolver.reload_bullet_in",
        "item.revolver.reload_bullets_out",
        "item.revolver.silenced_fire",
        "item.rocket_launcher.fire",
        "item.rocket_launcher.lid_close",
        "item.rocket_launcher.lid_open",
        "item.rocket_launcher.rocket_in",
        "item.soulhunter_mk2.enchanted_fire",
        "item.soulhunter_mk2.fire",
        "item.water_drop"
    );

    private GunSounds() {
    }

    public static void register() {
        GUN_SOUNDS.forEach(GunSounds::register);
    }

    // The sound event a gun data file names, registered or not, so any sound id plays.
    public static SoundEvent byId(Identifier id) {
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.getValue(id);
        return event != null ? event : SoundEvent.createVariableRangeEvent(id);
    }

    private static SoundEvent register(String path) {
        Identifier id = MmsArsenal.id(path);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
}
