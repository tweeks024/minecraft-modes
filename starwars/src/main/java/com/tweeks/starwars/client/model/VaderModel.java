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
 * Darth Vader: humanoid + helmet dome (inflated head shell) + helmet flare
 * (the flared rim at the jaw) + a flowing cape hung off the back of the
 * torso + a chest control panel. Vanilla hat overlay hidden to avoid
 * z-fighting with the helmet dome.
 */
public class VaderModel extends HumanoidModel<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "darth_vader"),
        "main");

    public VaderModel(ModelPart root) {
        super(root);
        this.hat.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("helmet_dome",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8, new CubeDeformation(0.7f)),
            PartPose.ZERO);
        head.addOrReplaceChild("helmet_flare",
            CubeListBuilder.create()
                .texOffs(32, 16)
                .addBox(-5.0f, -2.0f, -5.0f, 10, 2, 10),
            PartPose.ZERO);
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("cape",
            CubeListBuilder.create()
                .texOffs(44, 32)
                .addBox(-4.5f, 0.0f, 2.1f, 9, 20, 1),
            PartPose.ZERO);
        body.addOrReplaceChild("chest_panel",
            CubeListBuilder.create()
                .texOffs(56, 54)
                .addBox(-2.0f, 3.0f, -2.6f, 4, 3, 1),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
