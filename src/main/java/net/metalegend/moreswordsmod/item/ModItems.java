package net.metalegend.moreswordsmod.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.metalegend.moreswordsmod.item.custom.KatanaItem;
import net.metalegend.moreswordsmod.item.custom.LightningStaffItem;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.metalegend.moreswordsmod.MoreSwordsMod;

public class ModItems {
    public static final Item IRON_KATANA = registerItem("iron_katana", new KatanaItem(ToolMaterials.IRON, 4, -2.2f, new FabricItemSettings().food(FoodComponents.GOLDEN_CARROT)));
    public static final Item GOLD_KATANA = registerItem("gold_katana", new KatanaItem(ToolMaterials.GOLD, 4, -2f, new FabricItemSettings().food(FoodComponents.GOLDEN_APPLE)));
    public static final Item DIAMOND_KATANA = registerItem("diamond_katana", new KatanaItem(ToolMaterials.DIAMOND, 5, -1.8f, new FabricItemSettings().food(FoodComponents.GOLDEN_APPLE)));
    public static final Item NETHERITE_KATANA = registerItem("netherite_katana", new KatanaItem(ToolMaterials.NETHERITE, 6, -1.6f, new FabricItemSettings().fireproof().food(FoodComponents.ENCHANTED_GOLDEN_APPLE)));
    public static final Item LIGHTNING_STAFF = registerItem("lightning_staff", new LightningStaffItem(new FabricItemSettings()));
    public static final Item MAGICAL_STICK = registerItem("magical_stick", new Item(new FabricItemSettings()));

    private static void addItemsToCombatItemGroup(FabricItemGroupEntries entries) {
        entries.add(IRON_KATANA);
        entries.add(GOLD_KATANA);
        entries.add(DIAMOND_KATANA);
        entries.add(NETHERITE_KATANA);
    }
    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(MoreSwordsMod.MOD_ID, name), item);
    }
    public static void registerModItems() {
        MoreSwordsMod.LOGGER.debug("Registering Mod Items For " + MoreSwordsMod.MOD_ID);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(ModItems::addItemsToCombatItemGroup);
    }
}
