package com.tweeks.starwars.world.gate;

import com.mojang.serialization.MapCodec;
import com.tweeks.starwars.world.planet.Planet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

/**
 * The hyperspace film inside an ignited gate frame. Carries its destination
 * in block state ({@link #PLANET}); riding the vanilla {@link Portal}
 * plumbing gives us transition delays, portal cooldowns and safe re-entry
 * semantics for free. The film dissolves nether-portal-style when its iron
 * frame is broken.
 */
public class HyperspacePortalBlock extends Block implements Portal {
    public static final MapCodec<HyperspacePortalBlock> CODEC = simpleCodec(HyperspacePortalBlock::new);
    public static final EnumProperty<Planet> PLANET = EnumProperty.create("planet", Planet.class);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    private static final VoxelShape X_SHAPE = Block.box(0.0, 0.0, 6.0, 16.0, 16.0, 10.0);
    private static final VoxelShape Z_SHAPE = Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 16.0);

    private static final int PLAYER_TRANSITION_TICKS = 20;
    private static final int MOB_TRANSITION_TICKS = 40;

    public HyperspacePortalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(PLANET, Planet.HOME)
            .setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PLANET, AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.Z ? Z_SHAPE : X_SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return entity instanceof Player ? PLAYER_TRANSITION_TICKS : MOB_TRANSITION_TICKS;
    }

    @Override
    public @Nullable TeleportTransition getPortalDestination(ServerLevel currentLevel, Entity entity, BlockPos portalEntryPos) {
        BlockState state = currentLevel.getBlockState(portalEntryPos);
        if (!(state.getBlock() instanceof HyperspacePortalBlock)) {
            return null;
        }
        Planet destination = state.getValue(PLANET);
        if (destination.levelKey().equals(currentLevel.dimension())) {
            return null; // gate aimed at the world it already stands in
        }
        ServerLevel targetLevel = currentLevel.getServer().getLevel(destination.levelKey());
        if (targetLevel == null) {
            return null;
        }
        Planet origin = Planet.byLevel(currentLevel.dimension());
        boolean axisX = state.getValue(AXIS) == Direction.Axis.X;
        return PortalLink.exitFor(targetLevel, entity, origin == null ? Planet.HOME : origin, axisX);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState,
                                     RandomSource random) {
        // The film only cares about its own plane (the gate axis + vertical):
        // losing an in-plane neighbour that is neither film nor frame means
        // the ring is broken, and the dissolve cascades through the film.
        Direction.Axis planeAxis = state.getValue(AXIS);
        boolean inPlane = directionToNeighbour.getAxis() == Direction.Axis.Y
            || directionToNeighbour.getAxis() == planeAxis;
        if (inPlane && !neighbourState.is(this) && !neighbourState.is(Blocks.IRON_BLOCK)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) != 0) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z,
                (random.nextDouble() - 0.5) * 0.3, random.nextDouble() * 0.2, (random.nextDouble() - 0.5) * 0.3);
        }
    }
}
