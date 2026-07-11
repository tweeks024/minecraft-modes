package com.tweeks.starwars.entity;

import java.util.UUID;

/**
 * Mutable snapshot of a boss singleton's status. Lives outside any Minecraft
 * class so it can be unit-tested without booting the FML loader.
 *
 * <p>Shared by Vader, Luke, Obi-Wan, and Boba Fett. Each boss has its own SavedData
 * subclass (extending {@link NamedCharacterSavedData}) that wraps an instance
 * of this POJO under a unique on-disk identifier.
 */
public final class SingletonState {

    private boolean alive;
    private UUID currentId;
    private String dimensionId;

    public SingletonState() {}

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

    public static SingletonState copyOf(SingletonState other) {
        SingletonState copy = new SingletonState();
        if (other.alive) {
            copy.setAlive(other.currentId, other.dimensionId);
        }
        return copy;
    }
}
