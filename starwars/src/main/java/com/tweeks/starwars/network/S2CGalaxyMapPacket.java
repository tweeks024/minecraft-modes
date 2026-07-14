package com.tweeks.starwars.network;

import java.util.List;

import com.tweeks.starwars.StarWarsMod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server → client: everything the galaxy map screen shows. Planet order for
 * {@code visitedMask} bits and {@code gateCounts} indices is
 * {@code Planet.values()} ordinal order; {@code nearbyGates} are the closest
 * recorded gates in the player's CURRENT dimension, nearest first.
 */
public record S2CGalaxyMapPacket(int visitedMask, List<Integer> gateCounts,
                                 List<GateInfo> nearbyGates) implements CustomPacketPayload {

    /** One nearby gate: interior origin + destination planet ordinal. */
    public record GateInfo(BlockPos pos, int destinationOrdinal) {
        public static final StreamCodec<RegistryFriendlyByteBuf, GateInfo> STREAM_CODEC =
            StreamCodec.composite(
                BlockPos.STREAM_CODEC, GateInfo::pos,
                ByteBufCodecs.VAR_INT, GateInfo::destinationOrdinal,
                GateInfo::new);
    }

    public static final Type<S2CGalaxyMapPacket> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "galaxy_map"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CGalaxyMapPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, S2CGalaxyMapPacket::visitedMask,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), S2CGalaxyMapPacket::gateCounts,
            GateInfo.STREAM_CODEC.apply(ByteBufCodecs.list()), S2CGalaxyMapPacket::nearbyGates,
            S2CGalaxyMapPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
