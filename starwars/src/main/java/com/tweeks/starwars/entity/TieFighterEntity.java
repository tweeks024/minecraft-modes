package com.tweeks.starwars.entity;

import com.tweeks.starwars.entity.vehicle.FlightPhysics;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * TIE/ln fighter: fast and fragile. Out-runs the X-wing
 * ({@link FlightPhysics#TIE_MAX_SPEED}) but its unshielded hull breaks up
 * after only {@code 25} hull-health of damage.
 */
public class TieFighterEntity extends StarfighterEntity {

    public static final float MAX_HULL_HEALTH = 25.0f;

    public TieFighterEntity(EntityType<? extends TieFighterEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public float maxHullHealth() {
        return MAX_HULL_HEALTH;
    }

    @Override
    public double maxSpeed() {
        return FlightPhysics.TIE_MAX_SPEED;
    }
}
