package com.tweeks.wildwest.client;

import com.tweeks.wildwest.WildWestMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Emissive overlay drawing the The Agent eyes texture at full brightness.
 * Extends vanilla {@code EyesLayer} — the base class's {@code submit} method
 * handles the full-bright, no-overlay draw automatically; we only supply the
 * {@code RenderType} bound to the eyes texture.
 *
 * <p>The eyes render red regardless of ambient light — the iconic The Agent
 * creepypasta visual. Because {@code EyesLayer.submit} uses
 * {@code OverlayTexture.NO_OVERLAY} unconditionally, the eyes do not
 * red-flash during damage. This is intentional.
 */
public class AgentEyesLayer
        extends EyesLayer<HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    private static final RenderType EYES = RenderTypes.eyes(
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/the_agent_eyes.png"));

    public AgentEyesLayer(RenderLayerParent<HumanoidRenderState, HumanoidModel<HumanoidRenderState>> parent) {
        super(parent);
    }

    @Override
    public RenderType renderType() {
        return EYES;
    }
}
