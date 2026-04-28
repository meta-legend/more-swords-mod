package net.metalegend.moreswordsmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import net.metalegend.moreswordsmod.command.DebugCommands;
import net.metalegend.moreswordsmod.entity.ModEntities;
import net.metalegend.moreswordsmod.item.ModItemGroups;
import net.metalegend.moreswordsmod.item.ModItems;
import net.metalegend.moreswordsmod.network.ModNetworking;
import net.metalegend.moreswordsmod.sound.ModSounds;
import net.metalegend.moreswordsmod.soul.ReapedSoulManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoreSwordsMod implements ModInitializer {
    public static final String MOD_ID = "moreswordsmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModEntities.registerModEntities();
        ModItemGroups.registerItemGroups();
        ModItems.registerModItems();
        ModNetworking.register();
        ModSounds.registerModSounds();
        ReapedSoulManager.register();
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            DebugCommands.register();
        }
    }
}
