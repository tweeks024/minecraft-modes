package com.tweeks.wildwest.entity;

import java.util.UUID;

/**
 * Mutable snapshot of the Entity 303 singleton's status. Lives outside any
 * Minecraft class so it can be unit-tested without booting the FML loader
 * (same constraint as {@link SteveStackerPhase}).
 *
 * <p>The persistent backing is {@link AgentSavedData}, which translates
 * between this state and Minecraft NBT.
 */
public final class AgentState {

    private boolean alive;
    private UUID currentId;
    private String dimensionId;

    public AgentState() {}

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

    public static AgentState copyOf(AgentState other) {
        AgentState copy = new AgentState();
        if (other.alive) {
            copy.setAlive(other.currentId, other.dimensionId);
        }
        return copy;
    }
}
