package com.tweeks.wildwest.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Constants smoke test. Catches accidental drift of cooldown / cap /
 * range / damage values that the spec promises. Full summon flow is
 * exercised manually in dev-client.
 */
class ReaperScytheItemTest {

    @Test
    void constants_matchSpec() {
        assertEquals(100, ReaperScytheItem.COOLDOWN_TICKS);  // 5 s
        assertEquals(3, ReaperScytheItem.MAX_MINIONS);
        assertEquals(4.0, ReaperScytheItem.SUMMON_RANGE);
        assertEquals(6.0, ReaperScytheItem.ATTACK_DAMAGE);
    }
}
