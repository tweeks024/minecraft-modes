package com.tweeks.starwars.faction;

import net.minecraft.world.entity.LivingEntity;

/**
 * Stormtrooper-armor disguise check. Task 19 (armor set) fills the real
 * slot-by-slot check; until then no armor items exist, so nobody can be
 * disguised and this correctly returns false.
 */
public final class Disguise {
    private Disguise() {}

    public static boolean isWearingFullStormtrooperSet(LivingEntity entity) {
        return false;
    }
}
