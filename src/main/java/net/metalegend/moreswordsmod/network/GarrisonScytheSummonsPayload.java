package net.metalegend.moreswordsmod.network;

import net.metalegend.moreswordsmod.MoreSwordsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// empty request packet that tells the server the client attempted bone scythe garrison
public record GarrisonScytheSummonsPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GarrisonScytheSummonsPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MoreSwordsMod.MOD_ID, "garrison_scythe_summons"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GarrisonScytheSummonsPayload> CODEC =
            StreamCodec.unit(new GarrisonScytheSummonsPayload());

    @Override
    public CustomPacketPayload.Type<GarrisonScytheSummonsPayload> type() {
        return TYPE;
    }
}
