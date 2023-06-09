package net.metalegend.moreswordsmod;

import net.fabricmc.api.ModInitializer;

import net.metalegend.moreswordsmod.item.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoreSwordsMod implements ModInitializer {
    public static final String MOD_ID = "moreswordsmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.registerModItems();
    }
}