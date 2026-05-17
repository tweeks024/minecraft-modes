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
 * Emissive overlay drawing Null's white eyes at full brightness. Same pattern
 * as {@link AgentEyesLayer} — vanilla {@code EyesLayer.submit} handles the
 * full-bright no-overlay draw; we just supply the eyes-bound {@code RenderType}.
 *
 * <p>White eyes on a pitch-black silhouette is the defining Null visual; the
 * emissive layer keeps them glowing regardless of ambient light.
 */
public class NullEyesLayer
        extends EyesLayer<HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    private static final RenderType EYES = RenderTypes.eyes(
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/null_eyes.png"));

    public NullEyesLayer(RenderLayerParent<HumanoidRenderState, HumanoidModel<HumanoidRenderState>> parent) {
        super(parent);
    }

    @Override
    public RenderType renderType() {
        return EYES;
    }
}
