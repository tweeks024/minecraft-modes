package com.tweeks.starwars.bounty;

import java.util.List;

/**
 * The cantina bounty board's contract catalogue and selection logic. Pure —
 * no Minecraft types (target entities are plain id strings) — so the offer
 * rotation and matching are unit-tested without a game bootstrap.
 */
public final class BountyContract {

    /** One posted bounty: hunt {@code count} of {@code targetId} for {@code reward} credits. */
    public record Template(String targetId, String nameKey, int count, int reward) {
    }

    /** The standing board. Tougher / rarer targets pay more per head. */
    public static final List<Template> TEMPLATES = List.of(
        new Template("starwars:stormtrooper", "entity.starwars.stormtrooper", 5, 12),
        new Template("starwars:battle_droid", "entity.starwars.battle_droid", 5, 12),
        new Template("starwars:snowtrooper", "entity.starwars.snowtrooper", 4, 12),
        new Template("starwars:tusken_raider", "entity.starwars.tusken_raider", 3, 9),
        new Template("starwars:probe_droid", "entity.starwars.probe_droid", 2, 14),
        new Template("starwars:wampa", "entity.starwars.wampa", 1, 18),
        new Template("starwars:dragonsnake", "entity.starwars.dragonsnake", 2, 12)
    );

    private BountyContract() {
    }

    /**
     * The bounty currently posted for a given rotation bucket — every board
     * shows the same one, and it rotates as the bucket advances.
     */
    public static Template forBucket(long bucket) {
        int index = (int) Long.remainderUnsigned(mix(bucket), TEMPLATES.size());
        return TEMPLATES.get(index);
    }

    /** A fresh state for accepting the given template. */
    public static BountyState accept(Template template) {
        return new BountyState(template.targetId(), template.count(), template.count(), template.reward());
    }

    /** True when this kill counts toward the state's target. */
    public static boolean matches(BountyState state, String victimId) {
        return state != null && state.remaining() > 0 && state.targetId().equals(victimId);
    }

    /** One confirmed kill; returns the decremented state (never below zero). */
    public static BountyState onKill(BountyState state, String victimId) {
        if (!matches(state, victimId)) {
            return state;
        }
        return state.withRemaining(state.remaining() - 1);
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return (z ^ (z >>> 31)) & Long.MAX_VALUE;
    }
}
