package net.metalegend.moreswordsmod.soul;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.jspecify.annotations.Nullable;

// static tuning table for every summonable soul archetype
// profiles define what can be harvested and what the reconstructed summon should look
// like once spawned: tier summon weight charge chance stat overrides and any
// on-hit effect the summon should apply
public enum BoneScytheSoulProfile {
    SILVERFISH("silverfish", EntityType.SILVERFISH, 1, 1, 0.25f, 8.0, 2.75, 0.38, null, 0, 0),
    ENDERMITE("endermite", EntityType.ENDERMITE, 1, 1, 0.25f, 8.0, 3.0, 0.39, null, 0, 0),
    BABY_ZOMBIFIED_PIGLIN("baby_zombified_piglin", EntityType.ZOMBIFIED_PIGLIN, 1, 1, 0.25f, 12.0, 3.75, 0.40, null, 0, 0, true),
    BABY_HOGLIN("baby_hoglin", EntityType.HOGLIN, 1, 1, 0.25f, 14.0, 3.25, 0.38, null, 0, 0, true),

    ZOMBIE("zombie", EntityType.ZOMBIE, 2, 2, 0.35f, 18.0, 4.5, 0.30, null, 0, 0),
    BABY_ZOMBIE("baby_zombie", EntityType.ZOMBIE, 2, 2, 0.35f, 12.0, 3.75, 0.39, null, 0, 0, true),
    HUSK("husk", EntityType.HUSK, 2, 2, 0.35f, 19.0, 4.75, 0.30, null, 0, 0),
    BABY_HUSK("baby_husk", EntityType.HUSK, 2, 2, 0.35f, 13.0, 4.0, 0.39, null, 0, 0, true),
    DROWNED("drowned", EntityType.DROWNED, 2, 2, 0.35f, 20.0, 4.5, 0.28, null, 0, 0),
    BABY_DROWNED("baby_drowned", EntityType.DROWNED, 2, 2, 0.35f, 13.5, 3.75, 0.37, null, 0, 0, true),
    SKELETON("skeleton", EntityType.SKELETON, 2, 2, 0.35f, 17.0, 4.25, 0.31, null, 0, 0),
    SPIDER("spider", EntityType.SPIDER, 2, 2, 0.35f, 18.0, 4.75, 0.34, null, 0, 0),
    SLIME("slime", EntityType.SLIME, 2, 2, 0.35f, 24.0, 5.25, 0.29, null, 0, 0),

    BOGGED("bogged", EntityType.BOGGED, 3, 3, 0.45f, 18.0, 4.0, 0.31, MobEffects.POISON, 40, 0),
    CAVE_SPIDER("cave_spider", EntityType.CAVE_SPIDER, 3, 3, 0.45f, 16.0, 4.25, 0.35, MobEffects.POISON, 40, 0),
    STRAY("stray", EntityType.STRAY, 3, 3, 0.45f, 24.0, 5.75, 0.31, MobEffects.SLOWNESS, 60, 0),
    PARCHED("parched", EntityType.PARCHED, 3, 3, 0.45f, 22.0, 5.5, 0.31, MobEffects.WEAKNESS, 60, 0),
    PILLAGER("pillager", EntityType.PILLAGER, 3, 3, 0.45f, 23.0, 5.5, 0.30, null, 0, 0),
    PIGLIN("piglin", EntityType.PIGLIN, 3, 3, 0.45f, 24.0, 5.75, 0.31, null, 0, 0),
    ZOMBIFIED_PIGLIN("zombified_piglin", EntityType.ZOMBIFIED_PIGLIN, 3, 3, 0.45f, 25.0, 5.75, 0.31, null, 0, 0),
    MAGMA_CUBE("magma_cube", EntityType.MAGMA_CUBE, 3, 3, 0.45f, 25.0, 5.75, 0.29, null, 0, 0),
    WITHER_SKELETON("wither_skeleton", EntityType.WITHER_SKELETON, 3, 3, 0.45f, 32.0, 7.5, 0.32, MobEffects.WITHER, 40, 0),

    HOGLIN("hoglin", EntityType.HOGLIN, 4, 4, 0.55f, 28.0, 6.5, 0.31, null, 0, 0),
    ZOGLIN("zoglin", EntityType.ZOGLIN, 4, 4, 0.55f, 30.0, 7.0, 0.31, null, 0, 0),
    BLAZE("blaze", EntityType.BLAZE, 4, 4, 0.55f, 24.0, 6.25, 0.34, null, 0, 0),
    VINDICATOR("vindicator", EntityType.VINDICATOR, 4, 4, 0.55f, 28.0, 6.5, 0.31, null, 0, 0),

