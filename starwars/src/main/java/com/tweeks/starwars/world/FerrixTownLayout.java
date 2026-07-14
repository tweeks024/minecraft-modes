package com.tweeks.starwars.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Ferrix town fragment (Andor): a 20x10x14 block — two attached two-story
 * brick row houses (shared party wall, street-facing windows and doors, a
 * second-story floor, a flat dark-tile roof), a 4x4-footprint bell tower
 * ~10 tall attached at the west end (belfry slab, corner posts, one bell
 * marker), a paved street strip along the south, one loot chest, and two
 * stormtrooper patrol markers in the street (spawned as living entities at
 * generation time). Pure data so shape invariants are unit-testable without
 * MC bootstrap.
 */
public final class FerrixTownLayout {
    private FerrixTownLayout() {}

    public static final int SIZE_X = 20;
    public static final int SIZE_Y = 10;
    public static final int SIZE_Z = 14;

    public enum Kind {
        WALL, ROOF, FLOOR, WINDOW, DOOR_AIR, BELL, TOWER_WALL, CHEST, STORMTROOPER, AIR
    }

    public record Placement(int dx, int dy, int dz, Kind kind) {}

    // Street strip: z 0..3, full width. Houses: x 4..19, z 4..13, party wall
    // at x=11. Tower: x 0..3, z 4..7, attached to the west house wall.
    private static final int HOUSE_MIN_X = 4;
    private static final int HOUSE_MAX_X = 19;
    private static final int HOUSE_MIN_Z = 4;
    private static final int HOUSE_MAX_Z = 13;
    private static final int PARTY_WALL_X = 11;
    private static final int ROOF_Y = 8;

    private static boolean houseWindow(int x, int y, int z) {
        return z == HOUSE_MIN_Z
            && (x == 6 || x == 9 || x == 14 || x == 17)
            && (y == 2 || y == 6);
    }

    private static boolean houseDoor(int x, int y, int z) {
        return z == HOUSE_MIN_Z && (x == 7 || x == 15) && (y == 1 || y == 2);
    }

    public static List<Placement> placements() {
        List<Placement> out = new ArrayList<>();

        // Paved street strip along the south edge.
        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z <= 3; z++) {
                out.add(new Placement(x, 0, z, Kind.FLOOR));
            }
        }

        // Two attached two-story row houses.
        for (int x = HOUSE_MIN_X; x <= HOUSE_MAX_X; x++) {
            for (int z = HOUSE_MIN_Z; z <= HOUSE_MAX_Z; z++) {
                out.add(new Placement(x, 0, z, Kind.FLOOR));
                boolean perimeter = x == HOUSE_MIN_X || x == HOUSE_MAX_X
                    || z == HOUSE_MIN_Z || z == HOUSE_MAX_Z;
                boolean party = !perimeter && x == PARTY_WALL_X;
                for (int y = 1; y < ROOF_Y; y++) {
                    if (perimeter) {
                        if (houseWindow(x, y, z)) {
                            out.add(new Placement(x, y, z, Kind.WINDOW));
                        } else if (houseDoor(x, y, z)) {
                            out.add(new Placement(x, y, z, Kind.DOOR_AIR));
                        } else {
                            out.add(new Placement(x, y, z, Kind.WALL));
                        }
                    } else if (party) {
                        out.add(new Placement(x, y, z, Kind.WALL));
                    } else if (y == 4) {
                        out.add(new Placement(x, y, z, Kind.FLOOR));        // second story
                    } else {
                        out.add(new Placement(x, y, z, Kind.AIR));
                    }
                }
                out.add(new Placement(x, ROOF_Y, z, Kind.ROOF));            // flat roof
            }
        }

        // Bell tower (4x4 footprint, ~10 tall) attached at the west end.
        for (int x = 0; x <= 3; x++) {
            for (int z = 4; z <= 7; z++) {
                out.add(new Placement(x, 0, z, Kind.FLOOR));
                boolean shell = x == 0 || x == 3 || z == 4 || z == 7;
                for (int y = 1; y <= 7; y++) {
                    if (shell) {
                        boolean door = z == 4 && x == 1 && (y == 1 || y == 2);
                        out.add(new Placement(x, y, z, door ? Kind.DOOR_AIR : Kind.TOWER_WALL));
                    } else {
                        out.add(new Placement(x, y, z, Kind.AIR));
                    }
                }
                out.add(new Placement(x, 8, z, Kind.TOWER_WALL));           // belfry slab
                if ((x == 0 || x == 3) && (z == 4 || z == 7)) {
                    out.add(new Placement(x, 9, z, Kind.TOWER_WALL));       // corner posts
                }
            }
        }

        // Bell on the belfry slab.
        out.add(new Placement(1, 9, 5, Kind.BELL));

        // Loot chest inside the west house's ground floor.
        out.removeIf(p -> p.dx() == 6 && p.dy() == 1 && p.dz() == 11);
        out.add(new Placement(6, 1, 11, Kind.CHEST));

        // Imperial patrol: two stormtrooper markers in the street (spawned as
        // living entities at generation time).
        out.add(new Placement(8, 1, 2, Kind.STORMTROOPER));
        out.add(new Placement(14, 1, 2, Kind.STORMTROOPER));

        return out;
    }
}
