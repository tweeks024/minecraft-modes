package com.tweeks.wildwest.item;

import net.minecraft.world.entity.EquipmentSlot;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CursedTomeSlotPickerTest {

    /** Deterministic RNG stand-in for the helper. */
    static class FixedRng implements CursedTomeSlotPicker.IntRng {
        private final int[] values;
        private int idx = 0;
        FixedRng(int... values) { this.values = values; }
        @Override public int nextInt(int bound) { return values[idx++ % values.length] % bound; }
    }

    @Test
    void emptySet_returnsNull() {
        assertNull(CursedTomeSlotPicker.pick(EnumSet.noneOf(EquipmentSlot.class), new FixedRng(0)));
    }

    @Test
    void singleSlot_returnsThatSlot() {
        EnumSet<EquipmentSlot> set = EnumSet.of(EquipmentSlot.MAINHAND);
        assertEquals(EquipmentSlot.MAINHAND,
            CursedTomeSlotPicker.pick(set, new FixedRng(0)));
        assertEquals(EquipmentSlot.MAINHAND,
            CursedTomeSlotPicker.pick(set, new FixedRng(42)));
    }

    @Test
    void allSlots_uniformDistribution() {
        EnumSet<EquipmentSlot> all = EnumSet.of(
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST,
            EquipmentSlot.LEGS, EquipmentSlot.FEET);

        // Sequentially feed 0..5 in a loop; each slot should show up exactly once
        // per cycle of 6 draws.
        Map<EquipmentSlot, Integer> counts = new HashMap<>();
        CursedTomeSlotPicker.IntRng rng = new FixedRng(0, 1, 2, 3, 4, 5);
        for (int i = 0; i < 6; i++) {
            EquipmentSlot s = CursedTomeSlotPicker.pick(all, rng);
            counts.merge(s, 1, Integer::sum);
        }
        for (EquipmentSlot slot : all) {
            assertEquals(1, counts.getOrDefault(slot, 0).intValue(),
                "Each slot should be picked exactly once when index cycles 0-5");
        }
    }

    @Test
    void twoSlots_returnsOneOfThem() {
        EnumSet<EquipmentSlot> set = EnumSet.of(EquipmentSlot.HEAD, EquipmentSlot.FEET);
        EquipmentSlot picked = CursedTomeSlotPicker.pick(set, new FixedRng(0));
        assertTrue(picked == EquipmentSlot.HEAD || picked == EquipmentSlot.FEET);

        EquipmentSlot picked2 = CursedTomeSlotPicker.pick(set, new FixedRng(1));
        assertTrue(picked2 == EquipmentSlot.HEAD || picked2 == EquipmentSlot.FEET);
    }
}
