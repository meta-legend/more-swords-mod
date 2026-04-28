package net.metalegend.moreswordsmod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.metalegend.moreswordsmod.item.custom.BoneScytheItem;

// central registration point for lightweight custom payloads used by non-vanilla input paths
public final class ModNetworking {
    private ModNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(RecallScytheSummonsPayload.TYPE, RecallScytheSummonsPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RecallScytheSummonsPayload.TYPE, (payload, context) ->
                BoneScytheItem.tryRecallActiveSummons(context.player()));
    }
}
