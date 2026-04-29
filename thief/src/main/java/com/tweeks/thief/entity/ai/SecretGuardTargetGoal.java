package com.tweeks.thief.entity.ai;

import com.tweeks.securityguard.entity.SecurityGuardEntity;
import com.tweeks.thief.entity.RevealState;
import com.tweeks.thief.entity.ThiefEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.List;

/**
 * Lets a {@link RevealState#DISGUISED} Thief target the nearest Guard within
 * 12 blocks IF no player can see the Thief. Once targeted, the Thief
 * pathfinds to within 4 blocks (blackjack range) — actual hits come from
 * BlackjackStrikeGoal once revealed by the resulting hurt event.
 *
 * <p>The player-LOS check is gated by an O(1) "any nearby player at all?"
 * lookup — when no player is in range we skip the ray-traces entirely.
 */
public class SecretGuardTargetGoal extends Goal {

    private static final int GUARD_RANGE = 12;
    private static final int PLAYER_RANGE = 32;

    private final ThiefEntity thief;
    private SecurityGuardEntity targetGuard;

    public SecretGuardTargetGoal(ThiefEntity thief) {
        this.thief = thief;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (thief.getRevealState() != RevealState.DISGUISED) return false;
        if (!isHiddenFromNearbyPlayers()) return false;
        SecurityGuardEntity guard = findNearestGuard();
        if (guard == null) return false;
        targetGuard = guard;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return targetGuard != null
            && targetGuard.isAlive()
            && thief.getRevealState() == RevealState.DISGUISED
            && isHiddenFromNearbyPlayers()
            && thief.distanceToSqr(targetGuard) < (double) GUARD_RANGE * GUARD_RANGE;
    }

    @Override
    public void start() {
        thief.setTarget(targetGuard);
    }

    @Override
    public void stop() {
        thief.setTarget(null);
        targetGuard = null;
    }

    private boolean isHiddenFromNearbyPlayers() {
        Player nearest = thief.level().getNearestPlayer(thief, PLAYER_RANGE);
        if (nearest == null) {
            return isHiddenFromAllPlayers(false, false);
        }
        List<Player> candidates = thief.level().getEntitiesOfClass(Player.class,
            thief.getBoundingBox().inflate(PLAYER_RANGE));
        boolean anyLos = false;
        for (Player p : candidates) {
            if (p.hasLineOfSight(thief)) { anyLos = true; break; }
        }
        return isHiddenFromAllPlayers(true, anyLos);
    }

    /** Pure decision: hidden iff no player exists OR no nearby player has LOS. */
    public static boolean isHiddenFromAllPlayers(boolean nearestPlayerExists, boolean anyPlayerHasLineOfSight) {
        if (!nearestPlayerExists) return true;
        return !anyPlayerHasLineOfSight;
    }

    private SecurityGuardEntity findNearestGuard() {
        List<SecurityGuardEntity> guards = thief.level().getEntitiesOfClass(
            SecurityGuardEntity.class,
            thief.getBoundingBox().inflate(GUARD_RANGE),
            SecurityGuardEntity::isAlive);
        SecurityGuardEntity nearest = null;
        double nearestDistSqr = (double) GUARD_RANGE * GUARD_RANGE;
        for (SecurityGuardEntity g : guards) {
            double d = thief.distanceToSqr(g);
            if (d < nearestDistSqr) {
                nearestDistSqr = d;
                nearest = g;
            }
        }
        return nearest;
    }
}
