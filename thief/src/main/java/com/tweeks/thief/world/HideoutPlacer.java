package com.tweeks.thief.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Finds and places a Thief's hideout chest.
 *
 * <p>The {@link #place(ServerLevel, BlockPos)} entry-point is impure (touches
 * the world); the {@link #isValidCandidate} and {@link #findValidCandidate}
 * helpers take pure predicates so they can be unit-tested.
 */
public final class HideoutPlacer {

    private static final int MIN_RADIUS = 16;
    private static final int MAX_RADIUS = 32;
    private static final int MAX_Y_DELTA = 10;
    private static final int POI_EXCLUSION_RADIUS = 8;
    private static final int MAX_ATTEMPTS = 20;

    private HideoutPlacer() {}

    /** Real entry-point: tries to place a chest near {@code spawnPos} and returns the position if successful. */
    public static Optional<BlockPos> place(ServerLevel level, BlockPos spawnPos) {
        PoiManager poi = level.getPoiManager();
        Predicate<BlockPos> isReplaceable = p -> level.getBlockState(p).isAir();
        Predicate<BlockPos> opaqueAbove = p -> level.getBlockState(p.above()).isSolidRender();
        Predicate<BlockPos> solidBelow = p -> level.getBlockState(p.below()).isSolid();
        Predicate<BlockPos> nearVillagePoi = p ->
            poi.getCountInRange(holder -> true, p, POI_EXCLUSION_RADIUS,
                PoiManager.Occupancy.ANY) > 0;

        Optional<BlockPos> chosen = findValidCandidate(spawnPos, level.getRandom().nextLong(),
            isReplaceable, opaqueAbove, solidBelow, nearVillagePoi, MAX_ATTEMPTS);

        chosen.ifPresent(pos -> {
            BlockState chest = Blocks.CHEST.defaultBlockState()
                .setValue(ChestBlock.FACING, Direction.from2DDataValue(level.getRandom().nextInt(4)));
            level.setBlock(pos, chest, 3);
        });
        return chosen;
    }

    /** Pure: rolls candidates around {@code spawnPos} and returns the first that passes all predicates. */
    public static Optional<BlockPos> findValidCandidate(BlockPos spawnPos,
                                                        long seed,
                                                        Predicate<BlockPos> isReplaceable,
                                                        Predicate<BlockPos> opaqueAbove,
                                                        Predicate<BlockPos> solidBelow,
                                                        Predicate<BlockPos> nearVillagePoi,
                                                        int maxAttempts) {
        Random rng = new Random(seed);
        for (int i = 0; i < maxAttempts; i++) {
            int radius = MIN_RADIUS + rng.nextInt(MAX_RADIUS - MIN_RADIUS + 1);
            double angle = rng.nextDouble() * Math.PI * 2.0;
            int dx = (int) Math.round(Math.cos(angle) * radius);
            int dz = (int) Math.round(Math.sin(angle) * radius);
            int dy = rng.nextInt(MAX_Y_DELTA * 2 + 1) - MAX_Y_DELTA;
            BlockPos candidate = spawnPos.offset(dx, dy, dz);
            if (isValidCandidate(spawnPos, candidate, isReplaceable, opaqueAbove, solidBelow, nearVillagePoi)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /** Pure: a single candidate passes if it's replaceable, has cover above, support below, and is away from POIs. */
    public static boolean isValidCandidate(BlockPos spawnPos,
                                           BlockPos candidate,
                                           Predicate<BlockPos> isReplaceable,
                                           Predicate<BlockPos> opaqueAbove,
                                           Predicate<BlockPos> solidBelow,
                                           Predicate<BlockPos> nearVillagePoi) {
        if (Math.abs(candidate.getY() - spawnPos.getY()) > MAX_Y_DELTA) return false;
        if (!isReplaceable.test(candidate)) return false;
        if (!opaqueAbove.test(candidate)) return false;
        if (!solidBelow.test(candidate)) return false;
        if (nearVillagePoi.test(candidate)) return false;
        return true;
    }
}
