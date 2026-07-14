package com.tweeks.starwars.entity.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ProbeAlarmTest {

    private static ProbeAlarm.Result run(ProbeAlarm alarm, boolean target, int ticks) {
        ProbeAlarm.Result last = ProbeAlarm.Result.IDLE;
        for (int i = 0; i < ticks; i++) {
            last = alarm.tick(target);
        }
        return last;
    }

    @Test
    public void idleUntilArmWindowElapses() {
        ProbeAlarm alarm = new ProbeAlarm();
        for (int i = 0; i < ProbeAlarm.ARM_TICKS - 1; i++) {
            assertEquals(ProbeAlarm.Result.IDLE, alarm.tick(true), "tick " + i);
        }
    }

    @Test
    public void summonsOnExactArmTick() {
        ProbeAlarm alarm = new ProbeAlarm();
        // First ARM_TICKS-1 ticks are idle; the ARM_TICKS-th trips.
        assertEquals(ProbeAlarm.Result.IDLE, run(alarm, true, ProbeAlarm.ARM_TICKS - 1));
        assertEquals(ProbeAlarm.Result.SUMMON, alarm.tick(true));
    }

    @Test
    public void summonsOnlyOncePerWindow() {
        ProbeAlarm alarm = new ProbeAlarm();
        int summons = 0;
        for (int i = 0; i < ProbeAlarm.ARM_TICKS + 50; i++) {
            if (alarm.tick(true) == ProbeAlarm.Result.SUMMON) summons++;
        }
        assertEquals(1, summons);
    }

    @Test
    public void losingTargetResetsCharge() {
        ProbeAlarm alarm = new ProbeAlarm();
        run(alarm, true, ProbeAlarm.ARM_TICKS - 1);   // almost there
        assertEquals(ProbeAlarm.Result.IDLE, alarm.tick(false)); // lost target -> reset
        assertEquals(0, alarm.charge());
        // Must now serve the full window again from scratch.
        assertEquals(ProbeAlarm.Result.IDLE, run(alarm, true, ProbeAlarm.ARM_TICKS - 1));
        assertEquals(ProbeAlarm.Result.SUMMON, alarm.tick(true));
    }

    @Test
    public void cooldownBlocksReArm() {
        ProbeAlarm alarm = new ProbeAlarm();
        assertEquals(ProbeAlarm.Result.SUMMON, run(alarm, true, ProbeAlarm.ARM_TICKS));
        // Immediately after a summon it is locked out for the whole cooldown,
        // even with the target continuously held.
        for (int i = 0; i < ProbeAlarm.COOLDOWN_TICKS; i++) {
            assertEquals(ProbeAlarm.Result.IDLE, alarm.tick(true), "cooldown tick " + i);
        }
    }

    @Test
    public void reArmsAfterCooldown() {
        ProbeAlarm alarm = new ProbeAlarm();
        run(alarm, true, ProbeAlarm.ARM_TICKS);        // first summon
        run(alarm, true, ProbeAlarm.COOLDOWN_TICKS);   // serve the lockout
        // Now it can charge again and fire a second time.
        assertEquals(ProbeAlarm.Result.IDLE, run(alarm, true, ProbeAlarm.ARM_TICKS - 1));
        assertEquals(ProbeAlarm.Result.SUMMON, alarm.tick(true));
    }

    @Test
    public void cooldownCountsDownRegardlessOfTarget() {
        ProbeAlarm alarm = new ProbeAlarm();
        run(alarm, true, ProbeAlarm.ARM_TICKS);        // summon -> cooldown armed
        assertEquals(ProbeAlarm.COOLDOWN_TICKS, alarm.cooldown());
        run(alarm, false, ProbeAlarm.COOLDOWN_TICKS);  // losing target still drains it
        assertEquals(0, alarm.cooldown());
    }

    @Test
    public void neverSummonsWithoutTarget() {
        ProbeAlarm alarm = new ProbeAlarm();
        assertEquals(ProbeAlarm.Result.IDLE, run(alarm, false, ProbeAlarm.ARM_TICKS * 3));
    }
}
