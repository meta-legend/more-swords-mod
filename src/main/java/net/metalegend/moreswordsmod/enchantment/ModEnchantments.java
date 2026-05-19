package net.metalegend.moreswordsmod.enchantment;

import net.metalegend.moreswordsmod.MoreSwordsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

// resource keys for data-driven mod enchantments
public final class ModEnchantments {
    public static final ResourceKey<Enchantment> OSSUARY = ResourceKey.create(
            Registries.ENCHANTMENT,
            Identifier.fromNamespaceAndPath(MoreSwordsMod.MOD_ID, "ossuary")
    );

    private ModEnchantments() {
    }
}
