package com.tweeks.thief.entity.ai;

import com.tweeks.securitycore.ai.StunningMeleeGoal;
import com.tweeks.thief.entity.RevealState;
import com.tweeks.thief.entity.ThiefEntity;

/**
 * StunningMeleeGoal gated on {@link RevealState#REVEALED_MELEE}. When the
 * Thief is in any other state (including FLEEING or REVEALED_RANGED), this
 * goal yields so the appropriate combat goal can take over.
 */
public class BlackjackStrikeGoal extends StunningMeleeGoal {

    private final ThiefEntity thief;

    public BlackjackStrikeGoal(ThiefEntity thief) {
        super(thief, 1.0, true,
            /*stunDurationTicks=*/ 40,
            /*slownessAmplifier=*/ 1,
            /*weaknessAmplifier=*/ 0,
            /*knockbackStrength=*/ 0.2);
        this.thief = thief;
    }

    @Override
    public boolean canUse() {
        return thief.getRevealState() == RevealState.REVEALED_MELEE && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return thief.getRevealState() == RevealState.REVEALED_MELEE && super.canContinueToUse();
    }
}
