package com.tweeks.starwars.item;

import java.util.Optional;
import java.util.function.Predicate;

import com.mojang.logging.LogUtils;
import com.tweeks.starwars.network.S2COpenPlanetPickerPacket;
import com.tweeks.starwars.world.gate.GateShape;
import com.tweeks.starwars.world.gate.HyperspacePortalBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.network.PacketDistributor;

import org.slf4j.Logger;

/**
 * Ignites a hyperspace gate: use on an iron-block frame (any face of any
 * ring block, or the film of an already-lit gate to re-aim it) and, if the
 * frame is a valid {@link GateShape}, the planet picker radial opens; the
 * selection comes back over the network and fills the frame with portal film.
 */
public class StarCompassItem extends Item {

    private static final Logger LOGGER = LogUtils.getLogger();

    public StarCompassItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        var clickedState = level.getBlockState(clicked);
        boolean clickedFrame = clickedState.is(Blocks.IRON_BLOCK);
        boolean clickedFilm = clickedState.getBlock() instanceof HyperspacePortalBlock;
        if (!clickedFrame && !clickedFilm) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        Optional<GateShape.Result> shape;
        if (clickedFilm) {
            // Clicking the film re-aims an existing gate.
            shape = findShape(serverLevel, clicked);
        } else {
            // Prefer the clicked face, but accept a click on any face of any
            // ring block by probing the frame block's in-plane neighbours.
            BlockPos faceStart = clicked.relative(context.getClickedFace());
            shape = findShape(serverLevel, faceStart)
                .or(() -> GateShape.findNearFrame(clicked, framePredicate(serverLevel), interiorPredicate(serverLevel)));
        }
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.CONSUME;
        }
        if (shape.isEmpty()) {
            LOGGER.info("Star compass: no valid gate frame around {} (clicked face {})", clicked, context.getClickedFace());
            player.sendSystemMessage(Component.translatable("starwars.gate.invalid"), true);
            return InteractionResult.CONSUME;
        }
        GateShape.Result result = shape.get();
        LOGGER.info("Star compass: gate frame ok at {} (axis {}, {}x{}) — opening picker for {}",
            result.origin(), result.axis(), result.width(), result.height(), player.getName().getString());
        PacketDistributor.sendToPlayer(player, new S2COpenPlanetPickerPacket(
            result.origin(), result.axis() == Direction.Axis.X));
        return InteractionResult.CONSUME;
    }

    private static Predicate<BlockPos> framePredicate(ServerLevel level) {
        return pos -> level.getBlockState(pos).is(Blocks.IRON_BLOCK);
    }

    private static Predicate<BlockPos> interiorPredicate(ServerLevel level) {
        return pos -> {
            var state = level.getBlockState(pos);
            // Existing film counts as interior so a gate can be re-aimed.
            return state.isAir()
                || state.getBlock() instanceof HyperspacePortalBlock
                || state.canBeReplaced();
        };
    }

    /** Frame search shared with the select packet's server-side revalidation. */
    public static Optional<GateShape.Result> findShape(ServerLevel level, BlockPos start) {
        return GateShape.find(start, Direction.Axis.X, framePredicate(level), interiorPredicate(level))
            .or(() -> GateShape.find(start, Direction.Axis.Z, framePredicate(level), interiorPredicate(level)));
    }
}
