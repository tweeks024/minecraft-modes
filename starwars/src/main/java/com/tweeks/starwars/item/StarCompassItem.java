package com.tweeks.starwars.item;

import java.util.Optional;
import java.util.function.Predicate;

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

/**
 * Ignites a hyperspace gate: use on an iron-block frame and, if the frame is
 * a valid {@link GateShape}, the planet picker radial opens; the selection
 * comes back over the network and fills the frame with portal film.
 */
public class StarCompassItem extends Item {

    public StarCompassItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        if (!level.getBlockState(clicked).is(Blocks.IRON_BLOCK)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos start = clicked.relative(context.getClickedFace());
        Optional<GateShape.Result> shape = findShape(serverLevel, start);
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.CONSUME;
        }
        if (shape.isEmpty()) {
            player.sendSystemMessage(Component.translatable("starwars.gate.invalid"), true);
            return InteractionResult.CONSUME;
        }
        GateShape.Result result = shape.get();
        PacketDistributor.sendToPlayer(player, new S2COpenPlanetPickerPacket(
            result.origin(), result.axis() == Direction.Axis.X));
        return InteractionResult.CONSUME;
    }

    /** Frame search shared with the select packet's server-side revalidation. */
    public static Optional<GateShape.Result> findShape(ServerLevel level, BlockPos start) {
        Predicate<BlockPos> isFrame = pos -> level.getBlockState(pos).is(Blocks.IRON_BLOCK);
        Predicate<BlockPos> isInterior = pos -> {
            var state = level.getBlockState(pos);
            // Existing film counts as interior so a gate can be re-aimed.
            return state.isAir()
                || state.getBlock() instanceof HyperspacePortalBlock
                || state.canBeReplaced();
        };
        return GateShape.find(start, Direction.Axis.X, isFrame, isInterior)
            .or(() -> GateShape.find(start, Direction.Axis.Z, isFrame, isInterior));
    }
}
