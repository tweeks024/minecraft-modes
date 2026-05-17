package com.tweeks.wildwest.event;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Predicate;

/**
 * Passive trigger for the Void Mark drop from
 * {@link com.tweeks.wildwest.entity.NullEntity}. On a fatal LivingDamageEvent,
 * consumes the first Void Mark in the player's main inventory and teleports
 * them to their respawn point at 1 HP.
 *
 * <p>The {@link #findFirstMatchingSlot} helper is broken out as a pure function
 * for unit testing — it takes a predicate so tests can pass a sentinel object
 * comparator without having to instantiate vanilla {@link ItemStack}s (which
 * would require a full Minecraft bootstrap that's not viable in plain JUnit).
 * The event handler proper requires NeoForge runtime.
 */
public final class VoidMarkHandler {

    private VoidMarkHandler() {}

    /**
     * Returns the lowest-index slot in {@code items} matching {@code predicate},
     * or -1 if none match. Empty stacks are skipped — callers do not need to
     * check {@code isEmpty()} in their predicate.
     */
    public static <T> int findFirstMatchingSlot(List<T> items, Predicate<T> predicate) {
        for (int i = 0; i < items.size(); i++) {
            T s = items.get(i);
            if (s != null && predicate.test(s)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Convenience overload for the runtime call site: finds the lowest-index
     * slot whose stack is a non-empty instance of {@code voidMarkItem}.
     */
    public static int findFirstVoidMarkSlot(NonNullList<ItemStack> items, Item voidMarkItem) {
        return findFirstMatchingSlot(items, s -> !s.isEmpty() && s.is(voidMarkItem));
    }
}
