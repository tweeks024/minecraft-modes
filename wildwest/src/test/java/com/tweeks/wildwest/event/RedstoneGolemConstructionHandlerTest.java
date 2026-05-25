package com.tweeks.wildwest.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Constants smoke test for construction handler. Pattern-match logic is
 * exercised in dev-client by placing TNT atop a redstone-T pattern.
 */
class RedstoneGolemConstructionHandlerTest {

    @Test
    void constants_matchSpec() {
        assertEquals("wildwest:golem_consumed", RedstoneGolemConstructionHandler.CONSUMED_TAG);
    }
}
