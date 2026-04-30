package com.tweeks.securitycore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renders an item-shaped {@link Model} in the parent humanoid's right hand.
 * Generalised from the Guard's baton-rendering layer so future humanoid mobs
 * (e.g. Thief's blackjack) can reuse the same hand-anchoring math.
 *
 * @param <S> the render state type
 * @param <M> the parent humanoid model type whose {@code rightArm} bone we attach to
 */
public class HeldItemLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>>
        extends RenderLayer<S, M> {

    private final Model<S> heldModel;
    private final Identifier texture;
    private final float translateX;
    private final float translateY;
    private final float translateZ;
    private final float xRotationDegrees;

    /**
     * @param parent       the parent humanoid renderer
     * @param heldModel    the item-shaped model to render in the hand
     * @param texture      texture for the held model
     * @param translateX   X offset from the right-arm bone origin (block units, 1/16 = 1 pixel)
     * @param translateY   Y offset
     * @param translateZ   Z offset
     * @param xRotationDegrees  rotation about the X axis applied after translation
     */
    public HeldItemLayer(RenderLayerParent<S, M> parent,
                         Model<S> heldModel,
                         Identifier texture,
                         float translateX,
                         float translateY,
                         float translateZ,
                         float xRotationDegrees) {
        super(parent);
        this.heldModel = heldModel;
        this.texture = texture;
        this.translateX = translateX;
        this.translateY = translateY;
        this.translateZ = translateZ;
        this.xRotationDegrees = xRotationDegrees;
    }

    @Override
    public void submit(PoseStack pose,
                       SubmitNodeCollector collector,
                       int lightCoords,
                       S state,
                       float yRot,
                       float xRot) {
        pose.pushPose();
        getParentModel().rightArm.translateAndRotate(pose);
        pose.translate(translateX, translateY, translateZ);
        pose.mulPose(Axis.XP.rotationDegrees(xRotationDegrees));

        // Use the canonical RenderLayer helper. Last arg is render-order
        // priority — vanilla layers pass 1, 2, 3 to stack on top of the
        // parent body; 0 ties with the body and can leave the model
        // invisible behind arm geometry.
        renderColoredCutoutModel(heldModel, texture, pose, collector, lightCoords, state, -1, 1);

        pose.popPose();
    }
}
