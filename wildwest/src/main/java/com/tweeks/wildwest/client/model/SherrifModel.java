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
 * Sheriff: bigger cowboy hat than the deputy, plus a sheriff-star cube
 * on the chest. The star is a thin cube on the front of the body slot.
 */
public class SherrifModel extends HumanoidModel<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "sherrif"),
        "main");

    public SherrifModel(ModelPart root) {
        super(root);
        this.hat.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("cap_brim",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-5.0f, -9.0f, -5.0f, 10, 1, 10),
            PartPose.ZERO);
        head.addOrReplaceChild("cap_crown",
            CubeListBuilder.create()
                .texOffs(32, 11)
                .addBox(-3.5f, -12.0f, -3.5f, 7, 3, 7),
            PartPose.ZERO);

        // Sheriff star on the chest — small thin cube on body front.
        // Body's coordinate origin sits at body-pose; z = -2 places the
        // star just outside the front face (body cube z spans [-2, 2]).
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("star",
            CubeListBuilder.create()
                .texOffs(56, 0)
                .addBox(-1.5f, 1.0f, -2.5f, 3, 3, 1),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
