package com.tweeks.starwars.entity;

import com.tweeks.starwars.entity.vehicle.FlightPhysics;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * T-65 X-wing: the sturdier of the two starfighters. Takes {@code 60}
 * hull-health worth of damage before breaking up (the landspeeder-family
 * hull-health pattern) and cruises a touch slower than the nimble TIE.
 */
public class XwingEntity extends StarfighterEntity {

    public static final float MAX_HULL_HEALTH = 60.0f;

    public XwingEntity(EntityType<? extends XwingEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public float maxHullHealth() {
        return MAX_HULL_HEALTH;
    }

    @Override
    public double maxSpeed() {
        return FlightPhysics.XWING_MAX_SPEED;
    }
}
