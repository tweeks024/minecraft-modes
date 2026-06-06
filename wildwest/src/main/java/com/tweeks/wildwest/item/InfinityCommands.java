package com.tweeks.wildwest.item;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure helpers for the per-stone command {@code List<String>} stored on
 * the gauntlet stack's {@link ModDataComponents#COMMANDS} component. An
 * empty string in slot {@code i} means "stone i falls back to its
 * built-in ability." A normalized list always has length 6.
 */
public final class InfinityCommands {
    private InfinityCommands() {}

    public static final int SLOT_COUNT = 6;

    /** Returns the command for the given stone, or "" if unset/out-of-range. */
    public static String get(List<String> commands, int stoneIndex) {
        if (commands == null || stoneIndex < 0 || stoneIndex >= commands.size()) return "";
        String cmd = commands.get(stoneIndex);
        return cmd == null ? "" : cmd;
    }

    /**
     * Returns a new length-6 list with {@code [stoneIndex]} replaced by
     * {@code newCommand}. Existing entries beyond the source list's length
     * are filled with empty strings.
     */
    public static List<String> set(List<String> existing, int stoneIndex, String newCommand) {
        List<String> next = new ArrayList<>(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (i == stoneIndex) {
                next.add(newCommand == null ? "" : newCommand);
            } else if (existing != null && i < existing.size()) {
                String c = existing.get(i);
                next.add(c == null ? "" : c);
            } else {
                next.add("");
            }
        }
        return next;
    }

    /** Build a fresh list of 6 empty strings. */
    public static List<String> empty() {
        return List.of("", "", "", "", "", "");
    }

    /** Normalize an arbitrary list to length 6, truncating or padding with "". */
    public static List<String> normalize(List<String> source) {
        List<String> next = new ArrayList<>(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (source != null && i < source.size()) {
                String c = source.get(i);
                next.add(c == null ? "" : c);
            } else {
                next.add("");
            }
        }
        return next;
    }
}
