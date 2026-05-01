package com.tweeks.wildwest.entity.ai;

import java.util.List;
import java.util.Optional;

public final class FollowDecision {
    private FollowDecision() {}

    public interface Candidate {
        boolean isLeader();
        boolean isLawman();
        boolean isAlive();
    }

    public static <C extends Candidate> Optional<C> choose(
            boolean myFactionIsLawman,
            List<C> nearbyCandidates,
            C currentLeader) {
        if (currentLeader != null && currentLeader.isAlive()) {
            return Optional.of(currentLeader);
        }
        for (C c : nearbyCandidates) {
            if (!c.isLeader()) continue;
            if (c.isLawman() != myFactionIsLawman) continue;
            return Optional.of(c);
        }
        return Optional.empty();
    }
}
