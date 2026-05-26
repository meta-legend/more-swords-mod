package net.metalegend.moreswordsmod.item;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.metalegend.moreswordsmod.MoreSwordsMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

// custom creative tab definition for the mod's full item lineup
public class ModItemGroups {
    public static final CreativeModeTab MAGICAL_GROUP = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(MoreSwordsMod.MOD_ID, "magical"),
            FabricCreativeModeTab.builder()
                    .title(Component.translatable("itemgroup.moreswordsmod.magical"))
                    .icon(() -> new ItemStack(ModItems.ARCANE_ROD))
                    .displayItems((displayContext, entries) -> {
                        entries.accept(ModItems.ARCANE_ROD);
                        entries.accept(ModItems.WITHERED_RIB);
                        entries.accept(ModItems.SCATTERED_WINGS);
                        entries.accept(ModItems.SOUL_ESSENCE);
                        entries.accept(ModItems.WITHERED_STALK);
                        entries.accept(ModItems.ZEPHYR_GEM);
                        entries.accept(ModItems.KATANA_HILT);
                        entries.accept(ModItems.IRON_GUARD);
                        entries.accept(ModItems.GOLDEN_GUARD);
                        entries.accept(ModItems.DIAMOND_GUARD);
                        entries.accept(ModItems.NETHERITE_GUARD);
                        entries.accept(ModItems.IRON_KATANA_BLADE);
                        entries.accept(ModItems.GOLDEN_KATANA_BLADE);
                        entries.accept(ModItems.DIAMOND_KATANA_BLADE);
                        entries.accept(ModItems.NETHERITE_KATANA_BLADE);
                        entries.accept(ModItems.IRON_KATANA);
                        entries.accept(ModItems.GOLDEN_KATANA);
                        entries.accept(ModItems.DIAMOND_KATANA);
                        entries.accept(ModItems.NETHERITE_KATANA);
                        entries.accept(ModItems.LIGHTNING_STAFF);
                        entries.accept(ModItems.WIND_STAFF);
                        entries.accept(ModItems.BONE_SCYTHE);
                        entries.accept(ModItems.OBSIDIAN_GREATSWORD);
                    }).build());

    // mirrors the ModItems bootstrap pattern so group registration has an explicit init call
    public static void registerItemGroups() {
        MoreSwordsMod.LOGGER.info("Registering Item Groups for " + MoreSwordsMod.MOD_ID);
    }
}
