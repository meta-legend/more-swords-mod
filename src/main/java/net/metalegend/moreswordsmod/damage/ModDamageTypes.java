package net.metalegend.moreswordsmod.damage;

import net.metalegend.moreswordsmod.MoreSwordsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

public final class ModDamageTypes {
    public static final ResourceKey<DamageType> SHEATH_STRIKE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(MoreSwordsMod.MOD_ID, "sheath_strike")
    );

    private ModDamageTypes() {
    }
}
