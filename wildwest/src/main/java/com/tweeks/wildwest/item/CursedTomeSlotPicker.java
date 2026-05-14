package com.tweeks.wildwest.item;

import net.minecraft.world.entity.EquipmentSlot;

import java.util.EnumSet;

/**
 * Pure helper: pick one element uniformly at random from an
 * {@link EnumSet} of {@link EquipmentSlot}s. Extracted from
 * {@link CursedTomeItem} so it can be unit-tested without booting Minecraft.
 *
 * <p>The caller is responsible for filtering the input set to only slots
 * that hold non-empty, damageable stacks; this helper just picks one.
 */
public final class CursedTomeSlotPicker {

    private CursedTomeSlotPicker() {}

    /** RNG abstraction so tests can drive deterministic sequences. */
    public interface IntRng {
        int nextInt(int bound);
    }

    public static EquipmentSlot pick(EnumSet<EquipmentSlot> slots, IntRng rng) {
        if (slots.isEmpty()) return null;
        int idx = rng.nextInt(slots.size());
        int i = 0;
        for (EquipmentSlot s : slots) {
            if (i == idx) return s;
            i++;
        }
        return null; // unreachable
    }
}
