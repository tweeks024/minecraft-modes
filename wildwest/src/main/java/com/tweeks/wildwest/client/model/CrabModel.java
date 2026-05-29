package com.tweeks.wildwest.client.model;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.client.CrabRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

public class CrabModel extends EntityModel<CrabRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "crab"), "main");

    private final ModelPart body;
    private final ModelPart clawLeft;
    private final ModelPart clawRight;

    public CrabModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.clawLeft = body.getChild("claw_left");
        this.clawRight = body.getChild("claw_right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-3, -2, -3, 6, 2, 6),
            PartPose.offset(0, 22, 0));

        body.addOrReplaceChild("claw_left",
            CubeListBuilder.create().texOffs(0, 8).addBox(0, -1, -1, 2, 2, 2),
            PartPose.offset(3, -1, -2));
        body.addOrReplaceChild("claw_right",
            CubeListBuilder.create().texOffs(0, 8).addBox(-2, -1, -1, 2, 2, 2),
            PartPose.offset(-3, -1, -2));

        for (int i = 0; i < 4; i++) {
            float z = -2.0F + i * 1.3F;
            body.addOrReplaceChild("leg_l_" + i,
                CubeListBuilder.create().texOffs(12, 8).addBox(0, 0, 0, 1, 2, 1),
                PartPose.offset(3, 0, z));
            body.addOrReplaceChild("leg_r_" + i,
                CubeListBuilder.create().texOffs(12, 8).addBox(-1, 0, 0, 1, 2, 1),
                PartPose.offset(-3, 0, z));
        }

        return LayerDefinition.create(mesh, 32, 16);
    }

    @Override
    public void setupAnim(CrabRenderState state) {
        super.setupAnim(state);
        // Pinch animation: claws scale up/down on Y axis when pinchState is active.
        // ModelPart.yScale is a public float field available in NeoForge 26.1.2.
        float scale = 1.0F;
        if (state.pinchState.isStarted()) {
            long elapsed = state.pinchState.getTimeInMillis(state.ageInTicks);
            float t = (elapsed % 500L) / 500.0F;
            scale = (t < 0.5F) ? 1.0F + (0.6F * (t * 2.0F)) : 1.0F + (0.6F * ((1.0F - t) * 2.0F));
        }
        this.clawLeft.yScale = scale;
        this.clawRight.yScale = scale;
    }
}