    PIGLIN_BRUTE("piglin_brute", EntityType.PIGLIN_BRUTE, 5, 5, 0.70f, 34.0, 8.0, 0.31, null, 0, 0),
    IRON_GOLEM("iron_golem", EntityType.IRON_GOLEM, 5, 5, 0.70f, 40.0, 9.0, 0.30, null, 0, 0),
    RAVAGER("ravager", EntityType.RAVAGER, 5, 5, 0.70f, 52.0, 10.0, 0.28, null, 0, 0),
    EVOKER("evoker", EntityType.EVOKER, 5, 5, 0.70f, 28.0, 5.0, 0.30, null, 0, 0);

    private final String id;
    private final EntityType<?> sourceType;
    private final int tier;
    private final int weight;
    private final float chargeChance;
    private final double maxHealth;
    private final double attackDamage;
    private final double movementSpeed;
    private final @Nullable Holder<MobEffect> hitEffect;
    private final int hitEffectDuration;
    private final int hitEffectAmplifier;
    private final boolean babyVariant;

    BoneScytheSoulProfile(
            String id,
            EntityType<?> sourceType,
            int tier,
            int weight,
            float chargeChance,
            double maxHealth,
            double attackDamage,
            double movementSpeed,
            @Nullable Holder<MobEffect> hitEffect,
            int hitEffectDuration,
            int hitEffectAmplifier
    ) {
        this(id, sourceType, tier, weight, chargeChance, maxHealth, attackDamage, movementSpeed, hitEffect, hitEffectDuration, hitEffectAmplifier, false);
    }

    BoneScytheSoulProfile(
            String id,
            EntityType<?> sourceType,
            int tier,
            int weight,
            float chargeChance,
            double maxHealth,
            double attackDamage,
            double movementSpeed,
            @Nullable Holder<MobEffect> hitEffect,
            int hitEffectDuration,
            int hitEffectAmplifier,
            boolean babyVariant
    ) {
        this.id = id;
        this.sourceType = sourceType;
        this.tier = tier;
        this.weight = weight;
        this.chargeChance = chargeChance;
        this.maxHealth = maxHealth;
        this.attackDamage = attackDamage;
        this.movementSpeed = movementSpeed;
        this.hitEffect = hitEffect;
        this.hitEffectDuration = hitEffectDuration;
        this.hitEffectAmplifier = hitEffectAmplifier;
        this.babyVariant = babyVariant;
    }

    public String id() {
        return id;
    }

    public int tier() {
        return tier;
    }

    public int weight() {
        return weight;
    }

    public float chargeChance() {
        return chargeChance;
    }

    public double maxHealth() {
        return maxHealth;
    }

    public double attackDamage() {
        return attackDamage;
    }

    public double movementSpeed() {
        return movementSpeed;
    }

    public @Nullable Holder<MobEffect> hitEffect() {
        return hitEffect;
    }

    public int hitEffectDuration() {
        return hitEffectDuration;
    }

    public int hitEffectAmplifier() {
        return hitEffectAmplifier;
    }

    public Component summonName() {
        return babyVariant
                ? Component.translatable("text.moreswordsmod.reaped_baby_soul_named", Component.translatable(sourceType.getDescriptionId()))
                : Component.translatable("text.moreswordsmod.reaped_soul_named", Component.translatable(sourceType.getDescriptionId()));
    }

    public EntityType<?> sourceType() {
        return sourceType;
    }

    public boolean babyVariant() {
        return babyVariant;
    }

    public static @Nullable BoneScytheSoulProfile fromId(String id) {
        for (BoneScytheSoulProfile profile : values()) {
            if (profile.id.equals(id)) {
                return profile;
            }
        }

        return null;
    }

    public static @Nullable BoneScytheSoulProfile fromHarvestTarget(LivingEntity target) {
        if (!(target instanceof Enemy || target instanceof Hoglin || target instanceof IronGolem)
                || target instanceof Warden
                || target instanceof WitherBoss
                || target instanceof EnderDragon) {
            return null;
        }

        boolean isBaby = isBabyHarvest(target);
        EntityType<?> type = target.getType();
        for (BoneScytheSoulProfile profile : values()) {
            if (profile.sourceType == type && profile.babyVariant == isBaby) {
                return profile;
            }
        }

        return null;
    }

    private static boolean isBabyHarvest(LivingEntity target) {
        return (target instanceof Zombie zombie && zombie.isBaby())
                || (target instanceof Hoglin hoglin && hoglin.isBaby());
    }
}
