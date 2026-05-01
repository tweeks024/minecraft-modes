package com.tweeks.wildwest.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;

/**
 * Renders the entity's MAINHAND item in its right hand.
 *
 * <p>In modern NeoForge, {@link net.minecraft.client.renderer.entity.HumanoidMobRenderer}
 * already adds a vanilla {@link net.minecraft.client.renderer.entity.layers.ItemInHandLayer}
 * that reads from the render state's auto-populated {@code rightHandItemState}.
 * This layer mirrors that behaviour but is kept as a dedicated extension point
 * so the wildwest mod can later customise hand-item transforms (e.g. holstered
 * pistol, tilted rifle) without subclassing vanilla layers. It is NOT currently
 * registered on the four mob renderers — see {@code DeputyRenderer} et al — to
 * avoid double-rendering on top of the parent's {@code ItemInHandLayer}.
 *
 * <p>The legacy MC 1.21 {@code ItemRenderer.renderStatic(...)} entrypoint is
 * gone; rendering now goes through {@link ItemStackRenderState#submit} on the
 * {@link SubmitNodeCollector} pipeline.
 */
public class WildWestHeldItemLayer
        extends RenderLayer<HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    public WildWestHeldItemLayer(
            RenderLayerParent<HumanoidRenderState, HumanoidModel<HumanoidRenderState>> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack pose,
                       SubmitNodeCollector collector,
                       int lightCoords,
                       HumanoidRenderState state,
                       float yRot,
                       float xRot) {
        ItemStackRenderState item = state.rightHandItemState;
        if (item.isEmpty()) {
            return;
        }

        pose.pushPose();
        this.getParentModel().translateToHand(state, HumanoidArm.RIGHT, pose);
        pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.translate(0.0F, 0.125F, -0.625F);
        item.submit(pose, collector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        pose.popPose();
    }
}
