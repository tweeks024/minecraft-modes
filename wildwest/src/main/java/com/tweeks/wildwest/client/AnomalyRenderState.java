package com.tweeks.wildwest.client;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/**
 * Client-side render state for the Anomaly. Carries the synced
 * {@code DATA_REVEALED} flag from {@link com.tweeks.wildwest.entity.AnomalyEntity}
 * so the model can swing the lower jaw open on reveal.
 */
public class AnomalyRenderState extends HumanoidRenderState {
    public boolean revealed = false;
}
