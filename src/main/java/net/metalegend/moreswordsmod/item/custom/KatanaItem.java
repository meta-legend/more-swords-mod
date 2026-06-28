package net.metalegend.moreswordsmod.item.custom;

import net.metalegend.moreswordsmod.advancement.ModCriteriaTriggers;
import net.metalegend.moreswordsmod.config.ModConfig;
import net.metalegend.moreswordsmod.damage.ModDamageTypes;
import net.metalegend.moreswordsmod.item.TooltipHelper;
import net.metalegend.moreswordsmod.network.PlaySheathStrikeAnimationPayload;
import net.metalegend.moreswordsmod.sound.ModSounds;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// katana combat item centered on the sheath strike window
// each stack tracks its own readiness timer when the window is ready the next committed
// hit turns into a dash strike with bonus damage and a short defensive protection window
// during the actual lunge
public class KatanaItem extends Item {
    private static final String LAST_SHEATH_STRIKE_TICK_KEY = "LastSheathStrikeTick";
    private static final String LAST_SHEATH_STRIKE_TIME_MS_KEY = "LastSheathStrikeTimeMs";
    private static final String SHEATH_READY_SOUND_PLAYED_KEY = "SheathReadySoundPlayed";
    private static final double SHEATH_STRIKE_ANIMATION_RANGE_SQR = 4096.0;
    private static final Map<UUID, Long> ACTIVE_DASH_PROTECTION_UNTIL = new ConcurrentHashMap<>();

    public enum KatanaMaterial {
        COPPER, IRON, GOLD, DIAMOND, NETHERITE
    }

    private final KatanaMaterial material;

