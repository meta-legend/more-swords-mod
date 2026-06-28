package net.metalegend.moreswordsmod.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.metalegend.moreswordsmod.item.custom.BoneScytheItem;

// central registration point for lightweight custom payloads used by non-vanilla input paths
public final class ModNetworking {
    private ModNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(RecallScytheSummonsPayload.TYPE, RecallScytheSummonsPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GarrisonScytheSummonsPayload.TYPE, GarrisonScytheSummonsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PlaySheathStrikeAnimationPayload.TYPE, PlaySheathStrikeAnimationPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RecallScytheSummonsPayload.TYPE, (payload, context) ->
                BoneScytheItem.tryRecallActiveSummons(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(GarrisonScytheSummonsPayload.TYPE, (payload, context) ->
                BoneScytheItem.tryGarrisonActiveSummons(context.player()));
    }
}
