package com.tweeks.thief.entity;

/**
 * The five behavioral states of a {@link ThiefEntity}. The state is synced
 * server→client via an {@code EntityDataAccessor<Byte>}; the renderer reads it
 * to choose textures and the held weapon model.
 *
 * <p>Transitions are one-way once revealed (no return to {@link #DISGUISED}
 * or {@link #SUSPICIOUS}): the disguise is blown for this Thief's lifetime.
 * {@link #SUSPICIOUS} is a 2-second window that can either escalate to a
 * revealed state or relax back to {@link #DISGUISED}.
 */
public enum RevealState {
    /** Wandering villager facade; can secretly attack guards and steal from chests. */
    DISGUISED,
    /** 2s observation window; relaxes to DISGUISED or escalates to a revealed state. */
    SUSPICIOUS,
    /** Crossbow drawn; kites away from threats and fires from range. */
    REVEALED_RANGED,
    /** Blackjack drawn; closes for stunning melee strikes. */
    REVEALED_MELEE,
    /** Carrying loot or low HP; pathing back to the hideout chest. */
    FLEEING;

    public byte toByte() {
        return (byte) this.ordinal();
    }

    public static RevealState fromByte(byte b) {
        RevealState[] values = values();
        return (b >= 0 && b < values.length) ? values[b] : DISGUISED;
    }

    /** True if Guards (via SecurityHostile.isCurrentlyHostile) should treat this Thief as a target. */
    public boolean isHostile() {
        return this == REVEALED_RANGED || this == REVEALED_MELEE || this == FLEEING;
    }

    /**
     * Whether the state machine permits a transition from {@code this} to {@code next}.
     * Rules: DISGUISED → any. SUSPICIOUS → DISGUISED or any revealed/fleeing state.
     * Once revealed (RANGED/MELEE/FLEEING), can only swap among the revealed family.
     */
    public boolean canTransitionTo(RevealState next) {
        if (this == next) return true;
        return switch (this) {
            case DISGUISED -> true;
            case SUSPICIOUS -> next != SUSPICIOUS;
            case REVEALED_RANGED, REVEALED_MELEE, FLEEING -> next.isHostile();
        };
    }
}
