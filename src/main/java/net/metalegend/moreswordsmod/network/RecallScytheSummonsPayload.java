package net.metalegend.moreswordsmod.network;

import net.metalegend.moreswordsmod.MoreSwordsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// empty request packet that tells the server the client attempted bone scythe recall
public record RecallScytheSummonsPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RecallScytheSummonsPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MoreSwordsMod.MOD_ID, "recall_scythe_summons"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RecallScytheSummonsPayload> CODEC =
            StreamCodec.unit(new RecallScytheSummonsPayload());

    @Override
    public CustomPacketPayload.Type<RecallScytheSummonsPayload> type() {
        return TYPE;
    }
}
