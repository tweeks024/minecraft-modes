package com.tweeks.wildwest.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * Client-side render state for the Anomaly. Carries the synced
 * {@code DATA_REVEALED} flag from {@link com.tweeks.wildwest.entity.AnomalyEntity}
 * so the model can swing the lower jaw open on reveal.
 *
 * <p>Extends {@link LivingEntityRenderState} (not {@code HumanoidRenderState})
 * because the Anomaly now renders with the villager geometry, which has no
 * shield/arm-extension fields that HumanoidRenderState carries.
 */
public class AnomalyRenderState extends LivingEntityRenderState {
    public boolean revealed = false;
}
