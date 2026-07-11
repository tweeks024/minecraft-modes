package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.entity.SwMob;
import com.tweeks.starwars.faction.AlignmentEvents;
import com.tweeks.starwars.faction.Disguise;
import com.tweeks.starwars.faction.SwCombatant;
import com.tweeks.starwars.faction.SwFaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

public class SwTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {

    public SwTargetGoal(SwMob mob) {
        super(mob, LivingEntity.class, 5, false, false,
            (target, level) -> {
                if (!target.isAlive()) return false;
                SwFaction myFaction = mob.getFaction();
                boolean isCombatant = target instanceof SwCombatant;
                SwFaction targetFaction = isCombatant
                    ? ((SwCombatant) target).getFaction() : SwFaction.NEUTRAL;
                boolean isPlayer = target instanceof Player;
                int score = isPlayer ? AlignmentEvents.getScore((Player) target) : 0;
                boolean disguised = isPlayer
                    && Disguise.isWearingFullStormtrooperSet(target);
                return TargetPredicates.shouldTarget(
                    myFaction, isCombatant, targetFaction, isPlayer, score, disguised);
            });
    }

    /**
     * Mind Trick gate: {@code this.mob} is the vanilla
     * {@code TargetGoal}'s protected field (confirmed against the vanilla
     * 26.1.2 source — declared {@code protected final Mob mob;}), and
     * {@code Mob} inherits attachment access from {@code Entity}, so no
     * cast to {@code SwMob} is needed here.
     */
    @Override
    public boolean canUse() {
        if (this.mob.hasData(com.tweeks.starwars.faction.ModAttachments.PACIFIED.get())) {
            var p = this.mob.getData(com.tweeks.starwars.faction.ModAttachments.PACIFIED.get());
            if (p != null && com.tweeks.starwars.faction.PacifyState.isActive(
                    p.until(), this.mob.level().getGameTime())) {
                return false;
            }
        }
        return super.canUse();
    }
}
