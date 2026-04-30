package com.tweeks.wildwest.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionPredicateTest {

    static class TestLawman implements Lawman {}
    static class TestOutlaw implements Outlaw {}
    static class Neutral {}

    @Test
    void lawman_implementer_passes_lawman_check() {
        assertTrue(new TestLawman() instanceof Lawman);
        assertFalse(new TestLawman() instanceof Outlaw);
    }

    @Test
    void outlaw_implementer_passes_outlaw_check() {
        assertTrue(new TestOutlaw() instanceof Outlaw);
        assertFalse(new TestOutlaw() instanceof Lawman);
    }

    @Test
    void neutral_class_passes_neither() {
        assertFalse(new Neutral() instanceof Lawman);
        assertFalse(new Neutral() instanceof Outlaw);
    }
}
