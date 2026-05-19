package net.metalegend.moreswordsmod.sound;

import net.metalegend.moreswordsmod.MoreSwordsMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

// central sound registry for custom audio cues used by mod items
public final class ModSounds {
    public static final SoundEvent KATANA_SHEATH_STRIKE = register("katana.sheath_strike");
    public static final SoundEvent KATANA_SHEATH_READY = register("katana.sheath_ready");
    public static final SoundEvent KATANA_SHEATH_SHIELD_PIERCE = register("katana.sheath_shield_pierce");
    public static final SoundEvent BONE_SCYTHE_SUMMON_WINDOW_OPEN = register("bone_scythe.summon_window_open");
    public static final SoundEvent BONE_SCYTHE_SUMMON_WINDOW_CLOSE = register("bone_scythe.summon_window_close");
    public static final SoundEvent BONE_SCYTHE_SUMMON_WINDOW_TICK = register("bone_scythe.summon_window_tick");
    public static final SoundEvent BONE_SCYTHE_SOUL_SELECTION = register("bone_scythe.soul_selection");
    public static final SoundEvent BONE_SCYTHE_GRAVE_HARVEST = register("bone_scythe.grave_harvest");
    public static final SoundEvent BONE_SCYTHE_REVENANT_CALL = register("bone_scythe.revenant_call");
    public static final SoundEvent BONE_SCYTHE_FINAL_RECALL = register("bone_scythe.final_recall");
    public static final SoundEvent BONE_SCYTHE_REAPED_SOUL_DISSIPATE = register("bone_scythe.reaped_soul_dissipate");
    public static final SoundEvent BONE_SCYTHE_GRAVE_GARRISON = register("bone_scythe.grave_garrison");
    public static final SoundEvent WIND_STAFF_WIND_LEAP = register("wind_staff.wind_leap");
    public static final SoundEvent WIND_STAFF_GALE_GLIDE = register("wind_staff.gale_glide");
    public static final SoundEvent OBSIDIAN_GREATSWORD_GRAND_SLAM = register("obsidian_greatsword.grand_slam");

    private ModSounds() {
    }

    // uses variable range events so the same sound entry can behave naturally in world playback
    private static SoundEvent register(String id) {
        Identifier soundId = Identifier.fromNamespaceAndPath(MoreSwordsMod.MOD_ID, id);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, soundId, SoundEvent.createVariableRangeEvent(soundId));
    }

    // kept as an explicit bootstrap call so static field registration happens during mod init
    public static void registerModSounds() {
        MoreSwordsMod.LOGGER.info("Registering mod sounds for {}", MoreSwordsMod.MOD_ID);
    }
}
