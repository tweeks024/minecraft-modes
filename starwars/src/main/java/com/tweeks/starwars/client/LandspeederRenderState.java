package com.tweeks.starwars.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Render state for {@link com.tweeks.starwars.entity.LandspeederEntity}.
 * Mirrors decompiled {@code BoatRenderState}'s shape (a non-living vehicle
 * extends {@link EntityRenderState} directly, not
 * {@code LivingEntityRenderState}) — interpolated yaw plus the
 * {@code VehicleEntity} hurt/damage wobble fields — with two
 * landspeeder-specific additions precomputed once per frame in
 * {@link LandspeederRenderer#extractRenderState}: {@code bankRoll} (turn
 * banking) and {@code bobOffset} (hover bob).
 */
public class LandspeederRenderState extends EntityRenderState {
    /** Interpolated yaw, degrees — {@code entity.getYRot(partialTicks)}. */
    public float yRot;
    public int hurtDir;
    public float hurtTime;
    public float damageTime;

    /**
     * Yaw-rate-proportional bank, degrees, clamped to spec §5.6's ±12°.
     * Computed from the entity's raw per-tick yaw delta
     * ({@code getYRot() - yRotO}), not the interpolated {@link #yRot}.
     */
    public float bankRoll;

    /**
     * Vertical hover bob, blocks — ±0.03 sinusoidal, ~2s period
     * ({@code sin(ageInTicks * 2*PI/40) * 0.03}).
     */
    public float bobOffset;
}
