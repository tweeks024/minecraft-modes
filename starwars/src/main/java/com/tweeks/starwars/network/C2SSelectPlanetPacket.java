package com.tweeks.starwars.network;

import java.util.Optional;

import com.mojang.logging.LogUtils;
import com.tweeks.starwars.Registration;
import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.item.StarCompassItem;
import com.tweeks.starwars.world.gate.GateShape;
import com.tweeks.starwars.world.gate.HyperspacePortalBlock;
import com.tweeks.starwars.world.gate.PortalRecords;
import com.tweeks.starwars.world.planet.Planet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import org.slf4j.Logger;

/**
 * Client → server: the player picked a destination wedge on the planet
 * picker. The server re-validates everything (range, frame shape, sensible
 * destination) before filling the frame with portal film — the client is
 * never trusted.
 */
public record C2SSelectPlanetPacket(BlockPos origin, boolean axisX, int wedge) implements CustomPacketPayload {

    public static final Type<C2SSelectPlanetPacket> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "select_planet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSelectPlanetPacket> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, C2SSelectPlanetPacket::origin,
            ByteBufCodecs.BOOL,    C2SSelectPlanetPacket::axisX,
            ByteBufCodecs.VAR_INT, C2SSelectPlanetPacket::wedge,
            C2SSelectPlanetPacket::new);

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double MAX_REACH_SQ = 20.0 * 20.0;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SSelectPlanetPacket pkt, IPayloadContext ctx) {
        Planet destination = Planet.byWedge(pkt.wedge());
        if (destination == null) {
            LOGGER.debug("Dropping C2SSelectPlanetPacket with out-of-range wedge={}", pkt.wedge());
            return;
        }
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                LOGGER.warn("C2SSelectPlanetPacket received without a ServerPlayer context");
                return;
            }
            ServerLevel level = player.level();
            if (player.distanceToSqr(pkt.origin().getCenter()) > MAX_REACH_SQ) {
                LOGGER.info("Gate ignition rejected: {} too far from gate at {}", player.getName().getString(), pkt.origin());
                return; // too far — stale or spoofed
            }
            if (destination.levelKey().equals(level.dimension())) {
                LOGGER.info("Gate ignition rejected: {} picked {} while already in it", player.getName().getString(), destination);
                player.sendSystemMessage(Component.translatable("starwars.gate.already_there"), true);
                return;
            }
            Optional<GateShape.Result> shape = StarCompassItem.findShape(level, pkt.origin());
            if (shape.isEmpty()) {
                LOGGER.info("Gate ignition rejected: frame at {} no longer valid", pkt.origin());
                player.sendSystemMessage(Component.translatable("starwars.gate.invalid"), true);
                return;
            }
            GateShape.Result result = shape.get();
            BlockState film = Registration.HYPERSPACE_PORTAL.get().defaultBlockState()
                .setValue(HyperspacePortalBlock.PLANET, destination)
                .setValue(HyperspacePortalBlock.AXIS, result.axis());
            int placed = 0;
            for (BlockPos pos : result.interiorPositions()) {
                if (!level.getBlockState(pos).equals(film)) {
                    level.setBlock(pos, film, Block.UPDATE_ALL);
                    placed++;
                }
            }
            LOGGER.info("Gate at {} locked to {} by {} ({} film blocks placed)",
                result.origin(), destination, player.getName().getString(), placed);
            PortalRecords.get(level).put(new PortalRecords.GateRecord(
                result.origin(), result.axis() == Direction.Axis.X, destination));
            level.playSound(null, pkt.origin(), SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.7F, 1.4F);
            player.sendSystemMessage(
                Component.translatable("starwars.gate.locked", Component.translatable(destination.translationKey())),
                true);
        });
    }
}
