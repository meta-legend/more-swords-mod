package net.metalegend.moreswordsmod.item;

import net.metalegend.moreswordsmod.MoreSwordsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModItemTags {
    public static final TagKey<Item> OBSIDIAN_GREATSWORD_REPAIR_MATERIALS = repairMaterials("obsidian_greatsword");
    public static final TagKey<Item> BONE_SCYTHE_REPAIR_MATERIALS = repairMaterials("bone_scythe");
    public static final TagKey<Item> LIGHTNING_STAFF_REPAIR_MATERIALS = repairMaterials("lightning_staff");
    public static final TagKey<Item> WIND_STAFF_REPAIR_MATERIALS = repairMaterials("wind_staff");

    private ModItemTags() {
    }

    private static TagKey<Item> repairMaterials(String name) {
        return TagKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(MoreSwordsMod.MOD_ID, "repair_materials/" + name)
        );
    }
}
