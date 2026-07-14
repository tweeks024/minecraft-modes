package com.tweeks.starwars.client.model;

import com.tweeks.starwars.StarWarsMod;
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
 * Rebel trooper: a verbatim clone of the {@link StormtrooperModel} rig
 * (standard humanoid + inflated head shell + chin cube) under its own
 * layer location — the shell reads as the Endor/Hoth helmet and the chin
 * cube as its strap. Same bone names and cube sizes so the art pipeline
 * shares one rig. Texture 64x64.
 */
public class RebelTrooperModel extends HumanoidModel<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "rebel_trooper"),
        "main");

    public RebelTrooperModel(ModelPart root) {
        super(root);
        this.hat.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        // Helmet shell: inflated head box using the hat-overlay UV region.
        head.addOrReplaceChild("helmet_shell",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8, new CubeDeformation(0.6f)),
            PartPose.ZERO);
        // Chin strap cube.
        head.addOrReplaceChild("chin_vent",
            CubeListBuilder.create()
                .texOffs(56, 16)
                .addBox(-1.5f, -1.5f, -5.0f, 3, 2, 1),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
