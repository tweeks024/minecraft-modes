package com.tweeks.starwars.item;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ForceCooldownsTest {

    @Test
    void powerRoster_matchesSpec() {
        assertEquals(5, ForcePower.values().length);
        assertEquals(60, ForcePower.PUSH.cooldownTicks());
        assertEquals(60, ForcePower.PULL.cooldownTicks());
        assertEquals(120, ForcePower.LEAP.cooldownTicks());
        assertEquals(300, ForcePower.MIND_TRICK.cooldownTicks());
        assertEquals(200, ForcePower.LIGHTNING.cooldownTicks());
        // Alignment pull: light powers +2, dark -2, neutral 0.
        assertEquals(0, ForcePower.PUSH.alignmentDelta());
        assertEquals(0, ForcePower.PULL.alignmentDelta());
        assertEquals(2, ForcePower.LEAP.alignmentDelta());
        assertEquals(2, ForcePower.MIND_TRICK.alignmentDelta());
        assertEquals(-2, ForcePower.LIGHTNING.alignmentDelta());
    }

    @Test
    void byIndex_clampsToPush() {
        assertEquals(ForcePower.PUSH, ForcePower.byIndex(-1));
        assertEquals(ForcePower.PUSH, ForcePower.byIndex(99));
        assertEquals(ForcePower.LIGHTNING, ForcePower.byIndex(4));
    }

    @Test
    void cooldowns_applyAndExpire() {
        List<Long> cds = ForceCooldowns.emptyCooldowns();
        assertFalse(ForceCooldowns.isOnCooldown(cds, 0, 100L));
        cds = ForceCooldowns.applyCooldown(cds, 0, 100L, 60);
        assertTrue(ForceCooldowns.isOnCooldown(cds, 0, 100L));
        assertTrue(ForceCooldowns.isOnCooldown(cds, 0, 159L));
        assertFalse(ForceCooldowns.isOnCooldown(cds, 0, 160L));
        assertEquals(160L, ForceCooldowns.getExpiry(cds, 0));
        // Other slots untouched.
        assertFalse(ForceCooldowns.isOnCooldown(cds, 1, 100L));
    }

    @Test
    void cooldowns_nullAndShortListSafe() {
        assertFalse(ForceCooldowns.isOnCooldown(null, 0, 0L));
        assertFalse(ForceCooldowns.isOnCooldown(List.of(), 4, 0L));
        List<Long> padded = ForceCooldowns.applyCooldown(List.of(5L), 3, 10L, 20);
        assertEquals(5, padded.size());
        assertEquals(5L, padded.get(0));
        assertEquals(30L, padded.get(3));
    }

    @Test
    void pacify_isActiveUntilExpiry() {
        assertTrue(com.tweeks.starwars.faction.PacifyState.isActive(200L, 199L));
        assertFalse(com.tweeks.starwars.faction.PacifyState.isActive(200L, 200L));
        assertFalse(com.tweeks.starwars.faction.PacifyState.isActive(0L, 5L));
    }

    @Test
    void slotCount_coversEveryForcePower() {
        // If a power were added past SLOT_COUNT, applyCooldown would silently
        // drop that slot's cooldown. This coupling must hold.
        assertTrue(ForceCooldowns.SLOT_COUNT >= com.tweeks.starwars.item.ForcePower.values().length,
            "SLOT_COUNT (" + ForceCooldowns.SLOT_COUNT + ") must cover all "
                + com.tweeks.starwars.item.ForcePower.values().length + " Force powers");
    }

    @Test
    void applyCooldown_recordsTheHighestPowerSlot() {
        int last = ForceCooldowns.SLOT_COUNT - 1;
        List<Long> after = ForceCooldowns.applyCooldown(ForceCooldowns.emptyCooldowns(), last, 100L, 40);
        assertEquals(ForceCooldowns.SLOT_COUNT, after.size());
        assertTrue(ForceCooldowns.isOnCooldown(after, last, 100L),
            "the top power slot must actually record a cooldown");
        assertFalse(ForceCooldowns.isOnCooldown(after, last, 140L));
    }
}
