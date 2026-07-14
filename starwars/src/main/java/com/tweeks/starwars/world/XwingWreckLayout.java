package com.tweeks.starwars.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Crashed X-wing (Dagobah): a 13-wide, 13-long wreck sunk nose-down into the
 * swamp — the layout's dy runs from {@link #MIN_Y} (-2) so the nose and the
 * low wing pair sit below the terrain-snapped surface, half-submerged. A
 * stepped fuselage climbs tail-up along z, four wings splay in the classic X
 * (two up, two down, mirrored in both the x axis and the y axis), orange
 * squadron stripes accent the hull, a two-pane glass canopy tops the cockpit,
 * and one loot chest sits in the tail. Deliberately air-free: the piece
 * places solid blocks only and never clears the surrounding water. Pure data
 * so shape invariants are unit-testable without MC bootstrap.
 */
public final class XwingWreckLayout {
    private XwingWreckLayout() {}

    public static final int SIZE_X = 13;
    public static final int SIZE_Y = 6;
    public static final int SIZE_Z = 13;
    /** Lowest layout dy: the sunken nose sits two blocks below the surface. */
    public static final int MIN_Y = -2;

    public enum Kind { FUSELAGE, ORANGE, WING, COCKPIT, CHEST }

    public record Placement(int dx, int dy, int dz, Kind kind) {}

    /** Fuselage center line. */
    public static final int SPINE_X = 6;

    /** Wings attach across these fuselage segments. */
    private static final int[] WING_Z = { 9, 10 };

    /** Fuselage base height: nose buried at -2, stepping up to 1 at the tail. */
    private static int base(int z) {
        return Math.min(z / 3, 3) - 2;
    }

    public static List<Placement> placements() {
        List<Placement> out = new ArrayList<>();

        // Fuselage: a slim nose spar (z 0..5) widening to a 3-wide hull
        // (z 6..12), two blocks tall, stepping tail-up out of the swamp.
        for (int z = 0; z < SIZE_Z; z++) {
            int minX = z < 6 ? SPINE_X : SPINE_X - 1;
            int maxX = z < 6 ? SPINE_X : SPINE_X + 1;
            for (int x = minX; x <= maxX; x++) {
                out.add(new Placement(x, base(z), z, Kind.FUSELAGE));
                out.add(new Placement(x, base(z) + 1, z, Kind.FUSELAGE));
            }
        }

        // Orange squadron stripes ringing the hull flanks.
        replace(out, SPINE_X - 1, 1, 7, Kind.ORANGE);
        replace(out, SPINE_X + 1, 1, 7, Kind.ORANGE);
        replace(out, SPINE_X - 1, 2, 11, Kind.ORANGE);
        replace(out, SPINE_X + 1, 2, 11, Kind.ORANGE);

        // Cockpit canopy: two glass panes stepping up toward the tail.
        out.add(new Placement(SPINE_X, 2, 8, Kind.COCKPIT));
        out.add(new Placement(SPINE_X, 3, 9, Kind.COCKPIT));

        // Four wings in the X splay: two rising, two dipping into the water,
        // mirrored around the spine (x) and around the hull midline (y).
        for (int z : WING_Z) {
            for (int d = 2; d <= 5; d++) {
                int rise = (d - 2) / 2;
                out.add(new Placement(SPINE_X - d, 2 + rise, z, Kind.WING));
                out.add(new Placement(SPINE_X + d, 2 + rise, z, Kind.WING));
                out.add(new Placement(SPINE_X - d, 1 - rise, z, Kind.WING));
                out.add(new Placement(SPINE_X + d, 1 - rise, z, Kind.WING));
            }
        }

        // Loot chest in the tail hull, open to the sky above the waterline.
        replace(out, SPINE_X, 2, 11, Kind.CHEST);

        return out;
    }

    private static void replace(List<Placement> out, int x, int y, int z, Kind kind) {
        out.removeIf(p -> p.dx() == x && p.dy() == y && p.dz() == z);
        out.add(new Placement(x, y, z, kind));
    }
}
