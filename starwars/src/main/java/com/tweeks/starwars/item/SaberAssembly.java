package com.tweeks.starwars.item;

/**
 * Pure decision core for {@link KyberSaberRecipe}: given the tallies of a
 * crafting grid, decide whether a lightsaber assembles and in what colour.
 * Kept free of Minecraft types (only the pure {@link SaberColor} enum) so it
 * unit-tests without a registry bootstrap.
 */
public final class SaberAssembly {

    /** Result: whether a saber assembles, and its blade colour if so. */
    public record Result(boolean valid, SaberColor color) {
        public static final Result NONE = new Result(false, SaberColor.BLUE);
    }

    private SaberAssembly() {
    }

    /**
     * Valid iff exactly one hilt, exactly one kyber crystal, and nothing else
     * in the grid. The blade takes the crystal's colour (Sith red included).
     */
    public static Result evaluate(int hilts, int crystals, int others, SaberColor crystalColor) {
        if (hilts == 1 && crystals == 1 && others == 0) {
            return new Result(true, crystalColor);
        }
        return Result.NONE;
    }
}
