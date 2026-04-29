package com.tweeks.thief.world;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HideoutPlacerTest {

    private static final BlockPos SPAWN = new BlockPos(0, 64, 0);

    @Test
    void rejectsCandidateOutsideYBand() {
        BlockPos tooHigh = new BlockPos(20, 75, 20);
        BlockPos justRight = new BlockPos(20, 70, 20);
        assertFalse(HideoutPlacer.isValidCandidate(SPAWN, tooHigh,
            allTrue(), allTrue(), allTrue(), noPois()));
        assertTrue(HideoutPlacer.isValidCandidate(SPAWN, justRight,
            allTrue(), allTrue(), allTrue(), noPois()));
    }

    @Test
    void rejectsCandidateNearVillagePoi() {
        BlockPos candidate = new BlockPos(20, 64, 20);
        // Predicate returns true if candidate IS near a POI (should reject)
        Predicate<BlockPos> nearVillagePoi = p -> p.equals(candidate);
        assertFalse(HideoutPlacer.isValidCandidate(SPAWN, candidate,
            allTrue(), allTrue(), allTrue(), nearVillagePoi));
    }

    @Test
    void requiresReplaceableBlock() {
        BlockPos candidate = new BlockPos(20, 64, 20);
        assertFalse(HideoutPlacer.isValidCandidate(SPAWN, candidate,
            alwaysFalse(), allTrue(), allTrue(), noPois()));
    }

    @Test
    void requiresOpaqueAbove() {
        BlockPos candidate = new BlockPos(20, 64, 20);
        assertFalse(HideoutPlacer.isValidCandidate(SPAWN, candidate,
            allTrue(), alwaysFalse(), allTrue(), noPois()));
    }

    @Test
    void requiresSolidBelow() {
        BlockPos candidate = new BlockPos(20, 64, 20);
        assertFalse(HideoutPlacer.isValidCandidate(SPAWN, candidate,
            allTrue(), allTrue(), alwaysFalse(), noPois()));
    }

    @Test
    void findCandidate_returnsPresentWhenOneValid() {
        // Create predicates that accept ANY position (all checks pass)
        Optional<BlockPos> result = HideoutPlacer.findValidCandidate(SPAWN,
            42L,
            allTrue(), allTrue(), allTrue(), noPois(),
            100);
        assertTrue(result.isPresent());
    }

    @Test
    void findCandidate_returnsEmptyWhenNoneValid() {
        Optional<BlockPos> result = HideoutPlacer.findValidCandidate(SPAWN,
            42L,
            alwaysFalse(), alwaysFalse(), alwaysFalse(), noPois(),
            20);
        assertEquals(Optional.empty(), result);
    }

    private static Predicate<BlockPos> allTrue() { return p -> true; }
    private static Predicate<BlockPos> alwaysFalse() { return p -> false; }
    private static Predicate<BlockPos> noPois() { return p -> false; }
}
