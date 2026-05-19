package net.metalegend.moreswordsmod.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityType;
import net.metalegend.moreswordsmod.MoreSwordsMod;
import net.metalegend.moreswordsmod.entity.custom.ShieldTestDummyEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final EntityType<ShieldTestDummyEntity> SHIELD_TEST_DUMMY = registerLiving(
            "shield_test_dummy",
            FabricEntityType.Builder.createMob(
                            ShieldTestDummyEntity::new,
                            MobCategory.MISC,
                            builder -> builder.defaultAttributes(ShieldTestDummyEntity::createAttributes)
                    )
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(8)
                    .updateInterval(3)
    );
    private ModEntities() {
    }

    private static <T extends Mob> EntityType<T> registerLiving(
            String name,
            EntityType.Builder<T> builder
    ) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(
                Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(MoreSwordsMod.MOD_ID, name)
        );
        EntityType<T> type = builder.build(key);
        Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
        return type;
    }

    public static void registerModEntities() {
        MoreSwordsMod.LOGGER.debug("Registering Mod Entities For {}", MoreSwordsMod.MOD_ID);
    }
}
