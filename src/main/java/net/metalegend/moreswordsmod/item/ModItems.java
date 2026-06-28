package net.metalegend.moreswordsmod.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.metalegend.moreswordsmod.MoreSwordsMod;
import net.metalegend.moreswordsmod.config.ModConfig;
import net.metalegend.moreswordsmod.item.custom.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import java.util.function.Function;

// item registry and combat-tab population for all mod items
public class ModItems {

    public static final Item COPPER_KATANA = register(
            "copper_katana",
            properties -> new KatanaItem(
                    KatanaItem.KatanaMaterial.COPPER,
                    ModConfig.get().katanas.copper.attackDamage,
                    ModConfig.get().katanas.copper.attackSpeed,
                    properties
            )
    );

    public static final Item IRON_KATANA = register(
            "iron_katana",
            properties -> new KatanaItem(
                    KatanaItem.KatanaMaterial.IRON,
                    ModConfig.get().katanas.iron.attackDamage,
                    ModConfig.get().katanas.iron.attackSpeed,
                    properties
            )
    );

    public static final Item GOLDEN_KATANA = register(
            "golden_katana",
            properties -> new KatanaItem(
                    KatanaItem.KatanaMaterial.GOLD,
                    ModConfig.get().katanas.gold.attackDamage,
                    ModConfig.get().katanas.gold.attackSpeed,
                    properties
            )
    );

    public static final Item DIAMOND_KATANA = register(
            "diamond_katana",
            properties -> new KatanaItem(
                    KatanaItem.KatanaMaterial.DIAMOND,
                    ModConfig.get().katanas.diamond.attackDamage,
                    ModConfig.get().katanas.diamond.attackSpeed,
                    properties
            )
    );

    public static final Item NETHERITE_KATANA = register(
            "netherite_katana",
            properties -> new KatanaItem(
                    KatanaItem.KatanaMaterial.NETHERITE,
                    ModConfig.get().katanas.netherite.attackDamage,
                    ModConfig.get().katanas.netherite.attackSpeed,
                    properties
            )
    );

    public static final Item KATANA_HILT = register(
            "katana_hilt",
            properties -> new FlavorTextItem("katana_hilt", properties)
    );

    public static final Item COPPER_GUARD = register(
            "copper_guard",
            properties -> new FlavorTextItem("copper_guard", properties)
    );

    public static final Item IRON_GUARD = register(
            "iron_guard",
            properties -> new FlavorTextItem("iron_guard", properties)
    );

    public static final Item GOLDEN_GUARD = register(
            "golden_guard",
            properties -> new FlavorTextItem("golden_guard", properties)
    );

    public static final Item DIAMOND_GUARD = register(
            "diamond_guard",
            properties -> new FlavorTextItem("diamond_guard", properties)
    );

    public static final Item NETHERITE_GUARD = register(
            "netherite_guard",
            properties -> new FlavorTextItem("netherite_guard", properties)
    );

    public static final Item COPPER_KATANA_BLADE = register(
            "copper_katana_blade",
            properties -> new FlavorTextItem("copper_katana_blade", properties)
    );

    public static final Item IRON_KATANA_BLADE = register(
            "iron_katana_blade",
            properties -> new FlavorTextItem("iron_katana_blade", properties)
    );

    public static final Item GOLDEN_KATANA_BLADE = register(
            "golden_katana_blade",
            properties -> new FlavorTextItem("golden_katana_blade", properties)
    );

    public static final Item DIAMOND_KATANA_BLADE = register(
            "diamond_katana_blade",
            properties -> new FlavorTextItem("diamond_katana_blade", properties)
    );

    public static final Item NETHERITE_KATANA_BLADE = register(
            "netherite_katana_blade",
            properties -> new FlavorTextItem("netherite_katana_blade", properties)
    );

    public static final Item OBSIDIAN_GREATSWORD = register(
            "obsidian_greatsword",
            ObsidianGreatswordItem::new
    );

    public static final Item BONE_SCYTHE = register(
            "bone_scythe",
            BoneScytheItem::new
    );

    public static final Item LIGHTNING_STAFF = register(
            "lightning_staff",
            LightningStaffItem::new
    );

    public static final Item WIND_STAFF = register(
            "wind_staff",
            WindStaffItem::new
    );

    public static final Item WITHERED_RIB = register(
            "withered_rib",
            properties -> new FlavorTextItem("withered_rib", properties)
    );

    public static final Item SCATTERED_WINGS = register(
            "scattered_wings",
            properties -> new FlavorTextItem("scattered_wings", properties)
    );

    public static final Item SOUL_ESSENCE = register(
            "soul_essence",
            properties -> new FlavorTextItem("soul_essence", properties)
    );

    public static final Item WITHERED_STALK = register(
            "withered_stalk",
            properties -> new FlavorTextItem("withered_stalk", properties)
    );

    public static final Item ZEPHYR_GEM = register(
            "zephyr_gem",
            properties -> new FlavorTextItem("zephyr_gem", properties)
    );

    public static final Item ARCANE_ROD = register(
            "arcane_rod",
            properties -> new FlavorTextItem("arcane_rod", properties)
    );

    // builds the Item.Properties with a stable ResourceKey before handing construction to the item factory
    private static Item register(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(MoreSwordsMod.MOD_ID, name)
        );

        Item.Properties properties = new Item.Properties().setId(key);
        Item item = factory.apply(properties);

        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    // only combat-relevant items are mirrored into the vanilla combat tab
    private static void addItemsToCombatItemGroup(FabricItemGroupEntries entries) {
        entries.accept(COPPER_KATANA);
        entries.accept(IRON_KATANA);
        entries.accept(GOLDEN_KATANA);
        entries.accept(DIAMOND_KATANA);
        entries.accept(NETHERITE_KATANA);
        entries.accept(OBSIDIAN_GREATSWORD);
        entries.accept(BONE_SCYTHE);
    }

    private static void addItemsToIngredientsItemGroup(FabricItemGroupEntries entries) {
        entries.accept(KATANA_HILT);
        entries.accept(COPPER_GUARD);
        entries.accept(IRON_GUARD);
        entries.accept(GOLDEN_GUARD);
        entries.accept(DIAMOND_GUARD);
        entries.accept(NETHERITE_GUARD);
        entries.accept(COPPER_KATANA_BLADE);
        entries.accept(IRON_KATANA_BLADE);
        entries.accept(GOLDEN_KATANA_BLADE);
        entries.accept(DIAMOND_KATANA_BLADE);
        entries.accept(NETHERITE_KATANA_BLADE);
        entries.accept(WITHERED_RIB);
        entries.accept(SCATTERED_WINGS);
        entries.accept(SOUL_ESSENCE);
        entries.accept(WITHERED_STALK);
        entries.accept(ZEPHYR_GEM);
    }

    // the static fields do the actual registry writes this method exists as the bootstrap touchpoint from MoreSwordsMod
    public static void registerModItems() {
        MoreSwordsMod.LOGGER.debug("Registering Mod Items For " + MoreSwordsMod.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT)
                .register(ModItems::addItemsToCombatItemGroup);
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
                .register(ModItems::addItemsToIngredientsItemGroup);
    }
}
