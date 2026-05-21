package com.tweeks.wildwest.entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link MinionCounter#countMatching}.
 * Uses lightweight test doubles instead of constructing a real
 * {@link net.minecraft.server.level.ServerLevel}.
 */
class ScytheSkeletonOwnerScanTest {

    private static final UUID OWNER_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_B = UUID.fromString("00000000-0000-0000-0000-000000000002");

    /** Lightweight stand-in matching the contract of `countMatching`. */
    private record FakeMinion(boolean alive, Optional<UUID> ownerUUID) {}

    @Test
    void emptyList_returnsZero() {
        int n = MinionCounter.countMatching(
            List.<FakeMinion>of(),
            OWNER_A,
            FakeMinion::alive,
            FakeMinion::ownerUUID);
        assertEquals(0, n);
    }

    @Test
    void noMatchingOwner_returnsZero() {
        int n = MinionCounter.countMatching(
            List.of(new FakeMinion(true, Optional.of(OWNER_B))),
            OWNER_A,
            FakeMinion::alive,
            FakeMinion::ownerUUID);
        assertEquals(0, n);
    }

    @Test
    void singleMatch_returnsOne() {
        int n = MinionCounter.countMatching(
            List.of(new FakeMinion(true, Optional.of(OWNER_A))),
            OWNER_A,
            FakeMinion::alive,
            FakeMinion::ownerUUID);
        assertEquals(1, n);
    }

    @Test
    void twoMatchesOneOther_returnsTwoForA_oneForB() {
        var list = List.of(
            new FakeMinion(true, Optional.of(OWNER_A)),
            new FakeMinion(true, Optional.of(OWNER_A)),
            new FakeMinion(true, Optional.of(OWNER_B)));
        assertEquals(2, MinionCounter.countMatching(
            list, OWNER_A, FakeMinion::alive, FakeMinion::ownerUUID));
        assertEquals(1, MinionCounter.countMatching(
            list, OWNER_B, FakeMinion::alive, FakeMinion::ownerUUID));
    }

    @Test
    void deadMinion_isNotCounted() {
        int n = MinionCounter.countMatching(
            List.of(new FakeMinion(false, Optional.of(OWNER_A))),
            OWNER_A,
            FakeMinion::alive,
            FakeMinion::ownerUUID);
        assertEquals(0, n);
    }

    @Test
    void minionWithEmptyOwner_isNotCounted() {
        int n = MinionCounter.countMatching(
            List.of(new FakeMinion(true, Optional.empty())),
            OWNER_A,
            FakeMinion::alive,
            FakeMinion::ownerUUID);
        assertEquals(0, n);
    }
}
