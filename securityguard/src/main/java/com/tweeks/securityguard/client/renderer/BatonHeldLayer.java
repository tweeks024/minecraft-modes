package com.tweeks.securityguard.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tweeks.securityguard.SecurityGuardMod;
import com.tweeks.securityguard.client.model.BatonModel;
import com.tweeks.securityguard.client.model.SecurityGuardModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public class BatonHeldLayer extends RenderLayer<HumanoidRenderState, SecurityGuardModel> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
        SecurityGuardMod.MOD_ID, "textures/entity/baton.png");

    private final BatonModel batonModel;

    public BatonHeldLayer(RenderLayerParent<HumanoidRenderState, SecurityGuardModel> parent,
                          EntityRendererProvider.Context context) {
        super(parent);
        this.batonModel = new BatonModel(context.bakeLayer(BatonModel.LAYER_LOCATION));
    }

    @Override
    public void submit(PoseStack pose,
                       SubmitNodeCollector collector,
                       int lightCoords,
                       HumanoidRenderState state,
                       float yRot,
                       float xRot) {
        pose.pushPose();
        getParentModel().rightArm.translateAndRotate(pose);
        pose.translate(-0.0625f, 0.625f, 0.0f);
        pose.mulPose(Axis.XP.rotationDegrees(180.0f));

        collector.submitModel(
            batonModel,
            state,
            pose,
            TEXTURE,
            lightCoords,
            OverlayTexture.NO_OVERLAY,
            -1,
            null
        );

        pose.popPose();
    }
}
