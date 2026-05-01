package com.tweeks.wildwest.client.model;

import com.tweeks.wildwest.WildWestMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Deputy: standard humanoid + small cowboy hat. UV regions for the hat
 * occupy the head's spare overlay area (32..64, 0..16) — same trick the
 * SecurityGuardModel uses. Vanilla {@code hat} overlay is hidden so it
 * doesn't z-fight with our cap.
 */
public class DeputyModel extends HumanoidModel<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "deputy"),
        "main");

    public DeputyModel(ModelPart root) {
        super(root);
        this.hat.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        // Hat brim — wider, thinner disc across top of head.
        head.addOrReplaceChild("cap_brim",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-4.5f, -9.0f, -4.5f, 9, 1, 9),
            PartPose.ZERO);
        // Hat crown — taller bump on top.
        head.addOrReplaceChild("cap_crown",
            CubeListBuilder.create()
                .texOffs(32, 10)
                .addBox(-3.0f, -11.0f, -3.0f, 6, 2, 6),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
