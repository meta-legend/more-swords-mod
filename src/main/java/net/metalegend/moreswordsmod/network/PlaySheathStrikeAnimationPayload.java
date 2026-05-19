package net.metalegend.moreswordsmod.network;

import net.metalegend.moreswordsmod.MoreSwordsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// tiny clientbound event that tells clients which entity should play the sheath strike animation
public record PlaySheathStrikeAnimationPayload(int entityId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlaySheathStrikeAnimationPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(MoreSwordsMod.MOD_ID, "play_sheath_strike_animation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlaySheathStrikeAnimationPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    PlaySheathStrikeAnimationPayload::entityId,
                    PlaySheathStrikeAnimationPayload::new
            );

    @Override
    public CustomPacketPayload.Type<PlaySheathStrikeAnimationPayload> type() {
        return TYPE;
    }
}
