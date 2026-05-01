package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.ai.FollowDecision.Candidate;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FollowDecisionTest {

    static class TestCandidate implements Candidate {
        private final boolean leader;
        private final boolean lawman;
        private final boolean alive;
        private final String label;

        TestCandidate(String label, boolean leader, boolean lawman, boolean alive) {
            this.label = label;
            this.leader = leader;
            this.lawman = lawman;
            this.alive = alive;
        }

        @Override public boolean isLeader() { return leader; }
        @Override public boolean isLawman() { return lawman; }
        @Override public boolean isAlive() { return alive; }
        @Override public String toString() { return label; }
    }

    @Test
    void emptyList_noCurrentLeader_returnsEmpty() {
        Optional<Candidate> result = FollowDecision.choose(true, List.of(), null);
        assertTrue(result.isEmpty());
    }

    @Test
    void onlyFollowers_returnsEmpty() {
        Candidate dep = new TestCandidate("deputy", false, true, true);
        Optional<Candidate> result = FollowDecision.choose(true, List.of(dep), null);
        assertTrue(result.isEmpty());
    }

    @Test
    void oneSameFactionLeader_picksThatLeader() {
        Candidate sherrif = new TestCandidate("sherrif", true, true, true);
        Optional<Candidate> result = FollowDecision.choose(true, List.of(sherrif), null);
        assertEquals("sherrif", result.orElseThrow().toString());
    }

    @Test
    void crossFactionLeader_returnsEmpty() {
        Candidate banditLeader = new TestCandidate("bandit_leader", true, false, true);
        Optional<Candidate> result = FollowDecision.choose(true, List.of(banditLeader), null);
        assertTrue(result.isEmpty());
    }

    @Test
    void multipleSameFactionLeaders_picksFirstInList() {
        Candidate near = new TestCandidate("near", true, true, true);
        Candidate far  = new TestCandidate("far",  true, true, true);
        Optional<Candidate> result = FollowDecision.choose(true, List.of(near, far), null);
        assertEquals("near", result.orElseThrow().toString());
    }

    @Test
    void alreadyFollowingAliveLeader_keepsCurrentLeader() {
        Candidate current = new TestCandidate("current", true, true, true);
        Candidate other = new TestCandidate("other", true, true, true);
        Optional<Candidate> result = FollowDecision.choose(true, List.of(other), current);
        assertEquals("current", result.orElseThrow().toString());
    }

    @Test
    void currentLeaderDead_picksNewNearest() {
        Candidate dead = new TestCandidate("dead", true, true, false);
        Candidate alive = new TestCandidate("alive", true, true, true);
        Optional<Candidate> result = FollowDecision.choose(true, List.of(alive), dead);
        assertEquals("alive", result.orElseThrow().toString());
    }
}
