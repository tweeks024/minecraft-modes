package com.tweeks.starwars.bounty;

import com.tweeks.starwars.bounty.BountyContract.Template;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BountyContractTest {

    @Test
    void everyTemplateIsWellFormed() {
        for (Template t : BountyContract.TEMPLATES) {
            assertTrue(t.count() > 0, t.targetId());
            assertTrue(t.reward() > 0, t.targetId());
            assertTrue(t.targetId().startsWith("starwars:"), t.targetId());
        }
    }

    @Test
    void bucketSelectionIsDeterministicAndRotates() {
        assertSame(BountyContract.forBucket(7), BountyContract.forBucket(7));
        Set<Template> seen = new HashSet<>();
        for (long bucket = 0; bucket < 200; bucket++) {
            seen.add(BountyContract.forBucket(bucket));
        }
        // Rotation should surface most of the board over time.
        assertTrue(seen.size() >= BountyContract.TEMPLATES.size() - 1,
            "only " + seen.size() + " of " + BountyContract.TEMPLATES.size() + " templates ever posted");
    }

    @Test
    void acceptStartsAtFullCount() {
        Template t = BountyContract.TEMPLATES.get(0);
        BountyState s = BountyContract.accept(t);
        assertEquals(t.targetId(), s.targetId());
        assertEquals(t.count(), s.total());
        assertEquals(t.count(), s.remaining());
        assertEquals(t.reward(), s.reward());
        assertFalse(s.complete());
        assertEquals(0, s.killed());
    }

    @Test
    void matchingKillsDecrementToCompletion() {
        BountyState s = BountyContract.accept(new Template("starwars:wampa", "e", 2, 18));
        s = BountyContract.onKill(s, "starwars:stormtrooper"); // wrong target
        assertEquals(2, s.remaining());
        s = BountyContract.onKill(s, "starwars:wampa");
        assertEquals(1, s.remaining());
        assertEquals(1, s.killed());
        assertFalse(s.complete());
        s = BountyContract.onKill(s, "starwars:wampa");
        assertEquals(0, s.remaining());
        assertTrue(s.complete());
        // Overkill never goes negative.
        s = BountyContract.onKill(s, "starwars:wampa");
        assertEquals(0, s.remaining());
    }

    @Test
    void matchesGuardsNullAndCompletedState() {
        assertFalse(BountyContract.matches(null, "starwars:wampa"));
        BountyState done = new BountyState("starwars:wampa", 1, 0, 18);
        assertFalse(BountyContract.matches(done, "starwars:wampa"));
    }
}
