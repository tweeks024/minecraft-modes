package com.tweeks.wildwest.block;

import com.mojang.serialization.MapCodec;
import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.entity.projectile.CannonballEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cannon block. Horizontal facing + boolean loaded state. Right-click
 * empty cannon with gunpowder in main hand + iron nugget in inventory →
 * reloads (consumes both, sets loaded=true). Right-click loaded cannon
 * → fires {@link CannonballEntity} via {@link CannonFireGeometry}, sets loaded=false.
 *
 * <p>AI cannot use this {@link #useItemOn} method — that path is handled by
 * CannonOperateGoal (later task), which calls
 * {@link #aiFire(ServerLevel, BlockPos, BlockState, LivingEntity, Vec3)}.
 */
public class CannonBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<CannonBlock> CODEC = simpleCodec(CannonBlock::new);

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LOADED = BooleanProperty.create("loaded");

    public static final double MUZZLE_SPEED = 2.0;
    private static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.75, 1.0);

    public CannonBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(LOADED, false));
    }

    @Override
    protected MapCodec<? extends CannonBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LOADED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Cannon faces AWAY from the player (like a furnace).
        return this.defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite())
            .setValue(LOADED, false);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                          BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        boolean loaded = state.getValue(LOADED);

        if (level.isClientSide()) {
            // Always SUCCESS now: server-side either fires, reloads, or shows
            // a hint message — every interaction is meaningful.
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel sl)) return InteractionResult.PASS;

        if (loaded) {
            playerFire(sl, pos, state, player);
            return InteractionResult.CONSUME;
        }

        // Reload path. Show above-hotbar hint when ingredients are missing so
        // players can discover the recipe without consulting docs.
        if (!stack.is(Items.GUNPOWDER)) {
            player.sendOverlayMessage(
                Component.translatable("block.wildwest.cannon.needs_gunpowder"));
            return InteractionResult.CONSUME;
        }
        int ironSlot = findIronNuggetSlot(player);
        if (ironSlot < 0) {
            player.sendOverlayMessage(
                Component.translatable("block.wildwest.cannon.needs_iron_nugget"));
            return InteractionResult.CONSUME;
        }

        if (!player.isCreative()) {
            stack.shrink(1);
            player.getInventory().getItem(ironSlot).shrink(1);
        }

        sl.setBlockAndUpdate(pos, state.setValue(LOADED, true));
        sl.playSound(null, pos, SoundEvents.FIRECHARGE_USE,
            SoundSource.BLOCKS, 0.6f, 1.4f);
        return InteractionResult.CONSUME;
    }

    private static int findIronNuggetSlot(Player player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(Items.IRON_NUGGET)) return i;
        }
        return -1;
    }

    private static void playerFire(ServerLevel sl, BlockPos pos, BlockState state, LivingEntity owner) {
        fire(sl, pos, state, owner, null);
    }

    public static void aiFire(ServerLevel sl, BlockPos pos, BlockState state, LivingEntity owner,
                              Vec3 targetPos) {
        fire(sl, pos, state, owner, targetPos);
    }

    private static void fire(ServerLevel sl, BlockPos pos, BlockState state, LivingEntity owner,
                             Vec3 targetPos) {
        Direction facing = state.getValue(FACING);
        CannonState.Facing pureFacing = toPureFacing(facing);

        CannonFireGeometry.Vec3d targetVec = targetPos == null
            ? null
            : new CannonFireGeometry.Vec3d(targetPos.x, targetPos.y, targetPos.z);

        CannonFireGeometry.Result r = CannonFireGeometry.compute(
            pos.getX(), pos.getY(), pos.getZ(), pureFacing, MUZZLE_SPEED, targetVec, 0, 0, 0);

        CannonballEntity ball = ModEntities.CANNONBALL.get().create(sl, EntitySpawnReason.TRIGGERED);
        if (ball == null) return;
        ball.setPos(r.spawnX(), r.spawnY(), r.spawnZ());
        ball.setDeltaMovement(r.vx(), r.vy(), r.vz());
        ball.setOwner(owner);
        sl.addFreshEntity(ball);

        sl.setBlockAndUpdate(pos, state.setValue(LOADED, false));
        sl.sendParticles(ParticleTypes.LARGE_SMOKE,
            r.spawnX(), r.spawnY(), r.spawnZ(), 6, 0.1, 0.1, 0.1, 0.0);
        sl.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(),
            SoundSource.BLOCKS, 1.0f, 0.7f);
    }

    private static CannonState.Facing toPureFacing(Direction d) {
        return switch (d) {
            case NORTH -> CannonState.Facing.NORTH;
            case SOUTH -> CannonState.Facing.SOUTH;
            case EAST -> CannonState.Facing.EAST;
            case WEST -> CannonState.Facing.WEST;
            default -> CannonState.Facing.NORTH; // up/down can't happen for HorizontalDirectionalBlock
        };
    }
}
