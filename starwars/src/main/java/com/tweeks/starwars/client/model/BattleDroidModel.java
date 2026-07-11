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
 * B1 battle droid: spindly 2px limbs, narrow 6x10x3 torso, elongated
 * head with a 2px front snout. Replaces every humanoid box rather than
 * adding overlays; bone names/pivots stay vanilla so walk/swing animate.
 */
public class BattleDroidModel extends HumanoidModel<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "battle_droid"),
        "main");

    public BattleDroidModel(ModelPart root) {
        super(root);
        this.hat.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0f, -6.0f, -2.0f, 4, 6, 4)   // narrow skull
                .texOffs(16, 0).addBox(-1.0f, -3.0f, -5.0f, 2, 2, 3), // snout
            PartPose.offset(0.0f, 0.0f, 0.0f));
        root.addOrReplaceChild("body",
            CubeListBuilder.create()
                .texOffs(0, 16).addBox(-3.0f, 0.0f, -1.5f, 6, 10, 3)
                .texOffs(18, 16).addBox(-1.0f, 10.0f, -1.0f, 2, 2, 2), // hip block
            PartPose.ZERO);
        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create().texOffs(32, 16).addBox(-1.0f, -1.0f, -1.0f, 2, 12, 2),
            PartPose.offset(-4.0f, 2.0f, 0.0f));
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create().texOffs(40, 16).addBox(-1.0f, -1.0f, -1.0f, 2, 12, 2),
            PartPose.offset(4.0f, 2.0f, 0.0f));
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create().texOffs(48, 16).addBox(-1.0f, 0.0f, -1.0f, 2, 12, 2),
            PartPose.offset(-1.5f, 12.0f, 0.0f));
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().texOffs(56, 16).addBox(-1.0f, 0.0f, -1.0f, 2, 12, 2),
            PartPose.offset(1.5f, 12.0f, 0.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
