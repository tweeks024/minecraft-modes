package com.tweeks.wildwest.event;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pure-Java tests for {@link VoidMarkHandler#findFirstMatchingSlot}. Uses a
 * tagged sentinel record instead of touching vanilla {@code Items} / {@code ItemStack}
 * — those statics transitively initialize {@code Blocks} / {@code SoundEvents} /
 * {@code FeatureFlags}, which fail with "no current FML Loader" outside the
 * NeoForge runtime. The runtime overload {@link VoidMarkHandler#findFirstVoidMarkSlot}
 * is exercised end-to-end by the live game (no easy way to bootstrap registries
 * in plain JUnit), but it is a thin wrapper over the generic helper covered here.
 */
class VoidMarkInventoryScanTest {

    /** Tagged stand-in for an ItemStack — VOID_MARK vs ANYTHING_ELSE. */
    private record Stack(boolean isVoidMark) {}

    private static final Stack VOID_MARK = new Stack(true);
    private static final Stack OTHER = new Stack(false);
    private static final Predicate<Stack> IS_VOID_MARK = s -> s != null && s.isVoidMark();

    private static List<Stack> emptyInventory() {
        ArrayList<Stack> items = new ArrayList<>(36);
        for (int i = 0; i < 36; i++) items.add(null);
        return items;
    }

    @Test
    void emptyInventory_returnsNegativeOne() {
        assertEquals(-1, VoidMarkHandler.findFirstMatchingSlot(emptyInventory(), IS_VOID_MARK));
    }

    @Test
    void noVoidMarks_returnsNegativeOne() {
        List<Stack> items = emptyInventory();
        items.set(0, OTHER);
        items.set(5, OTHER);
        assertEquals(-1, VoidMarkHandler.findFirstMatchingSlot(items, IS_VOID_MARK));
    }

    @Test
    void singleVoidMarkAtSlot5_returnsFive() {
        List<Stack> items = emptyInventory();
        items.set(5, VOID_MARK);
        assertEquals(5, VoidMarkHandler.findFirstMatchingSlot(items, IS_VOID_MARK));
    }

    @Test
    void multipleVoidMarks_returnsLowestIndex() {
        List<Stack> items = emptyInventory();
        items.set(2, VOID_MARK);
        items.set(7, VOID_MARK);
        assertEquals(2, VoidMarkHandler.findFirstMatchingSlot(items, IS_VOID_MARK));
    }

    @Test
    void otherItemsAtLowerSlots_stillFindsVoidMark() {
        List<Stack> items = emptyInventory();
        items.set(0, OTHER);
        items.set(1, OTHER);
        items.set(3, VOID_MARK);
        items.set(7, VOID_MARK);
        assertEquals(3, VoidMarkHandler.findFirstMatchingSlot(items, IS_VOID_MARK));
    }
}
