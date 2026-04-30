package com.tweeks.wildwest.network;

import com.tweeks.wildwest.WildWestMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public record S2CTracerPacket(Vec3 start, Vec3 end) implements CustomPacketPayload {

    public static final Type<S2CTracerPacket> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "tracer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CTracerPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.DOUBLE, p -> p.start.x,
            ByteBufCodecs.DOUBLE, p -> p.start.y,
            ByteBufCodecs.DOUBLE, p -> p.start.z,
            ByteBufCodecs.DOUBLE, p -> p.end.x,
            ByteBufCodecs.DOUBLE, p -> p.end.y,
            ByteBufCodecs.DOUBLE, p -> p.end.z,
            (sx, sy, sz, ex, ey, ez) -> new S2CTracerPacket(
                new Vec3(sx, sy, sz), new Vec3(ex, ey, ez)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
