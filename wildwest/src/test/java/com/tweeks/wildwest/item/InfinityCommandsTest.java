package com.tweeks.wildwest.item;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InfinityCommandsTest {

    @Test
    void get_emptyList_returnsEmptyString() {
        assertEquals("", InfinityCommands.get(List.of(), 0));
        assertEquals("", InfinityCommands.get(List.of(), 5));
    }

    @Test
    void get_outOfRange_returnsEmptyString() {
        List<String> commands = List.of("a", "b", "c", "d", "e", "f");
        assertEquals("", InfinityCommands.get(commands, -1));
        assertEquals("", InfinityCommands.get(commands, 6));
    }

    @Test
    void get_returnsStoredCommand() {
        List<String> commands = List.of("/give", "/teleport", "/effect", "", "", "");
        assertEquals("/give", InfinityCommands.get(commands, 0));
        assertEquals("/effect", InfinityCommands.get(commands, 2));
        assertEquals("", InfinityCommands.get(commands, 3));
    }

    @Test
    void set_onEmpty_returnsLength6WithSlotSet() {
        List<String> next = InfinityCommands.set(List.of(), 3, "/give");
        assertEquals(6, next.size());
        assertEquals("/give", next.get(3));
        for (int i = 0; i < 6; i++) {
            if (i != 3) assertEquals("", next.get(i));
        }
    }

    @Test
    void set_preservesOtherSlots() {
        List<String> existing = List.of("/a", "/b", "/c", "", "", "");
        List<String> next = InfinityCommands.set(existing, 3, "/d");
        assertEquals(List.of("/a", "/b", "/c", "/d", "", ""), next);
    }

    @Test
    void normalize_padsShortListWithEmptyStrings() {
        assertEquals(List.of("/a", "/b", "", "", "", ""),
                     InfinityCommands.normalize(List.of("/a", "/b")));
    }

    @Test
    void normalize_truncatesOversizedList() {
        List<String> over = List.of("a", "b", "c", "d", "e", "f", "g", "h");
        assertEquals(6, InfinityCommands.normalize(over).size());
    }

    @Test
    void normalize_replacesNullsWithEmptyStrings() {
        // The Codec layer can theoretically deliver null-terminated arrays
        // on round-trip from corrupted NBT; we should treat them as ""
        java.util.ArrayList<String> withNull = new java.util.ArrayList<>();
        withNull.add("/give");
        withNull.add(null);
        withNull.add("/teleport");
        List<String> normalized = InfinityCommands.normalize(withNull);
        assertEquals("/give", normalized.get(0));
        assertEquals("", normalized.get(1));
        assertEquals("/teleport", normalized.get(2));
    }
}
