package com.tweeks.wildwest.entity;

import java.util.UUID;

/**
 * Mutable snapshot of a boss singleton's status. Lives outside any Minecraft
 * class so it can be unit-tested without booting the FML loader.
 *
 * <p>Shared by Herobrine, The Agent, and Null. Each boss has its own SavedData
 * subclass (extending {@link BossSingletonSavedData}) that wraps an instance
 * of this POJO under a unique on-disk identifier.
 */
public final class BossSingletonState {

    private boolean alive;
    private UUID currentId;
    private String dimensionId;

    public BossSingletonState() {}

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

    public static BossSingletonState copyOf(BossSingletonState other) {
        BossSingletonState copy = new BossSingletonState();
        if (other.alive) {
            copy.setAlive(other.currentId, other.dimensionId);
        }
        return copy;
    }
}
