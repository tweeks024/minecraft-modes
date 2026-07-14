package com.tweeks.starwars.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Shared render state for the wave-3 vehicles (speeder bike, X-wing, TIE
 * fighter). Like {@link LandspeederRenderState} it extends
 * {@link EntityRenderState} directly (a non-living {@code VehicleEntity} is
 * not a {@code LivingEntity}) and carries the interpolated orientation plus
 * the {@code VehicleEntity} hurt/damage wobble fields. {@link #xRot} is only
 * meaningful for the pitching starfighters; the hover bike leaves it at 0.
 */
public class VehicleRenderState extends EntityRenderState {
    /** Interpolated yaw, degrees. */
    public float yRot;
    /** Interpolated pitch, degrees (starfighters only). */
    public float xRot;
    public int hurtDir;
    public float hurtTime;
    public float damageTime;
}
