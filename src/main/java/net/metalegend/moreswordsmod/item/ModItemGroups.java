package net.metalegend.moreswordsmod.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.metalegend.moreswordsmod.MoreSwordsMod;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final ItemGroup MAGICAL_GROUP = Registry.register(Registries.ITEM_GROUP,
            new Identifier(MoreSwordsMod.MOD_ID, "magical"),
            FabricItemGroup.builder().displayName(Text.translatable("itemgroup.magical"))
                    .icon(() -> new ItemStack(ModItems.MAGICAL_STICK)).entries((displayContext, entries) -> {
                        entries.add(ModItems.LIGHTNING_STAFF);
                        entries.add(ModItems.MAGICAL_STICK);
                    }).build());
    public static void registerItemGroups() {
        MoreSwordsMod.LOGGER.info("Registering Item Groups for " + MoreSwordsMod.MOD_ID);
    }
}
