package net.metalegend.moreswordsmod.item.custom;

import net.metalegend.moreswordsmod.damage.ModDamageTypes;
import net.metalegend.moreswordsmod.entity.custom.ShieldTestDummyEntity;
import net.metalegend.moreswordsmod.item.TooltipHelper;
import net.metalegend.moreswordsmod.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class KatanaItem extends Item {
    private static final String LAST_SHEATH_STRIKE_TICK_KEY = "LastSheathStrikeTick";
    private static final String LAST_SHEATH_STRIKE_TIME_MS_KEY = "LastSheathStrikeTimeMs";
    private static final String SHEATH_READY_SOUND_PLAYED_KEY = "SheathReadySoundPlayed";
    private static final String DEBUG_FORCE_SHIELD_PIERCE_KEY = "DebugForceShieldPierce";
    private static final int SHEATH_STRIKE_WINDOW_TICKS = 60;
    private static final long SHEATH_STRIKE_WINDOW_MS = SHEATH_STRIKE_WINDOW_TICKS * 50L;
    private static final float SHEATH_STRIKE_DAMAGE_MULTIPLIER = 1.5f;
    private static final float AXE_SHIELD_DISABLE_SECONDS = 5.0f;
    private static final int SHIELD_DISABLE_TICKS = Math.round(AXE_SHIELD_DISABLE_SECONDS * 20.0f);
    private static final double SHEATH_STRIKE_DASH_STRENGTH = 1.45;
    private static final double AIR_COMBO_DASH_STRENGTH = 0.55;
    private static final double AIR_COMBO_VERTICAL_KNOCKBACK = 0.65;
    private static final double AIR_COMBO_PLAYER_VERTICAL_KNOCKBACK = 0.35;
    private static final int IRON_DURABILITY = 450;
    private static final int GOLD_DURABILITY = 128;
    private static final int DIAMOND_DURABILITY = 1561;
    private static final int NETHERITE_DURABILITY = 2031;

    public enum KatanaMaterial {
        IRON, GOLD, DIAMOND, NETHERITE
    }

    private final KatanaMaterial material;

    public KatanaItem(KatanaMaterial material, int attackDamage, float attackSpeed, Item.Properties properties) {
        super(applyKatanaProperties(material, properties)
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

    private static Item.Properties applyKatanaProperties(KatanaMaterial material, Item.Properties properties) {
        if (material == KatanaMaterial.NETHERITE) {
            return properties.fireResistant();
        }

        return properties;
    }

    @Override
    public @Nullable DamageSource getItemDamageSource(final LivingEntity attacker) {
        ItemStack weaponStack = attacker.getWeaponItem();
        if (weaponStack.isEmpty() || weaponStack.getItem() != this) {
            return null;
        }

        if (!isSheathStrikeReady(weaponStack, attacker.level().getGameTime())) {
            return null;
        }

        return new DamageSource(
                attacker.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(ModDamageTypes.SHEATH_STRIKE),
                attacker
        );
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);

        long gameTime = attacker.level().getGameTime();
        boolean isSheathStrike = isSheathStrikeReady(stack, gameTime);

        if (isSheathStrike) {
            performSheathStrike(stack, attacker, target);
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
                "tooltip.moreswordsmod." + material.name().toLowerCase() + "_katana.ability_desc_2",
                "tooltip.moreswordsmod." + material.name().toLowerCase() + "_katana.ability_desc_3",
                "tooltip.moreswordsmod." + material.name().toLowerCase() + "_katana.ability_desc_4"
        );
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);

        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) {
            return;
        }

        if (!shouldPlaySheathReadySound(stack)) {
            return;
        }

        level.playSound(null, player.blockPosition(), ModSounds.KATANA_SHEATH_READY, player.getSoundSource(), 0.85f, 1.0f);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(SHEATH_READY_SOUND_PLAYED_KEY, true));
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

    private static boolean shouldPlaySheathReadySound(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }

        CompoundTag tag = customData.copyTag();
        long lastStrikeTimeMs = tag.getLongOr(LAST_SHEATH_STRIKE_TIME_MS_KEY, 0L);
        if (lastStrikeTimeMs <= 0L) {
            return false;
        }

        if (tag.getBooleanOr(SHEATH_READY_SOUND_PLAYED_KEY, false)) {
            return false;
        }

        return getSheathStrikeReadyProgress(stack) >= 1.0f;
    }

    private static void markSheathStrikeUse(ItemStack stack, long currentGameTime) {
        long currentTimeMs = System.currentTimeMillis();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putLong(LAST_SHEATH_STRIKE_TICK_KEY, currentGameTime);
            tag.putLong(LAST_SHEATH_STRIKE_TIME_MS_KEY, currentTimeMs);
            tag.putBoolean(SHEATH_READY_SOUND_PLAYED_KEY, false);
        });
    }

    public static void armDebugShieldPierce(ItemStack stack, long currentGameTime) {
        long currentTimeMs = System.currentTimeMillis();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putLong(LAST_SHEATH_STRIKE_TICK_KEY, currentGameTime - SHEATH_STRIKE_WINDOW_TICKS);
            tag.putLong(LAST_SHEATH_STRIKE_TIME_MS_KEY, currentTimeMs - SHEATH_STRIKE_WINDOW_MS);
            tag.putBoolean(SHEATH_READY_SOUND_PLAYED_KEY, true);
            tag.putBoolean(DEBUG_FORCE_SHIELD_PIERCE_KEY, true);
        });
    }

    private void performSheathStrike(ItemStack stack, LivingEntity attacker, LivingEntity target) {
        float baseAttackDamage = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
        boolean piercedShield = false;

        if (baseAttackDamage > 0.0f) {
            piercedShield = pierceShieldIfBlocking(stack, target);
            if (attacker instanceof Player player) {
                target.hurt(attacker.damageSources().playerAttack(player), baseAttackDamage * SHEATH_STRIKE_DAMAGE_MULTIPLIER);
            } else {
                target.hurt(attacker.damageSources().mobAttack(attacker), baseAttackDamage * SHEATH_STRIKE_DAMAGE_MULTIPLIER);
            }
        }

        Vec3 dashDirection = target.position().subtract(attacker.position());
        if (dashDirection.lengthSqr() > 1.0E-6) {
            Vec3 normalizedDirection = dashDirection.normalize();
            double dashStrength = target.onGround() ? SHEATH_STRIKE_DASH_STRENGTH : AIR_COMBO_DASH_STRENGTH;
            attacker.setDeltaMovement(normalizedDirection.scale(dashStrength).add(0.0, 0.08, 0.0));
            attacker.hurtMarked = true;

            attacker.level().playSound(
                    null,
                    attacker.blockPosition(),
                    piercedShield ? ModSounds.KATANA_SHEATH_SHIELD_PIERCE : ModSounds.KATANA_SHEATH_STRIKE,
                    attacker.getSoundSource(),
                    0.9f,
                    1.0f
            );

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

        if (!target.onGround()) {
            double verticalBoost = target instanceof Player ? AIR_COMBO_PLAYER_VERTICAL_KNOCKBACK : AIR_COMBO_VERTICAL_KNOCKBACK;
            Vec3 targetVelocity = target.getDeltaMovement();
            target.setDeltaMovement(targetVelocity.x, Math.max(targetVelocity.y, 0.0) + verticalBoost, targetVelocity.z);
            target.hurtMarked = true;
        }
    }

    private static boolean pierceShieldIfBlocking(ItemStack stack, LivingEntity target) {
        boolean debugForced = consumeDebugForcedShieldPierce(stack);
        ItemStack blockingWith = target.getItemBlockingWith();
        if (blockingWith == null || !blockingWith.is(Items.SHIELD)) {
            return debugForced;
        }

        if (target instanceof Player player) {
            player.getCooldowns().addCooldown(blockingWith, SHIELD_DISABLE_TICKS);
        } else if (target instanceof ShieldTestDummyEntity dummy) {
            dummy.disableShieldForTicks(SHIELD_DISABLE_TICKS);
        }

        target.stopUsingItem();
        return true;
    }

    private static boolean consumeDebugForcedShieldPierce(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }

        CompoundTag tag = customData.copyTag();
        if (!tag.getBooleanOr(DEBUG_FORCE_SHIELD_PIERCE_KEY, false)) {
            return false;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> data.putBoolean(DEBUG_FORCE_SHIELD_PIERCE_KEY, false));
        return true;
    }
}
