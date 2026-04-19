package net.metalegend.moreswordsmod.item.custom;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public class KatanaItem extends Item {

    public enum KatanaMaterial {
        IRON, GOLD, DIAMOND, NETHERITE
    }

    private final KatanaMaterial material;

    public KatanaItem(KatanaMaterial material, int attackDamage, float attackSpeed, Item.Properties properties) {
        super(properties.attributes(
                ItemAttributeModifiers.builder()
                        .add(
                                Attributes.ATTACK_DAMAGE,
                                new AttributeModifier(
                                        Identifier.withDefaultNamespace("base_attack_damage"),
                                        attackDamage,
                                        AttributeModifier.Operation.ADD_VALUE
                                ),
                                EquipmentSlotGroup.MAINHAND
                        )
                        .add(
                                Attributes.ATTACK_SPEED,
                                new AttributeModifier(
                                        Identifier.withDefaultNamespace("base_attack_speed"),
                                        attackSpeed,
                                        AttributeModifier.Operation.ADD_VALUE
                                ),
                                EquipmentSlotGroup.MAINHAND
                        )
                        .build()
        ));

        this.material = material;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);

        if (material == KatanaMaterial.DIAMOND) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25, 1));
            target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 12, 1));
        }

        if (material == KatanaMaterial.NETHERITE) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 50, 1));
        }
    }
}