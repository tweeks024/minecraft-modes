package com.tweeks.securityguard.client.model;

import com.tweeks.securityguard.SecurityGuardMod;
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

public class SecurityGuardModel extends HumanoidModel<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(SecurityGuardMod.MOD_ID, "guard"),
        "main");

    public SecurityGuardModel(ModelPart root) {
        super(root);
        // HumanoidModel auto-creates a "hat" overlay child of head whose UV
        // samples from (32..64, 0..16) — the same region our cap_brim/cap_crown
        // child cubes occupy. Leaving it visible renders an opaque navy plane
        // directly in front of the face, hiding the painted features. The
        // explicit cap parts replace it.
        this.hat.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("cap_brim",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-4.5f, -9.0f, -4.5f, 9, 1, 9),
            PartPose.ZERO);
        head.addOrReplaceChild("cap_crown",
            CubeListBuilder.create()
                .texOffs(32, 10)
                .addBox(-3.5f, -11.5f, -3.5f, 7, 2, 7),
            PartPose.ZERO);
        head.addOrReplaceChild("nose",
            CubeListBuilder.create()
                .texOffs(56, 16)
                .addBox(-1.0f, -2.0f, -5.0f, 2, 2, 2),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
