package com.tweeks.wildwest.entity;

import java.util.UUID;

/**
 * Mutable snapshot of the Entity 303 singleton's status. Lives outside any
 * Minecraft class so it can be unit-tested without booting the FML loader
 * (same constraint as {@link SteveStackerPhase}).
 *
 * <p>The persistent backing is {@link Entity303SavedData}, which translates
 * between this state and Minecraft NBT.
 */
public final class Entity303State {

    private boolean alive;
    private UUID currentId;
    private String dimensionId;

    public Entity303State() {}

    public boolean isAlive() { return this.alive; }
    public UUID getCurrentId() { return this.currentId; }
    public String getDimensionId() { return this.dimensionId; }

    public void setAlive(UUID id, String dimensionId) {
        this.alive = true;
        this.currentId = id;
        this.dimensionId = dimensionId;
    }

    public void clear() {
        this.alive = false;
        this.currentId = null;
        this.dimensionId = null;
    }

    public static Entity303State copyOf(Entity303State other) {
        Entity303State copy = new Entity303State();
        if (other.alive) {
            copy.setAlive(other.currentId, other.dimensionId);
        }
        return copy;
    }
}
