package net.metalegend.moreswordsmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.metalegend.moreswordsmod.entity.ModEntities;
import net.metalegend.moreswordsmod.item.ModItems;
import net.metalegend.moreswordsmod.network.RecallScytheSummonsPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;

// client-only glue for rendering and input that vanilla does not already route through item callbacks
public class MoreSwordsModClient implements ClientModInitializer {
    private static boolean recallAttackHeld;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.SHIELD_TEST_DUMMY, ZombieRenderer::new);
        ClientTickEvents.END_CLIENT_TICK.register(MoreSwordsModClient::handleScytheRecallInput);
    }

    // empty-space left-click does not become a normal server item hook so recall has to
    // start as a client input check and then be revalidated server-side when the packet arrives
    private static void handleScytheRecallInput(Minecraft client) {
        boolean attackPressed = client.options.keyAttack.isDown();

        if (!attackPressed) {
            recallAttackHeld = false;
            return;
        }

        if (recallAttackHeld || client.player == null || client.level == null || client.screen != null) {
            return;
        }

        recallAttackHeld = true;

        ItemStack mainHandStack = client.player.getMainHandItem();
        HitResult hitResult = client.hitResult;
        if (!client.player.isShiftKeyDown()
                || !mainHandStack.is(ModItems.BONE_SCYTHE)
                || hitResult == null
                || hitResult.getType() != HitResult.Type.MISS
                || !ClientPlayNetworking.canSend(RecallScytheSummonsPayload.TYPE)) {
            return;
        }

        ClientPlayNetworking.send(new RecallScytheSummonsPayload());
    }
}
