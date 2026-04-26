package net.metalegend.moreswordsmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.metalegend.moreswordsmod.entity.ModEntities;
import net.minecraft.client.renderer.entity.ZombieRenderer;

public class ExampleModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.SHIELD_TEST_DUMMY, ZombieRenderer::new);
    }
}
