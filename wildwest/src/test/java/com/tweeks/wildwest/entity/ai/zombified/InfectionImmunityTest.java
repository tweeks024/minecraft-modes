package com.tweeks.wildwest.entity.ai.zombified;

import com.tweeks.wildwest.entity.ai.zombified.InfectionImmunity.Subject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfectionImmunityTest {

    static class TestSubject implements Subject {
        boolean undead, boss, walker, creative, spectator, mobOrPlayer = true;
        @Override public boolean isUndead() { return undead; }
        @Override public boolean isBoss() { return boss; }
        @Override public boolean isWalker() { return walker; }
        @Override public boolean isCreativeOrSpectatorPlayer() { return creative || spectator; }
        @Override public boolean isMobOrPlayer() { return mobOrPlayer; }
    }

    @Test
    void plainCow_isNotImmune() {
        assertFalse(InfectionImmunity.isImmune(new TestSubject()));
    }

    @Test
    void undeadEntity_isImmune() {
        TestSubject s = new TestSubject(); s.undead = true;
        assertTrue(InfectionImmunity.isImmune(s));
    }

    @Test
    void boss_isImmune() {
        TestSubject s = new TestSubject(); s.boss = true;
        assertTrue(InfectionImmunity.isImmune(s));
    }

    @Test
    void walker_isImmune() {
        TestSubject s = new TestSubject(); s.walker = true;
        assertTrue(InfectionImmunity.isImmune(s));
    }

    @Test
    void creativePlayer_isImmune() {
        TestSubject s = new TestSubject(); s.creative = true;
        assertTrue(InfectionImmunity.isImmune(s));
    }

    @Test
    void spectatorPlayer_isImmune() {
        TestSubject s = new TestSubject(); s.spectator = true;
        assertTrue(InfectionImmunity.isImmune(s));
    }

    @Test
    void armorStandLikeNonLiving_isImmune() {
        TestSubject s = new TestSubject(); s.mobOrPlayer = false;
        assertTrue(InfectionImmunity.isImmune(s));
    }
}
