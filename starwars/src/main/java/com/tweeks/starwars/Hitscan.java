package com.tweeks.starwars;

import java.util.List;
import java.util.Optional;

public final class Hitscan {
    private Hitscan() {}

    public record Candidate(String id, double distanceAlongRay) {}

    public static Optional<Candidate> firstHitWithinRange(
            double blockDistance, List<Candidate> candidates) {
        Candidate best = null;
        for (Candidate c : candidates) {
            if (c.distanceAlongRay() >= blockDistance) continue;
            if (best == null || c.distanceAlongRay() < best.distanceAlongRay()) {
                best = c;
            }
        }
        return Optional.ofNullable(best);
    }
}
