package net.metalegend.moreswordsmod.item;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.metalegend.moreswordsmod.MoreSwordsMod;
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

    public static final Item IRON_KATANA = register(
            "iron_katana",
            properties -> new KatanaItem(KatanaItem.KatanaMaterial.IRON, 3, -2.0f, properties)
    );

    public static final Item GOLD_KATANA = register(
            "gold_katana",
            properties -> new KatanaItem(KatanaItem.KatanaMaterial.GOLD, 2, -1.6f, properties)
    );

    public static final Item DIAMOND_KATANA = register(
            "diamond_katana",
            properties -> new KatanaItem(KatanaItem.KatanaMaterial.DIAMOND, 4, -1.8f, properties)
    );

    public static final Item NETHERITE_KATANA = register(
            "netherite_katana",
            properties -> new KatanaItem(KatanaItem.KatanaMaterial.NETHERITE, 5, -1.7f, properties)
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

    public static final Item MAGICAL_STICK = register(
            "magical_stick",
            Item::new
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
    private static void addItemsToCombatItemGroup(FabricCreativeModeTabOutput entries) {
        entries.accept(IRON_KATANA);
        entries.accept(GOLD_KATANA);
        entries.accept(DIAMOND_KATANA);
        entries.accept(NETHERITE_KATANA);
        entries.accept(OBSIDIAN_GREATSWORD);
        entries.accept(BONE_SCYTHE);
    }

    // the static fields do the actual registry writes this method exists as the bootstrap touchpoint from MoreSwordsMod
    public static void registerModItems() {
        MoreSwordsMod.LOGGER.debug("Registering Mod Items For " + MoreSwordsMod.MOD_ID);

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
                .register(ModItems::addItemsToCombatItemGroup);
    }
}
