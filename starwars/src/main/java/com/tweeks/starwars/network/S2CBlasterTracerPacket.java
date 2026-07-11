package com.tweeks.starwars.network;

import com.tweeks.starwars.StarWarsMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public record S2CBlasterTracerPacket(Vec3 start, Vec3 end, int argbColor)
        implements CustomPacketPayload {

    /** Empire bolts render red, Light/player bolts blue. */
    public static final int COLOR_EMPIRE = 0xFFFF3020;
    public static final int COLOR_LIGHT = 0xFF3060FF;

    public static final Type<S2CBlasterTracerPacket> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "blaster_tracer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CBlasterTracerPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.DOUBLE, p -> p.start.x,
            ByteBufCodecs.DOUBLE, p -> p.start.y,
            ByteBufCodecs.DOUBLE, p -> p.start.z,
            ByteBufCodecs.DOUBLE, p -> p.end.x,
            ByteBufCodecs.DOUBLE, p -> p.end.y,
            ByteBufCodecs.DOUBLE, p -> p.end.z,
            ByteBufCodecs.INT, S2CBlasterTracerPacket::argbColor,
            (sx, sy, sz, ex, ey, ez, color) -> new S2CBlasterTracerPacket(
                new Vec3(sx, sy, sz), new Vec3(ex, ey, ez), color));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
