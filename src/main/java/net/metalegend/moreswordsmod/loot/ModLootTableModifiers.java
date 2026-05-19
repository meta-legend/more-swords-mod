package net.metalegend.moreswordsmod.loot;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.metalegend.moreswordsmod.MoreSwordsMod;
import net.metalegend.moreswordsmod.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

// adds progression material drops to vanilla mob loot without replacing vanilla loot tables
public final class ModLootTableModifiers {
    private static final float WITHERED_RIB_DROP_CHANCE = 0.15f;
    private static final float SCATTERED_WINGS_DROP_CHANCE = 0.15f;
    private static final float LOOTING_DROP_CHANCE_BONUS_PER_LEVEL = 0.03f;

    private ModLootTableModifiers() {
    }

    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!source.isBuiltin()) {
                return;
            }

            EntityType.WITHER_SKELETON.getDefaultLootTable()
                    .filter(key::equals)
                    .ifPresent(lootTable -> addRareMaterialDrop(tableBuilder, registries, ModItems.WITHERED_RIB, WITHERED_RIB_DROP_CHANCE));
            EntityType.PHANTOM.getDefaultLootTable()
                    .filter(key::equals)
                    .ifPresent(lootTable -> addRareMaterialDrop(tableBuilder, registries, ModItems.SCATTERED_WINGS, SCATTERED_WINGS_DROP_CHANCE));
        });

        MoreSwordsMod.LOGGER.debug("Registering loot table modifiers for {}", MoreSwordsMod.MOD_ID);
    }

    private static void addRareMaterialDrop(LootTable.Builder tableBuilder, HolderLookup.Provider registries, Item item, float chance) {
        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0f))
                .when(LootItemRandomChanceWithEnchantedBonusCondition.randomChanceAndLootingBoost(registries, chance, LOOTING_DROP_CHANCE_BONUS_PER_LEVEL))
                .add(LootItem.lootTableItem(item));

        tableBuilder.withPool(pool);
    }
}
