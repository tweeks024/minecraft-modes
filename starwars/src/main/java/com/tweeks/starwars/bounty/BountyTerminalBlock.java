package com.tweeks.starwars.bounty;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The cantina bounty board. Right-click to take the currently posted bounty,
 * check progress, or — once your target is eliminated — collect your credits.
 * Sneak-click to abandon an unfinished contract. All state lives on the
 * player ({@link BountyEvents}); the block is just the interface.
 */
public class BountyTerminalBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<BountyTerminalBlock> CODEC = simpleCodec(BountyTerminalBlock::new);

    /** Rotation period of the posted bounty: 10 game minutes. */
    private static final long ROTATION_TICKS = 12000L;

    public BountyTerminalBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        BountyState current = BountyEvents.get(serverPlayer);

        if (player.isShiftKeyDown()) {
            if (current != null && !current.complete()) {
                BountyEvents.clear(serverPlayer);
                serverPlayer.sendSystemMessage(Component.translatable("starwars.bounty.abandoned"), true);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.CONSUME;
        }

        if (current == null) {
            // Take the posted contract.
            long bucket = level.getGameTime() / ROTATION_TICKS;
            BountyContract.Template posted = BountyContract.forBucket(bucket);
            BountyEvents.set(serverPlayer, BountyContract.accept(posted));
            level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.6F, 1.2F);
            serverPlayer.sendSystemMessage(Component.translatable("starwars.bounty.accepted",
                posted.count(), Component.translatable(posted.nameKey()), posted.reward()), false);
            return InteractionResult.CONSUME;
        }

        if (!current.complete()) {
            serverPlayer.sendSystemMessage(Component.translatable("starwars.bounty.status",
                current.killed(), current.total(), current.reward()), false);
            return InteractionResult.CONSUME;
        }

        // Complete — pay out in credits and clear.
        payOut(serverPlayer, current.reward());
        BountyEvents.clear(serverPlayer);
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6F, 1.4F);
        serverPlayer.sendSystemMessage(Component.translatable("starwars.bounty.collected", current.reward()), false);
        return InteractionResult.CONSUME;
    }

    private static void payOut(ServerPlayer player, int credits) {
        ItemStack reward = new ItemStack(com.tweeks.starwars.Registration.CREDIT.get(), credits);
        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
    }
}
