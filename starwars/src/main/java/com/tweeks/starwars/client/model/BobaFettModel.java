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
 * Boba Fett: humanoid + helmet shell (inflated head overlay box, same
 * profile as the stormtrooper helmet) + a T-visor rangefinder stalk + a
 * jetpack strapped to the back of the torso. Vanilla hat overlay hidden to
 * avoid z-fighting with the helmet shell.
 */
public class BobaFettModel extends HumanoidModel<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "boba_fett"),
        "main");

    public BobaFettModel(ModelPart root) {
        super(root);
        this.hat.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("helmet_shell",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8, new CubeDeformation(0.6f)),
            PartPose.ZERO);
        head.addOrReplaceChild("rangefinder",
            CubeListBuilder.create()
                .texOffs(56, 16)
                .addBox(3.8f, -12.0f, -0.5f, 1, 4, 1),
            PartPose.ZERO);
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("jetpack",
            CubeListBuilder.create()
                .texOffs(44, 32)
                .addBox(-3.0f, 0.5f, 2.1f, 6, 8, 3),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
