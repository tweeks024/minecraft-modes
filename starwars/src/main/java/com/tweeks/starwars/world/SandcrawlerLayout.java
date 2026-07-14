package com.tweeks.starwars.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Jawa sandcrawler: a 7x12x16 rusty hulk oriented along +Z — two dark tread
 * layers under the full footprint, a hollow terracotta hull box, a stepped
 * front slope rising from the nose (z0) to full height at mid-vehicle (z8),
 * an interior deck, a side door gap, one cargo-hold loot chest, and two
 * salvage-astromech markers inside (spawned as living entities at generation
 * time). Pure data so shape invariants are unit-testable without MC bootstrap.
 */
public final class SandcrawlerLayout {
    private SandcrawlerLayout() {}

    public static final int SIZE_X = 7;
    public static final int SIZE_Y = 12;
    public static final int SIZE_Z = 16;

    public enum Kind { HULL, TREAD, SLOPE, DECK, CHEST, ASTROMECH, AIR }

    public record Placement(int dx, int dy, int dz, Kind kind) {}

    /** Z where the front slope reaches full hull height. */
    private static final int SLOPE_END_Z = 8;

    /** Top hull y at a given z: the stepped front incline, then a flat roof. */
    private static int topY(int z) {
        return Math.min(SIZE_Y - 1, 3 + z);
    }

    public static List<Placement> placements() {
        List<Placement> out = new ArrayList<>();

        for (int z = 0; z < SIZE_Z; z++) {
            for (int x = 0; x < SIZE_X; x++) {
                boolean edge = x == 0 || x == SIZE_X - 1 || z == 0 || z == SIZE_Z - 1;

                // Bottom two layers: dark treads under the full footprint.
                out.add(new Placement(x, 0, z, Kind.TREAD));
                out.add(new Placement(x, 1, z, Kind.TREAD));

                // Cargo floor (y2): deck inside, hull on the rim.
                out.add(new Placement(x, 2, z, edge ? Kind.HULL : Kind.DECK));

                // Hull shell above, stepped down toward the nose.
                for (int y = 3; y <= topY(z); y++) {
                    boolean top = y == topY(z);
                    boolean door = x == 0 && z >= 10 && z <= 11 && y <= 5;  // side door gap
                    if (door) {
                        out.add(new Placement(x, y, z, Kind.AIR));
                    } else if (top && z <= SLOPE_END_Z) {
                        out.add(new Placement(x, y, z, Kind.SLOPE));        // angled front
                    } else if (edge || top) {
                        out.add(new Placement(x, y, z, Kind.HULL));         // walls + roof
                    } else {
                        out.add(new Placement(x, y, z, Kind.AIR));          // hollow interior
                    }
                }
            }
        }

        // Loot chest in the rear cargo hold.
        out.removeIf(p -> p.dx() == 3 && p.dy() == 3 && p.dz() == 13);
        out.add(new Placement(3, 3, 13, Kind.CHEST));

        // Two salvage astromechs in the hold (spawned as living entities at
        // generation time).
        out.removeIf(p -> p.dx() == 2 && p.dy() == 3 && p.dz() == 6);
        out.add(new Placement(2, 3, 6, Kind.ASTROMECH));
        out.removeIf(p -> p.dx() == 4 && p.dy() == 3 && p.dz() == 10);
        out.add(new Placement(4, 3, 10, Kind.ASTROMECH));

        return out;
    }
}
