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
 * 74-Z speeder bike geometry (64x64), authored in the landspeeder's y-down /
 * pivot-24 convention (see {@link LandspeederModel}) so the shared vehicle
 * renderer's flip+translate places it upright on the ground. Static frame —
 * no animated sub-parts. Bone names and cube sizes match the parallel art
 * rig verbatim: {@code chassis 6x4x18}, {@code seat 4x2x6}, {@code
 * vane_left/vane_right 2x2x6}, {@code rail_left/rail_right 1x1x8}.
 */
public class SpeederBikeModel extends EntityModel<VehicleRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "speeder_bike"), "main");

    public SpeederBikeModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartPose origin = PartPose.offset(0.0f, 24.0f, 0.0f);

        root.addOrReplaceChild("chassis",
            CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, 16.0f, -9.0f, 6, 4, 18), origin);
        root.addOrReplaceChild("seat",
            CubeListBuilder.create().texOffs(0, 22).addBox(-2.0f, 14.0f, 0.0f, 4, 2, 6), origin);
        root.addOrReplaceChild("vane_left",
            CubeListBuilder.create().texOffs(20, 22).addBox(-4.0f, 16.0f, -9.0f, 2, 2, 6), origin);
        root.addOrReplaceChild("vane_right",
            CubeListBuilder.create().texOffs(20, 32).addBox(2.0f, 16.0f, -9.0f, 2, 2, 6), origin);
        root.addOrReplaceChild("rail_left",
            CubeListBuilder.create().texOffs(0, 32).addBox(-4.0f, 19.0f, -4.0f, 1, 1, 8), origin);
        root.addOrReplaceChild("rail_right",
            CubeListBuilder.create().texOffs(0, 42).addBox(3.0f, 19.0f, -4.0f, 1, 1, 8), origin);

        return LayerDefinition.create(mesh, 64, 64);
    }
}
