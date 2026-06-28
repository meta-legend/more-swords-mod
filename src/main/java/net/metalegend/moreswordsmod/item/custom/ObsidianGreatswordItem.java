package net.metalegend.moreswordsmod.item.custom;

import net.metalegend.moreswordsmod.advancement.ModCriteriaTriggers;
import net.metalegend.moreswordsmod.config.ModConfig;
import net.metalegend.moreswordsmod.item.ModItemTags;
import net.metalegend.moreswordsmod.item.TooltipHelper;
import net.metalegend.moreswordsmod.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Consumer;

public class ObsidianGreatswordItem extends Item {
    public ObsidianGreatswordItem(Item.Properties properties) {
        super(properties
                .durability(config().durability)
                .repairable(ModItemTags.OBSIDIAN_GREATSWORD_REPAIR_MATERIALS)
                .enchantable(config().enchantability)
                .attributes(
                ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE,
                                new AttributeModifier(Identifier.withDefaultNamespace("base_attack_damage"),
                                        config().attackDamage, AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ATTACK_SPEED,
                                new AttributeModifier(Identifier.withDefaultNamespace("base_attack_speed"),
                                        config().attackSpeed, AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ATTACK_KNOCKBACK,
                                new AttributeModifier(Identifier.withDefaultNamespace("base_attack_knockback"),
                                        config().attackKnockback, AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND)
                        .build()
        ));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        TooltipHelper.addTooltipLine(builder, "tooltip.moreswordsmod.obsidian_greatsword.flavor", ChatFormatting.GRAY);
        TooltipHelper.addAbilitySection(
                builder,
                "tooltip.moreswordsmod.obsidian_greatsword.ability_name",
                "tooltip.moreswordsmod.obsidian_greatsword.ability_desc_1",
                "tooltip.moreswordsmod.obsidian_greatsword.ability_desc_2"
        );
        TooltipHelper.addEnchantmentSeparatorIfNeeded(stack, builder);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(config().attackDurabilityCost, attacker, EquipmentSlot.MAINHAND);
        applyArmorPenetrationBonus(target, attacker);
    }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (user.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        // Require at least 65% swing charge so the slam can't be spammed without actually attacking.
        if (user.getAttackStrengthScale(0.5f) < config().minimumGrandSlamAttackStrength) {
            if (user instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(
                        Component.translatable("message.moreswordsmod.obsidian_greatsword.slam_not_ready").withStyle(ChatFormatting.RED),
                        true
                );
            }
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            performGroundSlam((ServerLevel) level, user, stack, hand);
        }

        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    private void applyArmorPenetrationBonus(LivingEntity target, LivingEntity attacker) {
        float baseAttackDamage = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (baseAttackDamage <= 0.0f) {
            return;
        }

        if (attacker instanceof Player player && player.getAttackStrengthScale(0.5f) < 0.9f) {
            return;
        }

        float armor = target.getArmorValue();
        float armorToughness = (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        if (armor <= 0.0f && armorToughness <= 0.0f) {
            return;
        }

        float fullArmorDamage = CombatRules.getDamageAfterAbsorb(target, baseAttackDamage, attacker.getLastDamageSource(), armor, armorToughness);
        float reducedArmorDamage = CombatRules.getDamageAfterAbsorb(
                target,
                baseAttackDamage,
                attacker.getLastDamageSource(),
                armor * (1.0f - config().armorPenetrationRatio),
                armorToughness * (1.0f - config().armorPenetrationRatio)
        );
        float bonusDamage = reducedArmorDamage - fullArmorDamage;

        if (bonusDamage <= 0.0f) {
            return;
        }

        if (attacker instanceof Player player) {
            hurtTarget(target, attacker.damageSources().playerAttack(player), bonusDamage);
        } else {
            hurtTarget(target, attacker.damageSources().mobAttack(attacker), bonusDamage);
        }
    }

    private void performGroundSlam(ServerLevel level, Player user, ItemStack stack, InteractionHand hand) {
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(user.blockPosition()).inflate(config().grandSlamRadius),
                entity -> entity != user && entity != user.getVehicle() && entity.isAlive()
        );

        int hitTargets = 0;
        for (LivingEntity target : targets) {
            Vec3 direction = target.position().subtract(user.position());
            if (direction.lengthSqr() > 1.0E-6) {
                Vec3 horizontalDirection = new Vec3(direction.x, 0.0, direction.z).normalize();
                target.setDeltaMovement(
                        target.getDeltaMovement().add(
                                horizontalDirection.scale(config().grandSlamHorizontalKnockback).add(0.0, config().grandSlamVerticalKnockback, 0.0)
                        )
                );
            } else {
                target.setDeltaMovement(target.getDeltaMovement().add(0.0, config().grandSlamVerticalKnockback, 0.0));
            }

            target.hurtMarked = true;
            if (target.hurtServer(level, user.damageSources().playerAttack(user), config().grandSlamDamage)) {
                hitTargets++;
                level.sendParticles(
                        ParticleTypes.CRIT,
                        target.getX(),
                        target.getY() + target.getBbHeight() * 0.55,
                        target.getZ(),
                        8,
                        target.getBbWidth() * 0.25,
                        target.getBbHeight() * 0.2,
                        target.getBbWidth() * 0.25,
                        0.08
                );
            }
        }

        if (hitTargets >= 3 && user instanceof ServerPlayer serverPlayer) {
            ModCriteriaTriggers.GRAND_SLAM_THREE.trigger(serverPlayer);
        }

        // Heavy impact thud with a low pitch and high volume for a ground-strike feel.
        level.playSound(null, user.blockPosition(), ModSounds.OBSIDIAN_GREATSWORD_GRAND_SLAM, user.getSoundSource(), 1.05f, 0.55f);

        // Obsidian chip rays radiating outward at ground level.
        BlockParticleOption obsidianChip = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OBSIDIAN.defaultBlockState());
        double groundY = user.getY() + 0.05;
        for (int i = 0; i < 16; i++) {
            double angle = i * Math.PI / 8;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            for (int step = 1; step <= 5; step++) {
                double dist = step * 0.65;
                level.sendParticles(obsidianChip, user.getX() + dx * dist, groundY, user.getZ() + dz * dist, 5, 0.1, 0.12, 0.1, 0.04);
            }
        }
        // Central smoke burst for impact dust.
        level.sendParticles(ParticleTypes.EXPLOSION, user.getX(), user.getY() + 0.25, user.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, user.getX(), user.getY() + 0.3, user.getZ(), 16, 0.55, 0.2, 0.55, 0.025);
        stack.hurtAndBreak(config().grandSlamDurabilityCost, user, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        user.getCooldowns().addCooldown(stack, config().grandSlamCooldownTicks);
    }

    private static boolean hurtTarget(LivingEntity target, DamageSource source, float damage) {
        return target.level() instanceof ServerLevel level && target.hurtServer(level, source, damage);
    }

    private static ModConfig.ObsidianGreatsword config() {
        return ModConfig.get().obsidianGreatsword;
    }
}
