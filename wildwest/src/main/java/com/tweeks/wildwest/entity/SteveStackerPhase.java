package com.tweeks.wildwest.entity;

/**
 * Pure phase-band logic for the Steve Stacker boss. Extracted as a standalone
 * (non-Mob) class so the band thresholds can be unit-tested without booting
 * Minecraft — referencing any class that extends {@code Monster} from a JUnit
 * context fails on {@code AttachmentHolder.<clinit>} requiring an FML loader.
 *
 * <p>Same shape as {@link com.tweeks.wildwest.entity.WeaponMode} and
 * {@link com.tweeks.wildwest.entity.ai.zombified.InfectionImmunity}.
 */
public final class SteveStackerPhase {
    private SteveStackerPhase() {}

    /**
     * @param health current HP
     * @param maxHealth max HP (typically 90; kept in the signature so any
     *                  future scaling is a one-call-site change, even though
     *                  the bands are currently hard-coded to 60 and 30)
     * @return stack height (3 = full stack, 2 = mid phase, 1 = final phase)
     */
    public static byte computeStackHeight(float health, float maxHealth) {
        if (health > 60.0f) return 3;
        if (health > 30.0f) return 2;
        return 1;
    }
}
