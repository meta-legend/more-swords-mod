package net.metalegend.moreswordsmod.item;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.metalegend.moreswordsmod.MoreSwordsMod;
import net.metalegend.moreswordsmod.item.custom.BoneScytheItem;
import net.metalegend.moreswordsmod.item.custom.KatanaItem;
import net.metalegend.moreswordsmod.item.custom.LightningStaffItem;
import net.metalegend.moreswordsmod.item.custom.ObsidianGreatswordItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public class ModItems {

    public static final Item IRON_KATANA = register(
            "iron_katana",
            properties -> new KatanaItem(KatanaItem.KatanaMaterial.IRON, 4, -2.2f, properties)
    );

    public static final Item GOLD_KATANA = register(
            "gold_katana",
            properties -> new KatanaItem(KatanaItem.KatanaMaterial.GOLD, 4, -2.0f, properties)
    );

    public static final Item DIAMOND_KATANA = register(
            "diamond_katana",
            properties -> new KatanaItem(KatanaItem.KatanaMaterial.DIAMOND, 5, -1.8f, properties)
    );

    public static final Item NETHERITE_KATANA = register(
            "netherite_katana",
            properties -> new KatanaItem(KatanaItem.KatanaMaterial.NETHERITE, 6, -1.6f, properties)
    );

    public static final Item LIGHTNING_STAFF = register(
            "lightning_staff",
            LightningStaffItem::new
    );

    public static final Item MAGICAL_STICK = register(
            "magical_stick",
            Item::new
    );

    public static final Item OBSIDIAN_GREATSWORD = register(
            "obsidian_greatsword",
            ObsidianGreatswordItem::new
    );

    public static final Item BONE_SCYTHE = register(
            "bone_scythe",
            BoneScytheItem::new
    );

    private static Item register(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(MoreSwordsMod.MOD_ID, name)
        );

        Item.Properties properties = new Item.Properties().setId(key);
        Item item = factory.apply(properties);

        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    private static void addItemsToCombatItemGroup(FabricCreativeModeTabOutput entries) {
        entries.accept(IRON_KATANA);
        entries.accept(GOLD_KATANA);
        entries.accept(DIAMOND_KATANA);
        entries.accept(NETHERITE_KATANA);
        entries.accept(OBSIDIAN_GREATSWORD);
        entries.accept(BONE_SCYTHE);
    }

    public static void registerModItems() {
        MoreSwordsMod.LOGGER.debug("Registering Mod Items For " + MoreSwordsMod.MOD_ID);

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
                .register(ModItems::addItemsToCombatItemGroup);
    }
}