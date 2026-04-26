package net.metalegend.moreswordsmod.sound;

import net.metalegend.moreswordsmod.MoreSwordsMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    public static final SoundEvent KATANA_SHEATH_STRIKE = register("katana.sheath_strike");
    public static final SoundEvent KATANA_SHEATH_READY = register("katana.sheath_ready");
    public static final SoundEvent KATANA_SHEATH_SHIELD_PIERCE = register("katana.sheath_shield_pierce");

    private ModSounds() {
    }

    private static SoundEvent register(String id) {
        Identifier soundId = Identifier.fromNamespaceAndPath(MoreSwordsMod.MOD_ID, id);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, soundId, SoundEvent.createVariableRangeEvent(soundId));
    }

    public static void registerModSounds() {
        MoreSwordsMod.LOGGER.info("Registering mod sounds for {}", MoreSwordsMod.MOD_ID);
    }
}
