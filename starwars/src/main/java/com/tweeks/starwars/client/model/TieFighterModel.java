package com.tweeks.starwars.client.model;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.VehicleRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/**
 * TIE/ln fighter geometry (128x64): a central command ball flanked by two
 * broad hexagonal solar panels on stubby pylons. Authored in the y-down /
 * pivot-24 convention for the shared vehicle renderer. Bone names and sizes
 * verbatim from the parallel art rig: {@code ball 8x8x8}, {@code window
 * 4x4x1}, {@code pylon_left/pylon_right 4x2x2}, {@code panel_left/panel_right
 * 1x16x14}.
 */
public class TieFighterModel extends EntityModel<VehicleRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "tie_fighter"), "main");

    public TieFighterModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartPose origin = PartPose.offset(0.0f, 24.0f, 0.0f);

        root.addOrReplaceChild("ball",
            CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, 10.0f, -4.0f, 8, 8, 8), origin);
        root.addOrReplaceChild("window",
            CubeListBuilder.create().texOffs(0, 16).addBox(-2.0f, 12.0f, -5.0f, 4, 4, 1), origin);
        root.addOrReplaceChild("pylon_left",
            CubeListBuilder.create().texOffs(0, 24).addBox(-8.0f, 13.0f, -1.0f, 4, 2, 2), origin);
        root.addOrReplaceChild("pylon_right",
            CubeListBuilder.create().texOffs(0, 30).addBox(4.0f, 13.0f, -1.0f, 4, 2, 2), origin);
        root.addOrReplaceChild("panel_left",
            CubeListBuilder.create().texOffs(40, 0).addBox(-9.0f, 6.0f, -7.0f, 1, 16, 14), origin);
        root.addOrReplaceChild("panel_right",
            CubeListBuilder.create().texOffs(72, 0).addBox(8.0f, 6.0f, -7.0f, 1, 16, 14), origin);

        return LayerDefinition.create(mesh, 128, 64);
    }
}
