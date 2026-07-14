package com.tweeks.starwars.world.gate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GateShapeTest {

    /** Synthetic world: a set of frame positions; everything else is empty. */
    private static Predicate<BlockPos> frameOf(Set<BlockPos> frame) {
        return frame::contains;
    }

    private static Predicate<BlockPos> emptyExcept(Set<BlockPos> frame) {
        return pos -> !frame.contains(pos);
    }

    /** Ring around an interior of width x height, interior origin at (0, 0, 0). */
    private static Set<BlockPos> ring(Direction.Axis axis, int width, int height, boolean corners) {
        Set<BlockPos> frame = new HashSet<>();
        Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        BlockPos origin = BlockPos.ZERO;
        for (int a = -1; a <= width; a++) {
            for (int dy = -1; dy <= height; dy++) {
                boolean interior = a >= 0 && a < width && dy >= 0 && dy < height;
                boolean corner = (a == -1 || a == width) && (dy == -1 || dy == height);
                if (interior || (corner && !corners)) {
                    continue;
                }
                frame.add(origin.relative(right, a).above(dy));
            }
        }
        return frame;
    }

    @Test
    void findsMinimalFrameFromAnyInteriorCell() {
        Set<BlockPos> frame = ring(Direction.Axis.X, 2, 3, true);
        for (int a = 0; a < 2; a++) {
            for (int dy = 0; dy < 3; dy++) {
                BlockPos start = BlockPos.ZERO.relative(Direction.EAST, a).above(dy);
                Optional<GateShape.Result> result =
                    GateShape.find(start, Direction.Axis.X, frameOf(frame), emptyExcept(frame));
                assertTrue(result.isPresent(), "not found from " + start);
                assertEquals(BlockPos.ZERO, result.get().origin());
                assertEquals(2, result.get().width());
                assertEquals(3, result.get().height());
            }
        }
    }

    @Test
    void cornersAreOptional() {
        Set<BlockPos> frame = ring(Direction.Axis.Z, 4, 5, false);
        Optional<GateShape.Result> result =
            GateShape.find(BlockPos.ZERO.above(2), Direction.Axis.Z, frameOf(frame), emptyExcept(frame));
        assertTrue(result.isPresent());
        assertEquals(4, result.get().width());
        assertEquals(5, result.get().height());
        assertEquals(Direction.Axis.Z, result.get().axis());
    }

    @Test
    void largestAllowedFrameIsFound() {
        Set<BlockPos> frame = ring(Direction.Axis.X, GateShape.MAX_WIDTH, GateShape.MAX_HEIGHT, true);
        Optional<GateShape.Result> result =
            GateShape.find(BlockPos.ZERO, Direction.Axis.X, frameOf(frame), emptyExcept(frame));
        assertTrue(result.isPresent());
        assertEquals(GateShape.MAX_WIDTH, result.get().width());
        assertEquals(GateShape.MAX_HEIGHT, result.get().height());
    }

    @Test
    void holeInSideWallRejects() {
        Set<BlockPos> frame = ring(Direction.Axis.X, 2, 3, true);
        frame.remove(BlockPos.ZERO.relative(Direction.WEST).above(1)); // left wall hole
        assertTrue(GateShape.find(BlockPos.ZERO, Direction.Axis.X, frameOf(frame), emptyExcept(frame)).isEmpty());
    }

    @Test
    void holeInTopRejects() {
        Set<BlockPos> frame = ring(Direction.Axis.X, 3, 3, true);
        frame.remove(BlockPos.ZERO.relative(Direction.EAST, 1).above(3)); // lintel hole
        assertTrue(GateShape.find(BlockPos.ZERO, Direction.Axis.X, frameOf(frame), emptyExcept(frame)).isEmpty());
    }

    @Test
    void tooNarrowRejects() {
        Set<BlockPos> frame = ring(Direction.Axis.X, 1, 3, true);
        assertTrue(GateShape.find(BlockPos.ZERO, Direction.Axis.X, frameOf(frame), emptyExcept(frame)).isEmpty());
    }

    @Test
    void tooShortRejects() {
        Set<BlockPos> frame = ring(Direction.Axis.X, 2, 2, true);
        assertTrue(GateShape.find(BlockPos.ZERO, Direction.Axis.X, frameOf(frame), emptyExcept(frame)).isEmpty());
    }

    @Test
    void oversizeInteriorRejects() {
        Set<BlockPos> frame = ring(Direction.Axis.X, GateShape.MAX_WIDTH + 1, 3, true);
        assertTrue(GateShape.find(BlockPos.ZERO, Direction.Axis.X, frameOf(frame), emptyExcept(frame)).isEmpty());
    }

    @Test
    void openSkyAboveStartRejects() {
        // Floor and walls but no lintel anywhere: height scan runs off the top.
        Set<BlockPos> frame = ring(Direction.Axis.X, 2, GateShape.MAX_HEIGHT + 2, true);
        for (int a = 0; a < 2; a++) {
            frame.remove(BlockPos.ZERO.relative(Direction.EAST, a).above(GateShape.MAX_HEIGHT + 2));
        }
        assertTrue(GateShape.find(BlockPos.ZERO, Direction.Axis.X, frameOf(frame), emptyExcept(frame)).isEmpty());
    }

    @Test
    void noFloorWithinReachRejects() {
        // A start position floating far above any frame.
        Set<BlockPos> frame = ring(Direction.Axis.X, 2, 3, true);
        BlockPos floating = BlockPos.ZERO.above(GateShape.MAX_HEIGHT + 5);
        assertTrue(GateShape.find(floating, Direction.Axis.X, frameOf(frame), emptyExcept(frame)).isEmpty());
    }

    @Test
    void interiorPositionsEnumerateTheFullWindow() {
        Set<BlockPos> frame = ring(Direction.Axis.X, 3, 4, true);
        GateShape.Result result =
            GateShape.find(BlockPos.ZERO, Direction.Axis.X, frameOf(frame), emptyExcept(frame)).orElseThrow();
        assertEquals(12, result.interiorPositions().size());
        assertTrue(result.interiorPositions().contains(BlockPos.ZERO));
        assertTrue(result.interiorPositions().contains(BlockPos.ZERO.relative(Direction.EAST, 2).above(3)));
    }
}
