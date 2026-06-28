package net.metalegend.moreswordsmod.advancement;

import net.metalegend.moreswordsmod.MoreSwordsMod;
import net.minecraft.advancements.criterion.PlayerTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class ModCriteriaTriggers {
    public static final PlayerTrigger SHEATH_STRIKE = register("sheath_strike");
    public static final PlayerTrigger STORMCALL = register("stormcall");
    public static final PlayerTrigger GALE_GLIDE = register("gale_glide");
    public static final PlayerTrigger TIER_5_REAPED_SOUL = register("tier_5_reaped_soul");
    public static final PlayerTrigger GRAND_SLAM_THREE = register("grand_slam_three");

    private ModCriteriaTriggers() {
    }

    public static void registerModCriteriaTriggers() {
        MoreSwordsMod.LOGGER.debug("Registering advancement criteria for " + MoreSwordsMod.MOD_ID);
    }

    private static PlayerTrigger register(String name) {
        return Registry.register(
                BuiltInRegistries.TRIGGER_TYPES,
                Identifier.fromNamespaceAndPath(MoreSwordsMod.MOD_ID, name),
                new PlayerTrigger()
        );
    }
}
