package com.tweeks.starwars.item;

/**
 * Pure decision core for {@link SaberstaffRecipe}: two hilts and two Sith-red
 * kyber crystals make a saberstaff. Kept free of Minecraft types (only the
 * pure {@link SaberColor} enum) so it unit-tests without a registry bootstrap.
 */
public final class SaberstaffAssembly {

    private SaberstaffAssembly() {
    }

    /**
     * Valid iff exactly two hilts, exactly two kyber crystals — both RED — and
     * nothing else in the grid. Only red crystals: the saberstaff is Maul's
     * Sith weapon, so it demands bled/Mustafar-red kyber.
     */
    public static boolean valid(int hilts, int redCrystals, int otherCrystals, int others) {
        return hilts == 2 && redCrystals == 2 && otherCrystals == 0 && others == 0;
    }
}
