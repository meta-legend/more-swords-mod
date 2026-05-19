package net.metalegend.moreswordsmod.item.custom;

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
    private static final int DURABILITY = 768;
    private static final int ENCHANTABILITY = 10;
    private static final int SLAM_COOLDOWN_TICKS = 80;
    private static final int SLAM_DURABILITY_COST = 12;
    private static final double SLAM_RADIUS = 3.0;
    private static final float SLAM_DAMAGE = 4.0f;
    private static final double SLAM_HORIZONTAL_KNOCKBACK = 1.5;
    private static final double SLAM_VERTICAL_KNOCKBACK = 0.35;
    private static final float ARMOR_PENETRATION_RATIO = 0.5f;

    public ObsidianGreatswordItem(Item.Properties properties) {
        super(properties
                .durability(DURABILITY)
                .enchantable(ENCHANTABILITY)
                .attributes(
                ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE,
                                new AttributeModifier(Identifier.withDefaultNamespace("base_attack_damage"),
                                        8, AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ATTACK_SPEED,
                                new AttributeModifier(Identifier.withDefaultNamespace("base_attack_speed"),
                                        -3.2f, AttributeModifier.Operation.ADD_VALUE),
                                EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ATTACK_KNOCKBACK,
                                new AttributeModifier(Identifier.withDefaultNamespace("base_attack_knockback"),
                                        1, AttributeModifier.Operation.ADD_VALUE),
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
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        applyArmorPenetrationBonus(target, attacker);
    }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (user.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        // Require at least 65% swing charge so the slam can't be spammed without actually attacking.
        if (user.getAttackStrengthScale(0.5f) < 0.65f) {
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
                armor * (1.0f - ARMOR_PENETRATION_RATIO),
                armorToughness * (1.0f - ARMOR_PENETRATION_RATIO)
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
                new AABB(user.blockPosition()).inflate(SLAM_RADIUS),
                entity -> entity != user && entity != user.getVehicle() && entity.isAlive()
        );

        for (LivingEntity target : targets) {
            Vec3 direction = target.position().subtract(user.position());
            if (direction.lengthSqr() > 1.0E-6) {
                Vec3 horizontalDirection = new Vec3(direction.x, 0.0, direction.z).normalize();
                target.setDeltaMovement(
                        target.getDeltaMovement().add(
                                horizontalDirection.scale(SLAM_HORIZONTAL_KNOCKBACK).add(0.0, SLAM_VERTICAL_KNOCKBACK, 0.0)
                        )
                );
            } else {
                target.setDeltaMovement(target.getDeltaMovement().add(0.0, SLAM_VERTICAL_KNOCKBACK, 0.0));
            }

            target.hurtMarked = true;
            target.hurtServer(level, user.damageSources().playerAttack(user), SLAM_DAMAGE);
        }

        // Heavy impact thud — lower pitch than explosion for a ground-strike feel.
        level.playSound(null, user.blockPosition(), ModSounds.OBSIDIAN_GREATSWORD_GRAND_SLAM, user.getSoundSource(), 0.8f, 0.6f);

        // Obsidian chip rays radiating outward in 8 directions at ground level.
        BlockParticleOption obsidianChip = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OBSIDIAN.defaultBlockState());
        double groundY = user.getY() + 0.05;
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            for (int step = 1; step <= 4; step++) {
                double dist = step * 0.65;
                level.sendParticles(obsidianChip, user.getX() + dx * dist, groundY, user.getZ() + dz * dist, 4, 0.1, 0.12, 0.1, 0.04);
            }
        }
        // Central smoke burst for impact dust.
        level.sendParticles(ParticleTypes.LARGE_SMOKE, user.getX(), user.getY() + 0.3, user.getZ(), 8, 0.4, 0.15, 0.4, 0.02);
        stack.hurtAndBreak(SLAM_DURABILITY_COST, user, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        user.getCooldowns().addCooldown(stack, SLAM_COOLDOWN_TICKS);
    }

    private static boolean hurtTarget(LivingEntity target, DamageSource source, float damage) {
        return target.level() instanceof ServerLevel level && target.hurtServer(level, source, damage);
    }
}
