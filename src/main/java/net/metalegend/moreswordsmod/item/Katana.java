package net.metalegend.moreswordsmod.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;

public class Katana extends SwordItem {
    public Katana(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, FabricItemSettings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (getMaterial() == ToolMaterials.NETHERITE) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 1));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 50, 1));
        }
        return super.postHit(stack, target, attacker);
    }
}
