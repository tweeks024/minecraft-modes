package com.tweeks.starwars.network;

import com.tweeks.starwars.StarWarsMod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → client: the star compass found a valid gate frame; open the
 * planet picker for it. {@code origin} is the frame's interior bottom-left
 * cell, echoed back in {@link C2SSelectPlanetPacket}.
 */
public record S2COpenPlanetPickerPacket(BlockPos origin, boolean axisX) implements CustomPacketPayload {

    public static final Type<S2COpenPlanetPickerPacket> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "open_planet_picker"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2COpenPlanetPickerPacket> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, S2COpenPlanetPickerPacket::origin,
            ByteBufCodecs.BOOL,    S2COpenPlanetPickerPacket::axisX,
            S2COpenPlanetPickerPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
