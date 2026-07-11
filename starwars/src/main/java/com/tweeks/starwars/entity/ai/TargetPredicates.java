package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.faction.Alignment;
import com.tweeks.starwars.faction.SwFaction;

/**
 * Pure faction-war targeting decision. All world lookups (faction of the
 * candidate, player alignment score, disguise state) happen in the caller
 * (SwTargetGoal); this class only decides.
 */
public final class TargetPredicates {
    private TargetPredicates() {}

    public static boolean shouldTarget(SwFaction myFaction,
                                       boolean targetIsCombatant,
                                       SwFaction targetFaction,
                                       boolean targetIsPlayer,
                                       int playerScore,
                                       boolean playerDisguisedAsEmpire) {
        if (myFaction == SwFaction.NEUTRAL) return false;
        if (targetIsCombatant) {
            return targetFaction == myFaction.enemy();
        }
        if (targetIsPlayer) {
            if (myFaction == SwFaction.EMPIRE && playerDisguisedAsEmpire) return false;
            return Alignment.isHostileTo(playerScore, myFaction);
        }
        return false;
    }
}
