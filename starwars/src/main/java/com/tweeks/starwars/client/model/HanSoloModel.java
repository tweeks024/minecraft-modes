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
 * Han Solo: humanoid skeleton + black-vest layer cube over the torso's
 * upper half (matches han_solo.bbmodel: (-4,0,-2) 8x8x4 @ UV(32,32),
 * inflate 0.25).
 */
public class HanSoloModel extends HumanoidModel<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "han_solo"),
        "main");

    public HanSoloModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition body = mesh.getRoot().getChild("body");
        body.addOrReplaceChild("vest",
            CubeListBuilder.create()
                .texOffs(32, 32)
                .addBox(-4.0f, 0.0f, -2.0f, 8, 8, 4, new CubeDeformation(0.25f)),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
