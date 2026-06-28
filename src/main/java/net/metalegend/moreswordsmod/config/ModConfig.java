package net.metalegend.moreswordsmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import net.metalegend.moreswordsmod.MoreSwordsMod;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModConfig {
    private static final String CONFIG_FILE_NAME = MoreSwordsMod.MOD_ID + ".json";
    private static final int CURRENT_SCHEMA_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig INSTANCE = new ModConfig();

    public int schemaVersion = CURRENT_SCHEMA_VERSION;
    public Katanas katanas = new Katanas();
    public ObsidianGreatsword obsidianGreatsword = new ObsidianGreatsword();
    public BoneScythe boneScythe = new BoneScythe();
    public LightningStaff lightningStaff = new LightningStaff();
    public WindStaff windStaff = new WindStaff();

    private ModConfig() {
    }

    public static ModConfig get() {
        return INSTANCE;
    }

    public static void load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);

        if (Files.notExists(configPath)) {
            INSTANCE = new ModConfig();
            INSTANCE.validate();
            writeConfig(configPath, INSTANCE);
            MoreSwordsMod.LOGGER.info("Created default config at " + configPath);
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath)) {
            ModConfig loadedConfig = GSON.fromJson(reader, ModConfig.class);
            if (loadedConfig == null) {
                MoreSwordsMod.LOGGER.warn("Config file " + configPath + " was empty. Using defaults.");
                loadedConfig = new ModConfig();
            }

            loadedConfig.ensureSections();
            loadedConfig.validate();
            INSTANCE = loadedConfig;
            MoreSwordsMod.LOGGER.info("Loaded config from " + configPath);
        } catch (IOException | JsonParseException exception) {
            MoreSwordsMod.LOGGER.error("Failed to load config from " + configPath + ". Using defaults.", exception);
            INSTANCE = new ModConfig();
            INSTANCE.validate();
        }
    }

    private static void writeConfig(Path configPath, ModConfig config) {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException exception) {
            MoreSwordsMod.LOGGER.error("Failed to write default config to " + configPath, exception);
        }
    }

    private void ensureSections() {
        if (katanas == null) {
            katanas = new Katanas();
        }
        if (katanas.copper == null) {
            katanas.copper = new KatanaStats(340, 13, 3, -1.9f);
        }
        if (katanas.iron == null) {
            katanas.iron = new KatanaStats(450, 14, 3, -2.0f);
        }
        if (katanas.gold == null) {
            katanas.gold = new KatanaStats(128, 22, 2, -1.6f);
        }
        if (katanas.diamond == null) {
            katanas.diamond = new KatanaStats(1561, 10, 4, -1.8f);
        }
        if (katanas.netherite == null) {
            katanas.netherite = new KatanaStats(2031, 15, 5, -1.7f);
        }
        if (obsidianGreatsword == null) {
            obsidianGreatsword = new ObsidianGreatsword();
        }
        if (boneScythe == null) {
            boneScythe = new BoneScythe();
        }
        if (lightningStaff == null) {
            lightningStaff = new LightningStaff();
        }
        if (windStaff == null) {
            windStaff = new WindStaff();
        }
    }

    private void validate() {
        ensureSections();
        schemaVersion = CURRENT_SCHEMA_VERSION;

        katanas.validate();
        obsidianGreatsword.validate();
        boneScythe.validate();
        lightningStaff.validate();
        windStaff.validate();
    }

    private static int intRange(String name, int value, int min, int max) {
        int clampedValue = Math.max(min, Math.min(max, value));
        if (clampedValue != value) {
            MoreSwordsMod.LOGGER.warn("Config value " + name + "=" + value + " is outside [" + min + ", " + max + "]. Using " + clampedValue + ".");
        }
        return clampedValue;
    }

    private static float floatRange(String name, float value, float min, float max) {
        if (!Float.isFinite(value)) {
            MoreSwordsMod.LOGGER.warn("Config value " + name + " is not finite. Using " + min + ".");
            return min;
        }

        float clampedValue = Math.max(min, Math.min(max, value));
        if (Float.compare(clampedValue, value) != 0) {
            MoreSwordsMod.LOGGER.warn("Config value " + name + "=" + value + " is outside [" + min + ", " + max + "]. Using " + clampedValue + ".");
        }
        return clampedValue;
    }

    private static double doubleRange(String name, double value, double min, double max) {
        if (!Double.isFinite(value)) {
            MoreSwordsMod.LOGGER.warn("Config value " + name + " is not finite. Using " + min + ".");
            return min;
        }

        double clampedValue = Math.max(min, Math.min(max, value));
        if (Double.compare(clampedValue, value) != 0) {
            MoreSwordsMod.LOGGER.warn("Config value " + name + "=" + value + " is outside [" + min + ", " + max + "]. Using " + clampedValue + ".");
        }
        return clampedValue;
    }

    public static final class Katanas {
        public KatanaStats copper = new KatanaStats(340, 13, 3, -1.9f);
        public KatanaStats iron = new KatanaStats(450, 14, 3, -2.0f);
        public KatanaStats gold = new KatanaStats(128, 22, 2, -1.6f);
        public KatanaStats diamond = new KatanaStats(1561, 10, 4, -1.8f);
        public KatanaStats netherite = new KatanaStats(2031, 15, 5, -1.7f);
        public int sheathStrikeCooldownTicks = 60;
        public float sheathStrikeDamageMultiplier = 1.5f;
        public int dashProtectionTicks = 8;
        public int shieldDisableTicks = 100;
        public float dashMeleeDamageMultiplier = 0.5f;
        public double sheathStrikeDashStrength = 1.45;
        public double airComboDashStrength = 0.55;
        public double airComboVerticalKnockback = 0.65;
        public double airComboPlayerVerticalKnockback = 0.35;
        public float diamondWeaknessChance = 0.25f;
        public int diamondWeaknessDurationTicks = 40;
        public float netheriteWeaknessChance = 0.33f;
        public int netheriteWeaknessDurationTicks = 60;

        private void validate() {
            copper.validate("katanas.copper");
            iron.validate("katanas.iron");
            gold.validate("katanas.gold");
            diamond.validate("katanas.diamond");
            netherite.validate("katanas.netherite");
            sheathStrikeCooldownTicks = intRange("katanas.sheathStrikeCooldownTicks", sheathStrikeCooldownTicks, 1, 1200);
            sheathStrikeDamageMultiplier = floatRange("katanas.sheathStrikeDamageMultiplier", sheathStrikeDamageMultiplier, 0.0f, 20.0f);
            dashProtectionTicks = intRange("katanas.dashProtectionTicks", dashProtectionTicks, 0, 200);
            shieldDisableTicks = intRange("katanas.shieldDisableTicks", shieldDisableTicks, 0, 1200);
            dashMeleeDamageMultiplier = floatRange("katanas.dashMeleeDamageMultiplier", dashMeleeDamageMultiplier, 0.0f, 1.0f);
            sheathStrikeDashStrength = doubleRange("katanas.sheathStrikeDashStrength", sheathStrikeDashStrength, 0.0, 10.0);
            airComboDashStrength = doubleRange("katanas.airComboDashStrength", airComboDashStrength, 0.0, 10.0);
            airComboVerticalKnockback = doubleRange("katanas.airComboVerticalKnockback", airComboVerticalKnockback, 0.0, 10.0);
            airComboPlayerVerticalKnockback = doubleRange("katanas.airComboPlayerVerticalKnockback", airComboPlayerVerticalKnockback, 0.0, 10.0);
            diamondWeaknessChance = floatRange("katanas.diamondWeaknessChance", diamondWeaknessChance, 0.0f, 1.0f);
            diamondWeaknessDurationTicks = intRange("katanas.diamondWeaknessDurationTicks", diamondWeaknessDurationTicks, 0, 1200);
            netheriteWeaknessChance = floatRange("katanas.netheriteWeaknessChance", netheriteWeaknessChance, 0.0f, 1.0f);
            netheriteWeaknessDurationTicks = intRange("katanas.netheriteWeaknessDurationTicks", netheriteWeaknessDurationTicks, 0, 1200);
        }
    }

    public static final class KatanaStats {
        public int durability;
        public int enchantability;
        public int attackDamage;
        public float attackSpeed;

        public KatanaStats() {
        }

        private KatanaStats(int durability, int enchantability, int attackDamage, float attackSpeed) {
            this.durability = durability;
            this.enchantability = enchantability;
            this.attackDamage = attackDamage;
            this.attackSpeed = attackSpeed;
        }

        private void validate(String prefix) {
            durability = intRange(prefix + ".durability", durability, 1, 100000);
            enchantability = intRange(prefix + ".enchantability", enchantability, 1, 100);
            attackDamage = intRange(prefix + ".attackDamage", attackDamage, 0, 1000);
            attackSpeed = floatRange(prefix + ".attackSpeed", attackSpeed, -10.0f, 10.0f);
        }
    }

    public static final class ObsidianGreatsword {
        public int durability = 768;
        public int enchantability = 10;
        public float attackDamage = 8.0f;
        public float attackSpeed = -3.2f;
        public float attackKnockback = 1.0f;
        public int attackDurabilityCost = 1;
        public float minimumGrandSlamAttackStrength = 0.65f;
        public int grandSlamCooldownTicks = 80;
        public int grandSlamDurabilityCost = 12;
        public double grandSlamRadius = 3.0;
        public float grandSlamDamage = 4.0f;
        public double grandSlamHorizontalKnockback = 1.5;
        public double grandSlamVerticalKnockback = 0.35;
        public float armorPenetrationRatio = 0.5f;

        private void validate() {
            durability = intRange("obsidianGreatsword.durability", durability, 1, 100000);
            enchantability = intRange("obsidianGreatsword.enchantability", enchantability, 1, 100);
            attackDamage = floatRange("obsidianGreatsword.attackDamage", attackDamage, 0.0f, 1000.0f);
            attackSpeed = floatRange("obsidianGreatsword.attackSpeed", attackSpeed, -10.0f, 10.0f);
            attackKnockback = floatRange("obsidianGreatsword.attackKnockback", attackKnockback, 0.0f, 100.0f);
            attackDurabilityCost = intRange("obsidianGreatsword.attackDurabilityCost", attackDurabilityCost, 0, 1000);
            minimumGrandSlamAttackStrength = floatRange("obsidianGreatsword.minimumGrandSlamAttackStrength", minimumGrandSlamAttackStrength, 0.0f, 1.0f);
            grandSlamCooldownTicks = intRange("obsidianGreatsword.grandSlamCooldownTicks", grandSlamCooldownTicks, 0, 12000);
            grandSlamDurabilityCost = intRange("obsidianGreatsword.grandSlamDurabilityCost", grandSlamDurabilityCost, 0, 1000);
            grandSlamRadius = doubleRange("obsidianGreatsword.grandSlamRadius", grandSlamRadius, 0.0, 64.0);
            grandSlamDamage = floatRange("obsidianGreatsword.grandSlamDamage", grandSlamDamage, 0.0f, 1000.0f);
            grandSlamHorizontalKnockback = doubleRange("obsidianGreatsword.grandSlamHorizontalKnockback", grandSlamHorizontalKnockback, 0.0, 50.0);
            grandSlamVerticalKnockback = doubleRange("obsidianGreatsword.grandSlamVerticalKnockback", grandSlamVerticalKnockback, 0.0, 50.0);
            armorPenetrationRatio = floatRange("obsidianGreatsword.armorPenetrationRatio", armorPenetrationRatio, 0.0f, 1.0f);
        }
    }

    public static final class BoneScythe {
        public int durability = 512;
        public int enchantability = 14;
        public float attackDamage = 4.0f;
        public float attackSpeed = -2.6f;
        public int attackDurabilityCost = 1;
        public int soulChargeCap = 10;
        public int soulImprintCap = 8;
        public float soulChargeChanceMultiplier = 1.0f;
        public int summonWindowTicks = 200;
        public int summonCooldownTicks = 240;
        public int summonDurabilityCost = 12;
        public double summonSpawnDistance = 1.5;
        public int maxActiveSummons = 3;
        public float playerWitherAttackStrengthThreshold = 0.9f;
        public float mobWitherChance = 0.25f;
        public int witherDurationTicks = 40;
        public int garrisonCooldownTicks = 60;
        public double garrisonTargetRange = 18.0;
        public int summonLifetimeTicks = 300;
        public int ossuaryLifetimeBonusTicks = 100;
        public float recallRefundMultiplier = 0.5f;
        public double targetRange = 16.0;
        public int maxReapedVexes = 2;
        public double vexBindRange = 24.0;
        public double summonHealthMultiplier = 1.0;
        public double summonAttackDamageMultiplier = 1.0;
        public double summonMovementSpeedMultiplier = 1.0;

        private void validate() {
            durability = intRange("boneScythe.durability", durability, 1, 100000);
            enchantability = intRange("boneScythe.enchantability", enchantability, 1, 100);
            attackDamage = floatRange("boneScythe.attackDamage", attackDamage, 0.0f, 1000.0f);
            attackSpeed = floatRange("boneScythe.attackSpeed", attackSpeed, -10.0f, 10.0f);
            attackDurabilityCost = intRange("boneScythe.attackDurabilityCost", attackDurabilityCost, 0, 1000);
            soulChargeCap = intRange("boneScythe.soulChargeCap", soulChargeCap, 1, 1000);
            soulImprintCap = intRange("boneScythe.soulImprintCap", soulImprintCap, 1, 1000);
            soulChargeChanceMultiplier = floatRange("boneScythe.soulChargeChanceMultiplier", soulChargeChanceMultiplier, 0.0f, 10.0f);
            summonWindowTicks = intRange("boneScythe.summonWindowTicks", summonWindowTicks, 1, 12000);
            summonCooldownTicks = intRange("boneScythe.summonCooldownTicks", summonCooldownTicks, 0, 12000);
            summonDurabilityCost = intRange("boneScythe.summonDurabilityCost", summonDurabilityCost, 0, 1000);
            summonSpawnDistance = doubleRange("boneScythe.summonSpawnDistance", summonSpawnDistance, 0.0, 16.0);
            maxActiveSummons = intRange("boneScythe.maxActiveSummons", maxActiveSummons, 1, 100);
            playerWitherAttackStrengthThreshold = floatRange("boneScythe.playerWitherAttackStrengthThreshold", playerWitherAttackStrengthThreshold, 0.0f, 1.0f);
            mobWitherChance = floatRange("boneScythe.mobWitherChance", mobWitherChance, 0.0f, 1.0f);
            witherDurationTicks = intRange("boneScythe.witherDurationTicks", witherDurationTicks, 0, 12000);
            garrisonCooldownTicks = intRange("boneScythe.garrisonCooldownTicks", garrisonCooldownTicks, 0, 12000);
            garrisonTargetRange = doubleRange("boneScythe.garrisonTargetRange", garrisonTargetRange, 0.0, 128.0);
            summonLifetimeTicks = intRange("boneScythe.summonLifetimeTicks", summonLifetimeTicks, 1, 72000);
            ossuaryLifetimeBonusTicks = intRange("boneScythe.ossuaryLifetimeBonusTicks", ossuaryLifetimeBonusTicks, 0, 72000);
            recallRefundMultiplier = floatRange("boneScythe.recallRefundMultiplier", recallRefundMultiplier, 0.0f, 1.0f);
            targetRange = doubleRange("boneScythe.targetRange", targetRange, 0.0, 128.0);
            maxReapedVexes = intRange("boneScythe.maxReapedVexes", maxReapedVexes, 0, 20);
            vexBindRange = doubleRange("boneScythe.vexBindRange", vexBindRange, 0.0, 128.0);
            summonHealthMultiplier = doubleRange("boneScythe.summonHealthMultiplier", summonHealthMultiplier, 0.01, 100.0);
            summonAttackDamageMultiplier = doubleRange("boneScythe.summonAttackDamageMultiplier", summonAttackDamageMultiplier, 0.0, 100.0);
            summonMovementSpeedMultiplier = doubleRange("boneScythe.summonMovementSpeedMultiplier", summonMovementSpeedMultiplier, 0.01, 10.0);
        }
    }

    public static final class LightningStaff {
        public int durability = 128;
        public int enchantability = 12;
        public int cooldownTicks = 60;
        public int durabilityCost = 3;
        public double boltRange = 16.0;
        public float entityRaycastMargin = 0.3f;

        private void validate() {
            durability = intRange("lightningStaff.durability", durability, 1, 100000);
            enchantability = intRange("lightningStaff.enchantability", enchantability, 1, 100);
            cooldownTicks = intRange("lightningStaff.cooldownTicks", cooldownTicks, 0, 12000);
            durabilityCost = intRange("lightningStaff.durabilityCost", durabilityCost, 0, 1000);
            boltRange = doubleRange("lightningStaff.boltRange", boltRange, 1.0, 256.0);
            entityRaycastMargin = floatRange("lightningStaff.entityRaycastMargin", entityRaycastMargin, 0.0f, 16.0f);
        }
    }

    public static final class WindStaff {
        public int durability = 192;
        public int enchantability = 14;
        public int combatCooldownTicks = 240;
        public int leapCooldownTicks = 40;
        public int combatDurabilityCost = 1;
        public int leapDurabilityCost = 1;
        public double glideMaxFallSpeed = -0.12;
        public float advancementGlideFallDistance = 5.0f;
        public double leapForwardStrength = 1.1;
        public double leapUpwardBonus = 0.45;
        public double entityLaunchHorizontal = 0.45;
        public double entityLaunchVertical = 0.6;
        public double playerLaunchHorizontal = 0.2;
        public double playerLaunchVertical = 0.3;
        public int targetLevitationDurationTicks = 20;
        public double mountBoostHorizontal = 1.5;
        public double mountBoostVertical = 0.55;
        public double mountBoostAirLift = 0.08;
        public int mountBoostProtectionTicks = 14;
        public int mountBoostVerticalProtectionTicks = 8;
        public double mountBoostSpeedDecay = 0.91;
        public double mountBoostVerticalSpeedDecay = 0.78;
        public double mountBoostMinProtectedSpeed = 0.25;
        public double mountBoostMinProtectedVerticalSpeed = 0.06;

        private void validate() {
            durability = intRange("windStaff.durability", durability, 1, 100000);
            enchantability = intRange("windStaff.enchantability", enchantability, 1, 100);
            combatCooldownTicks = intRange("windStaff.combatCooldownTicks", combatCooldownTicks, 0, 12000);
            leapCooldownTicks = intRange("windStaff.leapCooldownTicks", leapCooldownTicks, 0, 12000);
            combatDurabilityCost = intRange("windStaff.combatDurabilityCost", combatDurabilityCost, 0, 1000);
            leapDurabilityCost = intRange("windStaff.leapDurabilityCost", leapDurabilityCost, 0, 1000);
            glideMaxFallSpeed = doubleRange("windStaff.glideMaxFallSpeed", glideMaxFallSpeed, -10.0, 0.0);
            advancementGlideFallDistance = floatRange("windStaff.advancementGlideFallDistance", advancementGlideFallDistance, 0.0f, 1000.0f);
            leapForwardStrength = doubleRange("windStaff.leapForwardStrength", leapForwardStrength, 0.0, 20.0);
            leapUpwardBonus = doubleRange("windStaff.leapUpwardBonus", leapUpwardBonus, 0.0, 20.0);
            entityLaunchHorizontal = doubleRange("windStaff.entityLaunchHorizontal", entityLaunchHorizontal, 0.0, 20.0);
            entityLaunchVertical = doubleRange("windStaff.entityLaunchVertical", entityLaunchVertical, 0.0, 20.0);
            playerLaunchHorizontal = doubleRange("windStaff.playerLaunchHorizontal", playerLaunchHorizontal, 0.0, 20.0);
            playerLaunchVertical = doubleRange("windStaff.playerLaunchVertical", playerLaunchVertical, 0.0, 20.0);
            targetLevitationDurationTicks = intRange("windStaff.targetLevitationDurationTicks", targetLevitationDurationTicks, 0, 12000);
            mountBoostHorizontal = doubleRange("windStaff.mountBoostHorizontal", mountBoostHorizontal, 0.0, 20.0);
            mountBoostVertical = doubleRange("windStaff.mountBoostVertical", mountBoostVertical, 0.0, 20.0);
            mountBoostAirLift = doubleRange("windStaff.mountBoostAirLift", mountBoostAirLift, 0.0, 20.0);
            mountBoostProtectionTicks = intRange("windStaff.mountBoostProtectionTicks", mountBoostProtectionTicks, 0, 12000);
            mountBoostVerticalProtectionTicks = intRange("windStaff.mountBoostVerticalProtectionTicks", mountBoostVerticalProtectionTicks, 0, 12000);
            mountBoostSpeedDecay = doubleRange("windStaff.mountBoostSpeedDecay", mountBoostSpeedDecay, 0.0, 1.0);
            mountBoostVerticalSpeedDecay = doubleRange("windStaff.mountBoostVerticalSpeedDecay", mountBoostVerticalSpeedDecay, 0.0, 1.0);
            mountBoostMinProtectedSpeed = doubleRange("windStaff.mountBoostMinProtectedSpeed", mountBoostMinProtectedSpeed, 0.0, 20.0);
            mountBoostMinProtectedVerticalSpeed = doubleRange("windStaff.mountBoostMinProtectedVerticalSpeed", mountBoostMinProtectedVerticalSpeed, 0.0, 20.0);
        }
    }
}
