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
 * Bandit Leader: bigger hat than the bandit + bandanna + cape on the back.
 * Most decorated of the four mobs.
 */
public class BanditLeaderModel extends HumanoidModel<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "bandit_leader"),
        "main");

    public BanditLeaderModel(ModelPart root) {
        super(root);
        this.hat.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        // Wide-brim hat.
        head.addOrReplaceChild("cap_brim",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-5.5f, -9.0f, -5.5f, 11, 1, 11),
            PartPose.ZERO);
        head.addOrReplaceChild("cap_crown",
            CubeListBuilder.create()
                .texOffs(32, 12)
                .addBox(-3.5f, -12.0f, -3.5f, 7, 3, 7),
            PartPose.ZERO);
        // Bandanna over the lower face.
        head.addOrReplaceChild("bandanna",
            CubeListBuilder.create()
                .texOffs(48, 4)
                .addBox(-4.5f, -4.0f, -4.6f, 9, 4, 1),
            PartPose.ZERO);

        // Cape on the back of the body — thin slab, full body width.
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("cape",
            CubeListBuilder.create()
                .texOffs(40, 32)
                .addBox(-4.5f, 0.0f, 2.1f, 9, 12, 1),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
