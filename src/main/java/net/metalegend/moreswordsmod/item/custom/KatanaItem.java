package net.metalegend.moreswordsmod.item.custom;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.ToolMaterials;

public class KatanaItem extends SwordItem {
    public KatanaItem(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, FabricItemSettings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.damage(1, attacker, e -> e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
        if (getMaterial() == ToolMaterials.DIAMOND) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 25, 1));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, (int)12.5, 1));
        }

        if (getMaterial() == ToolMaterials.NETHERITE) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 1));
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 50, 1));
        }
        return super.postHit(stack, target, attacker);
    }
}
