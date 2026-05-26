package net.metalegend.moreswordsmod;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.metalegend.moreswordsmod.client.KatanaSheathStrikeAnimation;
import net.metalegend.moreswordsmod.client.WindStaffGlideSoundController;
import net.metalegend.moreswordsmod.item.ModItems;
import net.metalegend.moreswordsmod.network.GarrisonScytheSummonsPayload;
import net.metalegend.moreswordsmod.network.PlaySheathStrikeAnimationPayload;
import net.metalegend.moreswordsmod.network.RecallScytheSummonsPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;

// client-only glue for rendering and input that vanilla does not already route through item callbacks (client side entrypoint)
public class MoreSwordsModClient implements ClientModInitializer {
    private static final KeyMapping.Category BONE_SCYTHE_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MoreSwordsMod.MOD_ID, "bone_scythe"));
    private static KeyMapping finalRecallKey;
    private static KeyMapping graveGarrisonKey;

    @Override
    public void onInitializeClient() {
        finalRecallKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.moreswordsmod.final_recall",
                InputConstants.Type.MOUSE,
                InputConstants.MOUSE_BUTTON_LEFT,
                BONE_SCYTHE_CATEGORY
        ));
        graveGarrisonKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.moreswordsmod.grave_garrison",
                InputConstants.Type.MOUSE,
                InputConstants.MOUSE_BUTTON_MIDDLE,
                BONE_SCYTHE_CATEGORY
        ));

        ClientPlayNetworking.registerGlobalReceiver(PlaySheathStrikeAnimationPayload.TYPE, (payload, context) ->
                context.client().execute(() -> KatanaSheathStrikeAnimation.start(payload.entityId())));
        ClientTickEvents.END_CLIENT_TICK.register(MoreSwordsModClient::handleBoneScytheCommandInput);
        ClientTickEvents.END_CLIENT_TICK.register(KatanaSheathStrikeAnimation::tick);
        ClientTickEvents.END_CLIENT_TICK.register(WindStaffGlideSoundController::tick);
    }

    // custom scythe inputs start client-side because empty-space clicks do not reach normal item hooks
    private static void handleBoneScytheCommandInput(Minecraft client) {
        if (client.player == null || client.level == null || client.screen != null) {
            return;
        }

        while (finalRecallKey.consumeClick()) {
            trySendFinalRecall(client);
        }

        while (graveGarrisonKey.consumeClick()) {
            trySendGraveGarrison(client);
        }
    }

    // the server revalidates the scythe and shift state before recalling anything
    private static void trySendFinalRecall(Minecraft client) {
        HitResult hitResult = client.hitResult;
        if (!client.player.isShiftKeyDown()
                || !isHoldingBoneScythe(client)
                || hitResult == null
                || hitResult.getType() != HitResult.Type.MISS
                || !ClientPlayNetworking.canSend(RecallScytheSummonsPayload.TYPE)) {
            return;
        }

        ClientPlayNetworking.send(new RecallScytheSummonsPayload());
    }

    // grave garrison is a direct command input so it does not depend on the crosshair hit result
    private static void trySendGraveGarrison(Minecraft client) {
        if (!isHoldingBoneScythe(client) || !ClientPlayNetworking.canSend(GarrisonScytheSummonsPayload.TYPE)) {
            return;
        }

        ClientPlayNetworking.send(new GarrisonScytheSummonsPayload());
    }

    private static boolean isHoldingBoneScythe(Minecraft client) {
        ItemStack mainHandStack = client.player.getMainHandItem();
        ItemStack offHandStack = client.player.getOffhandItem();
        return mainHandStack.is(ModItems.BONE_SCYTHE) || offHandStack.is(ModItems.BONE_SCYTHE);
    }
}
