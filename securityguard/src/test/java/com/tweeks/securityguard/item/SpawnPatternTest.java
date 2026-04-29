package com.tweeks.securityguard.item;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnPatternTest {

    private static final BlockPos TOP = new BlockPos(0, 64, 0);

    private static Predicate<BlockPos> ironAt(BlockPos... positions) {
        Set<BlockPos> set = new HashSet<>(Set.of(positions));
        return set::contains;
    }

    private static Predicate<BlockPos> passableExcept(BlockPos... blocked) {
        Set<BlockPos> set = new HashSet<>(Set.of(blocked));
        return p -> !set.contains(p);
    }

    @Test
    void threeIronBelowAndTwoAirAbove_isValid() {
        Predicate<BlockPos> iron = ironAt(TOP, TOP.below(), TOP.below(2));
        Predicate<BlockPos> passable = passableExcept(TOP, TOP.below(), TOP.below(2));
        assertTrue(SpawnPattern.matches(iron, passable, TOP));
    }

    @Test
    void onlyTwoIron_invalid() {
        Predicate<BlockPos> iron = ironAt(TOP, TOP.below());  // missing top.below(2)
        Predicate<BlockPos> passable = passableExcept(TOP, TOP.below(), TOP.below(2));
        assertFalse(SpawnPattern.matches(iron, passable, TOP));
    }

    @Test
    void ironColumnButObstructedAbove_invalid() {
        Predicate<BlockPos> iron = ironAt(TOP, TOP.below(), TOP.below(2));
        Predicate<BlockPos> passable = passableExcept(TOP, TOP.below(), TOP.below(2), TOP.above());  // ceiling
        assertFalse(SpawnPattern.matches(iron, passable, TOP));
    }

    @Test
    void allAir_invalid() {
        Predicate<BlockPos> iron = ironAt();  // none
        Predicate<BlockPos> passable = passableExcept();  // everything passable
        assertFalse(SpawnPattern.matches(iron, passable, TOP));
    }

    @Test
    void mixedNonIronInColumn_invalid() {
        Predicate<BlockPos> iron = ironAt(TOP, TOP.below(2));  // gap in middle
        Predicate<BlockPos> passable = passableExcept(TOP, TOP.below(), TOP.below(2));
        assertFalse(SpawnPattern.matches(iron, passable, TOP));
    }
}
