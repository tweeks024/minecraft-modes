package com.tweeks.starwars.world.gate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Finds a rectangular hyperspace-gate frame around a starting interior
 * position, nether-portal style: a ring of frame blocks (iron) in a vertical
 * plane with an empty interior between {@link #MIN_WIDTH}x{@link #MIN_HEIGHT}
 * and {@link #MAX_WIDTH}x{@link #MAX_HEIGHT}. Corners are not required.
 *
 * <p>World access is abstracted behind two predicates so the search is pure
 * and unit-testable against synthetic grids.
 */
public final class GateShape {
    public static final int MIN_WIDTH = 2;
    public static final int MAX_WIDTH = 8;
    public static final int MIN_HEIGHT = 3;
    public static final int MAX_HEIGHT = 8;

    /** A found frame: {@code origin} is the bottom-left interior cell. */
    public record Result(Direction.Axis axis, BlockPos origin, int width, int height) {
        /** Every interior cell, bottom-to-top, left-to-right. */
        public List<BlockPos> interiorPositions() {
            List<BlockPos> positions = new ArrayList<>(width * height);
            Direction right = alongAxis(axis);
            for (int dy = 0; dy < height; dy++) {
                for (int dw = 0; dw < width; dw++) {
                    positions.add(origin.relative(right, dw).above(dy));
                }
            }
            return positions;
        }
    }

    private GateShape() {
    }

    static Direction alongAxis(Direction.Axis axis) {
        return axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
    }

    /**
     * Player-friendly entry point: the player clicked some face of a frame
     * block — probe every in-plane neighbour of that block (both axes) so a
     * click on ANY face of ANY ring block finds the gate. Only free-standing
     * corner blocks have no interior neighbour and stay unignitable.
     */
    public static Optional<Result> findNearFrame(BlockPos frameBlock,
                                                 Predicate<BlockPos> isFrame, Predicate<BlockPos> isInterior) {
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            Direction along = alongAxis(axis);
            for (Direction dir : new Direction[]{Direction.UP, along, along.getOpposite(), Direction.DOWN}) {
                BlockPos start = frameBlock.relative(dir);
                if (isInterior.test(start)) {
                    Optional<Result> result = find(start, axis, isFrame, isInterior);
                    if (result.isPresent()) {
                        return result;
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Searches the vertical plane through {@code start} along {@code axis}.
     * {@code isInterior} decides what counts as fillable interior (air,
     * replaceable plants, existing portal film for re-aiming); {@code isFrame}
     * decides what counts as frame material.
     */
    public static Optional<Result> find(BlockPos start, Direction.Axis axis,
                                        Predicate<BlockPos> isFrame, Predicate<BlockPos> isInterior) {
        if (!isInterior.test(start)) {
            return Optional.empty();
        }
        Direction right = alongAxis(axis);
        Direction left = right.getOpposite();

        // Fall to the frame floor.
        BlockPos bottom = start;
        int drop = 0;
        while (isInterior.test(bottom.below())) {
            bottom = bottom.below();
            if (++drop > MAX_HEIGHT) {
                return Optional.empty();
            }
        }
        // Slide to the left frame wall.
        BlockPos origin = bottom;
        int slide = 0;
        while (isInterior.test(origin.relative(left))) {
            origin = origin.relative(left);
            if (++slide > MAX_WIDTH) {
                return Optional.empty();
            }
        }
        // Measure width along the bottom row.
        int width = 1;
        while (width <= MAX_WIDTH && isInterior.test(origin.relative(right, width))) {
            width++;
        }
        if (width < MIN_WIDTH || width > MAX_WIDTH) {
            return Optional.empty();
        }
        // Measure height: full rows of interior cells.
        int height = 1;
        heightScan:
        while (height <= MAX_HEIGHT) {
            for (int dw = 0; dw < width; dw++) {
                if (!isInterior.test(origin.relative(right, dw).above(height))) {
                    break heightScan;
                }
            }
            height++;
        }
        if (height < MIN_HEIGHT || height > MAX_HEIGHT) {
            return Optional.empty();
        }
        // Ring check: frame under and over every column, frame beside every row.
        for (int dw = 0; dw < width; dw++) {
            BlockPos column = origin.relative(right, dw);
            if (!isFrame.test(column.below()) || !isFrame.test(column.above(height))) {
                return Optional.empty();
            }
        }
        for (int dy = 0; dy < height; dy++) {
            if (!isFrame.test(origin.relative(left).above(dy))
                || !isFrame.test(origin.relative(right, width).above(dy))) {
                return Optional.empty();
            }
        }
        return Optional.of(new Result(axis, origin.immutable(), width, height));
    }
}
