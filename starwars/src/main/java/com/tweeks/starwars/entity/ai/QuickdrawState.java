package com.tweeks.starwars.entity.ai;

import java.util.UUID;

/**
 * Pure state machine for Han Solo's "Shoots First" quickdraw: the first shot
 * against each newly acquired target fires after a short windup with bonus
 * damage; the target is then remembered so subsequent engagement falls
 * through to the normal {@link BlasterAttackGoal} pacing.
 *
 * <p>Single-field memory (last ambushed target only): switching to a
 * different target re-arms the quickdraw, including switching back to a
 * previously ambushed one. Accepted in the spec — keeps state trivially
 * small and unit-testable.
 *
 * <p>No engine imports — lives outside MC classes for unit testing, same
 * pattern as {@link com.tweeks.starwars.faction.Alignment}.
 */
public final class QuickdrawState {

    public static final int QUICKDRAW_WINDUP_TICKS = 8;

    private UUID lastAmbushedTargetId = null;
    private int windupRemaining = 0;

    /** True when a quickdraw may begin against this target. */
    public boolean canAmbush(UUID targetId) {
        return targetId != null && !targetId.equals(this.lastAmbushedTargetId);
    }

    public void startWindup() {
        this.windupRemaining = QUICKDRAW_WINDUP_TICKS;
    }

    /** Advance one tick; returns true exactly on the tick the windup expires. */
    public boolean tickWindup() {
        if (this.windupRemaining <= 0) return false;
        this.windupRemaining--;
        return this.windupRemaining == 0;
    }

    public boolean isWindingUp() {
        return this.windupRemaining > 0;
    }

    /** Record a completed ambush; the same target can't be ambushed again. */
    public void markAmbushed(UUID targetId) {
        this.lastAmbushedTargetId = targetId;
        this.windupRemaining = 0;
    }

    /** Abort the windup (target died/lost) without consuming the ambush. */
    public void cancel() {
        this.windupRemaining = 0;
    }
}
