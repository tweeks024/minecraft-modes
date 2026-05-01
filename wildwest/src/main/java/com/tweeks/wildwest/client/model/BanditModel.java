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
 * Bandit: humanoid + bandanna over the lower face. The bandanna is a thin
 * cube wrapping the front of the head from chin to nose.
 */
public class BanditModel extends HumanoidModel<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "bandit"),
        "main");

    public BanditModel(ModelPart root) {
        super(root);
        this.hat.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        // Bandanna — thin slab covering the lower half of the front face.
        // Head cube spans [-4, -8, -4] to [4, 0, 4]; placing the bandanna
        // at y in [-4, 0] and z just outside the front face (z = -4.5).
        head.addOrReplaceChild("bandanna",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-4.5f, -4.0f, -4.6f, 9, 4, 1),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
