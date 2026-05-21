package com.tweeks.wildwest.entity;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Pure-Java utility for counting entity-like objects by owner UUID.
 * Extracted from {@link ScytheSkeletonEntity} so it can be unit-tested
 * without booting the FML/NeoForge loader.
 */
public final class MinionCounter {

    private MinionCounter() {}

    /**
     * Generic counter helper, testable without a
     * {@link net.minecraft.server.level.ServerLevel}.
     * Counts items in {@code candidates} that are alive (per {@code aliveProbe})
     * and have an owner UUID matching {@code owner}.
     */
    public static <T> int countMatching(Iterable<T> candidates,
                                        UUID owner,
                                        Predicate<T> aliveProbe,
                                        Function<T, Optional<UUID>> ownerProbe) {
        int count = 0;
        for (T candidate : candidates) {
            if (!aliveProbe.test(candidate)) continue;
            if (ownerProbe.apply(candidate).filter(owner::equals).isPresent()) {
                count++;
            }
        }
        return count;
    }
}
