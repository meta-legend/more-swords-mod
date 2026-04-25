package net.metalegend.moreswordsmod.item.custom;

import net.metalegend.moreswordsmod.item.TooltipHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.world.phys.Vec3;

public class KatanaItem extends Item {
    private static final String LAST_SHEATH_STRIKE_TICK_KEY = "LastSheathStrikeTick";
    private static final String LAST_SHEATH_STRIKE_TIME_MS_KEY = "LastSheathStrikeTimeMs";
    private static final int SHEATH_STRIKE_WINDOW_TICKS = 60;
    private static final long SHEATH_STRIKE_WINDOW_MS = SHEATH_STRIKE_WINDOW_TICKS * 50L;
    private static final float SHEATH_STRIKE_DAMAGE_MULTIPLIER = 1.0f;
    private static final int IRON_DURABILITY = 450;
    private static final int GOLD_DURABILITY = 128;
    private static final int DIAMOND_DURABILITY = 1561;
    private static final int NETHERITE_DURABILITY = 2031;

    public enum KatanaMaterial {
        IRON, GOLD, DIAMOND, NETHERITE
    }

    private final KatanaMaterial material;

    public KatanaItem(KatanaMaterial material, int attackDamage, float attackSpeed, Item.Properties properties) {
        super(properties
                .durability(getDurability(material))
                .attributes(
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

        long gameTime = attacker.level().getGameTime();
        boolean isSheathStrike = isSheathStrikeReady(stack, gameTime);

        if (isSheathStrike) {
            performSheathStrike(attacker, target);
        }

        if (material == KatanaMaterial.DIAMOND && attacker.getRandom().nextFloat() < 0.25f) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
        }

        if (material == KatanaMaterial.NETHERITE && attacker.getRandom().nextFloat() < 0.33f) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
        }

        markSheathStrikeUse(stack, gameTime);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        if (getSheathStrikeReadyProgress(stack) < 1.0f) {
            return true;
        }

        if (stack.isDamaged()) {
            return super.isBarVisible(stack);
        }

        return false;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        float readyProgress = getSheathStrikeReadyProgress(stack);
        if (readyProgress < 1.0f) {
            return Math.max(1, Math.round(Mth.clamp(readyProgress, 0.0f, 1.0f) * 13.0f));
        }

        return super.getBarWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if (getSheathStrikeReadyProgress(stack) < 1.0f) {
            return 0x6FCBFF;
        }

        return super.getBarColor(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        TooltipHelper.addTooltipLine(builder, "tooltip.moreswordsmod." + material.name().toLowerCase() + "_katana.flavor", ChatFormatting.GRAY);
        TooltipHelper.addAbilitySection(
                builder,
                "tooltip.moreswordsmod." + material.name().toLowerCase() + "_katana.ability_name",
                "tooltip.moreswordsmod." + material.name().toLowerCase() + "_katana.ability_desc_1",
                "tooltip.moreswordsmod." + material.name().toLowerCase() + "_katana.ability_desc_2"
        );
    }

    private static int getDurability(KatanaMaterial material) {
        return switch (material) {
            case IRON -> IRON_DURABILITY;
            case GOLD -> GOLD_DURABILITY;
            case DIAMOND -> DIAMOND_DURABILITY;
            case NETHERITE -> NETHERITE_DURABILITY;
        };
    }

    private static boolean isSheathStrikeReady(ItemStack stack, long currentGameTime) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return true;
        }

        CompoundTag tag = customData.copyTag();
        long lastStrikeTick = tag.getLongOr(LAST_SHEATH_STRIKE_TICK_KEY, Long.MIN_VALUE / 4);
        return currentGameTime - lastStrikeTick >= SHEATH_STRIKE_WINDOW_TICKS;
    }

    private static float getSheathStrikeReadyProgress(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return 1.0f;
        }

        CompoundTag tag = customData.copyTag();
        long lastStrikeTimeMs = tag.getLongOr(LAST_SHEATH_STRIKE_TIME_MS_KEY, Long.MIN_VALUE / 4);
        if (lastStrikeTimeMs <= 0L) {
            return 1.0f;
        }

        long elapsedMs = Math.max(0L, System.currentTimeMillis() - lastStrikeTimeMs);
        return Math.min(1.0f, (float) elapsedMs / SHEATH_STRIKE_WINDOW_MS);
    }

    private static void markSheathStrikeUse(ItemStack stack, long currentGameTime) {
        long currentTimeMs = System.currentTimeMillis();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putLong(LAST_SHEATH_STRIKE_TICK_KEY, currentGameTime);
            tag.putLong(LAST_SHEATH_STRIKE_TIME_MS_KEY, currentTimeMs);
        });
    }

    private void performSheathStrike(LivingEntity attacker, LivingEntity target) {
        float baseAttackDamage = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);

        if (baseAttackDamage > 0.0f) {
            if (attacker instanceof Player player) {
                target.hurt(attacker.damageSources().playerAttack(player), baseAttackDamage * SHEATH_STRIKE_DAMAGE_MULTIPLIER);
            } else {
                target.hurt(attacker.damageSources().mobAttack(attacker), baseAttackDamage * SHEATH_STRIKE_DAMAGE_MULTIPLIER);
            }
        }

        Vec3 dashDirection = target.position().subtract(attacker.position());
        if (dashDirection.lengthSqr() > 1.0E-6) {
            Vec3 normalizedDirection = dashDirection.normalize();
            attacker.setDeltaMovement(normalizedDirection.scale(1.35).add(0.0, 0.08, 0.0));
            attacker.hurtMarked = true;

            if (attacker.level() instanceof ServerLevel serverLevel) {
                Vec3 start = attacker.position().add(0.0, attacker.getBbHeight() * 0.5, 0.0);
                Vec3 end = target.position().add(normalizedDirection.scale(0.8)).add(0.0, target.getBbHeight() * 0.5, 0.0);

                for (int i = 0; i < 6; i++) {
                    double progress = i / 5.0;
                    double particleX = start.x + (end.x - start.x) * progress;
                    double particleY = start.y + (end.y - start.y) * progress;
                    double particleZ = start.z + (end.z - start.z) * progress;
                    serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, particleX, particleY, particleZ, 1, 0.0, 0.0, 0.0, 0.0);
                }
            }
        }
    }
}
