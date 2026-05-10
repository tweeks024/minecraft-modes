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
 * Emissive overlay drawing the Herobrine eyes texture at full brightness.
 * Extends vanilla {@code EyesLayer} — the base class's {@code submit} method
 * handles the full-bright, no-overlay draw automatically; we only supply the
 * {@code RenderType} bound to the eyes texture.
 *
 * <p><b>Intentional:</b> the eyes do not red-flash when Herobrine takes damage.
 * {@code EyesLayer.submit} uses {@code OverlayTexture.NO_OVERLAY} unconditionally,
 * bypassing the dynamic damage overlay that the parent renderer would otherwise
 * pipe through. This is on purpose — stark unblinking white eyes match the
 * Herobrine creepypasta flavor (the rest of his body still flashes red as
 * normal). Do NOT route the dynamic overlay through unless changing the design
 * intent.
 */
public class HerobrineEyesLayer
        extends EyesLayer<HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    private static final RenderType EYES = RenderTypes.eyes(
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "textures/entity/herobrine_eyes.png"));

    public HerobrineEyesLayer(RenderLayerParent<HumanoidRenderState, HumanoidModel<HumanoidRenderState>> parent) {
        super(parent);
    }

    @Override
    public RenderType renderType() {
        return EYES;
    }
}
