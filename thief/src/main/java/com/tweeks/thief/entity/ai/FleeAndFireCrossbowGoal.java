package com.tweeks.thief.entity.ai;

import com.tweeks.thief.entity.RevealState;
import com.tweeks.thief.entity.ThiefEntity;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;

/**
 * Crossbow attack goal that only activates in {@link RevealState#REVEALED_RANGED}.
 * Kiting is handled implicitly by RangedCrossbowAttackGoal's stand-and-fire
 * with a back-step when the target closes — fine for v1.
 */
public class FleeAndFireCrossbowGoal extends RangedCrossbowAttackGoal<ThiefEntity> {

    private final ThiefEntity thief;

    public FleeAndFireCrossbowGoal(ThiefEntity thief) {
        super(thief, /*speedModifier=*/ 1.0, /*attackRadius=*/ 16.0f);
        this.thief = thief;
    }

    @Override
    public boolean canUse() {
        return thief.getRevealState() == RevealState.REVEALED_RANGED && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return thief.getRevealState() == RevealState.REVEALED_RANGED && super.canContinueToUse();
    }
}
