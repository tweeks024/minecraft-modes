package com.tweeks.wildwest.entity.projectile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeteorImpactLogicTest {

    @Test
    void airBlock_isNotReplaceable() {
        assertFalse(MeteorImpactLogic.shouldReplaceWithMagma(true, false));
    }

    @Test
    void solidNonImmune_isReplaceable() {
        assertTrue(MeteorImpactLogic.shouldReplaceWithMagma(false, false));
    }

    @Test
    void dragonImmune_isNotReplaceable() {
        // Bedrock, end portal frame, etc.
        assertFalse(MeteorImpactLogic.shouldReplaceWithMagma(false, true));
    }

    @Test
    void airAndImmune_returnsFalse() {
        // Pathological combination — air shouldn't be replaced regardless.
        assertFalse(MeteorImpactLogic.shouldReplaceWithMagma(true, true));
    }
}
