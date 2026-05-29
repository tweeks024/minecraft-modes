package com.tweeks.wildwest.entity.ai;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrabAlertSwarmHelperTest {

    @Test
    void shouldAlert_adultCrab_withinRadius_noTarget() {
        assertTrue(CrabAlertSwarmHelper.shouldAlert(7.99, false, false));
    }

    @Test
    void shouldNotAlert_outsideRadius() {
        assertFalse(CrabAlertSwarmHelper.shouldAlert(8.01, false, false));
    }

    @Test
    void shouldNotAlert_atExactRadius() {
        // strict less-than: distance == SWARM_RADIUS is NOT alerted
        assertFalse(CrabAlertSwarmHelper.shouldAlert(8.0, false, false));
    }

    @Test
    void shouldNotAlert_baby() {
        assertFalse(CrabAlertSwarmHelper.shouldAlert(2.0, true, false));
    }

    @Test
    void shouldNotAlert_crabAlreadyHasTarget() {
        assertFalse(CrabAlertSwarmHelper.shouldAlert(2.0, false, true));
    }
}
