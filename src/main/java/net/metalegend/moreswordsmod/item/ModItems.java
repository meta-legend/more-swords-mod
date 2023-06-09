package net.metalegend.moreswordsmod.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.yarn.constants.MiningLevels;
import net.minecraft.item.*;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.metalegend.moreswordsmod.MoreSwordsMod;

public class ModItems {
    public static final Item IRON_KATANA = registerItem("iron_katana", new Katana(ToolMaterials.IRON, 4, -2.2f, new FabricItemSettings()), ItemGroups.COMBAT);
    public static final Item GOLD_KATANA = registerItem("gold_katana", new Katana(ToolMaterials.GOLD, 4, -2f, new FabricItemSettings()), ItemGroups.COMBAT);
    public static final Item DIAMOND_KATANA = registerItem("diamond_katana", new Katana(ToolMaterials.DIAMOND, 5, -1.8f, new FabricItemSettings()), ItemGroups.COMBAT);
    public static final Item NETHERITE_KATANA = registerItem("netherite_katana", new Katana(ToolMaterials.NETHERITE, 6, -1.6f, new FabricItemSettings().fireproof()), ItemGroups.COMBAT);

    private static Item registerItem(String name, Item item, RegistryKey<ItemGroup> group) {
        ItemGroupEvents.modifyEntriesEvent(group).register(entries -> entries.add(item));
        return Registry.register(Registries.ITEM, new Identifier(MoreSwordsMod.MOD_ID, name), item);
    }
    public static void registerModItems() {
        MoreSwordsMod.LOGGER.debug("Registering Mod Items For " + MoreSwordsMod.MOD_ID);
    }
}
