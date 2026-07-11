package com.tweeks.starwars.entity.ai;

import com.tweeks.starwars.entity.ObiWanEntity;
import com.tweeks.starwars.faction.SwCombatant;
import com.tweeks.starwars.faction.SwFaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Defensive Force push: when 2+ enemies crowd within 4 blocks, repulse
 * every enemy within 5 blocks (0.8 horizontal, 0.3 vertical). 160-tick
 * cooldown.
 */
public class ObiWanPushGoal extends Goal {

    public static final int CROWD_THRESHOLD = 2;
    public static final double CROWD_RADIUS = 4.0;
    public static final double PUSH_RADIUS = 5.0;
    public static final double PUSH_STRENGTH = 0.8;
    public static final double PUSH_LIFT = 0.3;
    public static final int COOLDOWN_TICKS = 160;

    private final ObiWanEntity obiWan;
    private int cooldown;

    public ObiWanPushGoal(ObiWanEntity obiWan) {
        this.obiWan = obiWan;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    private List<LivingEntity> enemiesWithin(double radius) {
        return obiWan.level().getEntitiesOfClass(
            LivingEntity.class,
            obiWan.getBoundingBox().inflate(radius),
            e -> e != obiWan && e.isAlive()
                && e instanceof SwCombatant c
                && c.getFaction() == SwFaction.EMPIRE);
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) { cooldown--; return false; }
        return enemiesWithin(CROWD_RADIUS).size() >= CROWD_THRESHOLD;
    }

    @Override
    public void start() {
        for (LivingEntity enemy : enemiesWithin(PUSH_RADIUS)) {
            Vec3 away = enemy.position().subtract(obiWan.position());
            Vec3 flat = new Vec3(away.x, 0, away.z).normalize().scale(PUSH_STRENGTH);
            enemy.setDeltaMovement(enemy.getDeltaMovement().add(flat.x, PUSH_LIFT, flat.z));
            enemy.hurtMarked = true;   // force velocity sync to clients
        }
        this.cooldown = COOLDOWN_TICKS;
    }

    @Override
    public boolean canContinueToUse() { return false; }
}
