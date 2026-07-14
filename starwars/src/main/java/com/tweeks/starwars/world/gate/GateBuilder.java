package com.tweeks.starwars.world.gate;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure placement list for the auto-built arrival gate: a 2x3 portal in a
 * 4x5 iron ring on a small platform, with breathing room carved out. Offsets
 * are relative to the interior bottom-left cell; {@code axisX} mirrors the
 * gate plane. Emission order matters and is what the stamping code applies:
 * CLEAR, then PLATFORM, then FRAME, then PORTAL — so the film is never placed
 * before its supporting ring.
 */
public final class GateBuilder {
    public static final int WIDTH = 2;
    public static final int HEIGHT = 3;
    /** Platform/clearing extends this far beyond the frame along the gate. */
    private static final int APRON = 2;

    public enum Kind {
        CLEAR, PLATFORM, FRAME, PORTAL
    }

    public record Placement(int dx, int dy, int dz, Kind kind) {
    }

    private GateBuilder() {
    }

    /** Along-axis coordinate range of the worked area: [-APRON, WIDTH+APRON-1]. */
    private static boolean onRing(int a, int dy) {
        boolean insideBox = a >= -1 && a <= WIDTH && dy >= -1 && dy <= HEIGHT;
        boolean interior = a >= 0 && a < WIDTH && dy >= 0 && dy < HEIGHT;
        return insideBox && !interior;
    }

    public static List<Placement> arrivalGate(boolean axisX) {
        List<Placement> placements = new ArrayList<>();
        // Breathing room around and through the gate.
        for (int a = -APRON; a <= WIDTH + APRON - 1; a++) {
            for (int dy = 0; dy <= HEIGHT; dy++) {
                for (int cross = -1; cross <= 1; cross++) {
                    if (cross == 0 && onRing(a, dy)) {
                        continue; // frame will go here
                    }
                    if (cross == 0 && a >= 0 && a < WIDTH && dy < HEIGHT) {
                        continue; // portal film will go here
                    }
                    placements.add(place(a, dy, cross, Kind.CLEAR, axisX));
                }
            }
        }
        // Landing platform under everything.
        for (int a = -APRON; a <= WIDTH + APRON - 1; a++) {
            for (int cross = -1; cross <= 1; cross++) {
                if (cross == 0 && onRing(a, -1)) {
                    continue; // bottom frame row keeps its iron
                }
                placements.add(place(a, -1, cross, Kind.PLATFORM, axisX));
            }
        }
        // Iron ring.
        for (int a = -1; a <= WIDTH; a++) {
            for (int dy = -1; dy <= HEIGHT; dy++) {
                if (onRing(a, dy)) {
                    placements.add(place(a, dy, 0, Kind.FRAME, axisX));
                }
            }
        }
        // Portal film, last.
        for (int a = 0; a < WIDTH; a++) {
            for (int dy = 0; dy < HEIGHT; dy++) {
                placements.add(place(a, dy, 0, Kind.PORTAL, axisX));
            }
        }
        return placements;
    }

    private static Placement place(int along, int dy, int cross, Kind kind, boolean axisX) {
        return axisX
            ? new Placement(along, dy, cross, kind)
            : new Placement(cross, dy, along, kind);
    }
}