    public KatanaItem(KatanaMaterial material, int attackDamage, float attackSpeed, Item.Properties properties) {
        super(applyKatanaProperties(material, properties)
                .durability(getDurability(material))
                .repairable(getRepairItems(material))
                .enchantable(getEnchantability(material))
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

        ModConfig.Katanas config = katanaConfig();
        if (material == KatanaMaterial.DIAMOND && attacker.getRandom().nextFloat() < config.diamondWeaknessChance) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, config.diamondWeaknessDurationTicks, 0));
        }

        if (material == KatanaMaterial.NETHERITE && attacker.getRandom().nextFloat() < config.netheriteWeaknessChance) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, config.netheriteWeaknessDurationTicks, 0));
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
        String tooltipMaterialName = getTooltipMaterialName(material);
        TooltipHelper.addTooltipLine(builder, "tooltip.moreswordsmod." + tooltipMaterialName + "_katana.flavor", ChatFormatting.GRAY);
        TooltipHelper.addAbilitySection(
                builder,
                "tooltip.moreswordsmod." + tooltipMaterialName + "_katana.ability_name",
                "tooltip.moreswordsmod." + tooltipMaterialName + "_katana.ability_desc_1",
                "tooltip.moreswordsmod." + tooltipMaterialName + "_katana.ability_desc_2",
                "tooltip.moreswordsmod." + tooltipMaterialName + "_katana.ability_desc_3",
                "tooltip.moreswordsmod." + tooltipMaterialName + "_katana.ability_desc_4",
                "tooltip.moreswordsmod." + tooltipMaterialName + "_katana.ability_desc_5",
                "tooltip.moreswordsmod." + tooltipMaterialName + "_katana.ability_desc_6"
        );
        TooltipHelper.addEnchantmentSeparatorIfNeeded(stack, builder);
    }

    private static String getTooltipMaterialName(KatanaMaterial material) {
        return material == KatanaMaterial.GOLD ? "golden" : material.name().toLowerCase();
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
        return getStats(material).durability;
    }

    private static int getEnchantability(KatanaMaterial material) {
        return getStats(material).enchantability;
    }

    private static ModConfig.KatanaStats getStats(KatanaMaterial material) {
        ModConfig.Katanas config = katanaConfig();
        return switch (material) {
            case COPPER -> config.copper;
            case IRON -> config.iron;
            case GOLD -> config.gold;
            case DIAMOND -> config.diamond;
            case NETHERITE -> config.netherite;
        };
    }

    private static ModConfig.Katanas katanaConfig() {
        return ModConfig.get().katanas;
    }

    private static TagKey<Item> getRepairItems(KatanaMaterial material) {
        return switch (material) {
            case COPPER -> ItemTags.COPPER_TOOL_MATERIALS;
            case IRON -> ItemTags.IRON_TOOL_MATERIALS;
            case GOLD -> ItemTags.GOLD_TOOL_MATERIALS;
            case DIAMOND -> ItemTags.DIAMOND_TOOL_MATERIALS;
            case NETHERITE -> ItemTags.NETHERITE_TOOL_MATERIALS;
        };
    }

    private static boolean isSheathStrikeReady(ItemStack stack, long currentGameTime) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return true;
        }

        CompoundTag tag = customData.copyTag();
        long lastStrikeTick = tag.getLongOr(LAST_SHEATH_STRIKE_TICK_KEY, Long.MIN_VALUE / 4);
        return currentGameTime - lastStrikeTick >= katanaConfig().sheathStrikeCooldownTicks;
    }

    // client animation prediction uses the same stack timer that the server checks before committing
    public static boolean isSheathStrikeReadyForAnimation(ItemStack stack, long currentGameTime) {
        return stack.getItem() instanceof KatanaItem && isSheathStrikeReady(stack, currentGameTime);
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
        return Math.min(1.0f, (float) elapsedMs / (katanaConfig().sheathStrikeCooldownTicks * 50L));
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

    // projectile immunity is enforced from the LivingEntity mixin while dash protection is active
    public static boolean blocksProjectileDamage(LivingEntity entity, DamageSource source) {
        return isDashProtected(entity) && source.is(DamageTypeTags.IS_PROJECTILE);
    }

    // only direct melee hits are softened while fire fall explosions and magic are left unchanged
    public static float modifyIncomingDashDamage(LivingEntity entity, DamageSource source, float damage) {
        if (isDirectMeleeDamage(entity, source)) {
            return damage * katanaConfig().dashMeleeDamageMultiplier;
        }

        return damage;
    }

    public static boolean blocksDashKnockback(LivingEntity entity) {
        return isDashProtected(entity);
    }

    // the dash protection window is granted only when Sheath Strike commits not while
    // the sheath window is merely ready that keeps readiness offensive instead of passive
    private void performSheathStrike(ItemStack stack, LivingEntity attacker, LivingEntity target) {
        float baseAttackDamage = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
        boolean piercedShield = false;
        grantDashProtection(attacker);
        broadcastSheathStrikeAnimation(attacker);
        if (attacker instanceof ServerPlayer player) {
            ModCriteriaTriggers.SHEATH_STRIKE.trigger(player);
        }

        if (baseAttackDamage > 0.0f) {
            piercedShield = pierceShieldIfBlocking(stack, target);
            float sheathStrikeDamage = baseAttackDamage * katanaConfig().sheathStrikeDamageMultiplier;
            if (attacker instanceof Player player) {
                hurtTarget(target, attacker.damageSources().playerAttack(player), sheathStrikeDamage);
            } else {
                hurtTarget(target, attacker.damageSources().mobAttack(attacker), sheathStrikeDamage);
            }
        }

        Vec3 dashDirection = target.position().subtract(attacker.position());
        if (dashDirection.lengthSqr() > 1.0E-6) {
            Vec3 normalizedDirection = dashDirection.normalize();
            double dashStrength = target.onGround() ? katanaConfig().sheathStrikeDashStrength : katanaConfig().airComboDashStrength;
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
            double verticalBoost = target instanceof Player ? katanaConfig().airComboPlayerVerticalKnockback : katanaConfig().airComboVerticalKnockback;
            Vec3 targetVelocity = target.getDeltaMovement();
            target.setDeltaMovement(targetVelocity.x, Math.max(targetVelocity.y, 0.0) + verticalBoost, targetVelocity.z);
            target.hurtMarked = true;
        }
    }

    private static void broadcastSheathStrikeAnimation(LivingEntity attacker) {
        if (!(attacker.level() instanceof ServerLevel level)) {
            return;
        }

        PlaySheathStrikeAnimationPayload payload = new PlaySheathStrikeAnimationPayload(attacker.getId());
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(attacker) <= SHEATH_STRIKE_ANIMATION_RANGE_SQR
                    && ServerPlayNetworking.canSend(viewer, PlaySheathStrikeAnimationPayload.TYPE)) {
                ServerPlayNetworking.send(viewer, payload);
            }
        }
    }

    private static boolean pierceShieldIfBlocking(ItemStack stack, LivingEntity target) {
        ItemStack blockingWith = target.getItemBlockingWith();
        if (blockingWith == null || !blockingWith.is(Items.SHIELD)) {
            return false;
        }

        if (target instanceof Player player) {
            player.getCooldowns().addCooldown(blockingWith, katanaConfig().shieldDisableTicks);
        }

        target.stopUsingItem();
        return true;
    }

    private static boolean hurtTarget(LivingEntity target, DamageSource source, float damage) {
        return target.level() instanceof ServerLevel level && target.hurtServer(level, source, damage);
    }

    // uuid-based tracking lets the mixin resolve dash protection from the hurt entity alone
    private static void grantDashProtection(LivingEntity entity) {
        ACTIVE_DASH_PROTECTION_UNTIL.put(entity.getUUID(), entity.level().getGameTime() + katanaConfig().dashProtectionTicks);
    }

    private static boolean isDirectMeleeDamage(LivingEntity entity, DamageSource source) {
        return isDashProtected(entity)
                && !source.is(DamageTypeTags.IS_PROJECTILE)
                && source.getDirectEntity() instanceof LivingEntity;
    }

    private static boolean isDashProtected(LivingEntity entity) {
        Long protectedUntilTick = ACTIVE_DASH_PROTECTION_UNTIL.get(entity.getUUID());
        if (protectedUntilTick == null) {
            return false;
        }

        long currentGameTime = entity.level().getGameTime();
        if (currentGameTime > protectedUntilTick) {
            ACTIVE_DASH_PROTECTION_UNTIL.remove(entity.getUUID());
            return false;
        }

        return true;
    }
}
