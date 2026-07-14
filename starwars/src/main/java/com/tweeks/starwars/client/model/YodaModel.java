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
 * Yoda: a tiny humanoid (BattleDroid pattern — replaced boxes, vanilla
 * bone names so walk/swing/held-item animate) with the signature wide
 * 9x8x8 head at pivot (0,12,0), 3x2x1 ears riding the head, a 6x7x4 robe
 * body, 2x6x2 arms and 2x4x2 legs. Texture 64x64.
 */
public class YodaModel extends HumanoidModel<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "yoda"),
        "main");

    public YodaModel(ModelPart root) {
        super(root);
        this.hat.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();

        // UVs and geometry match tools/yoda.bbmodel exactly.
        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5f, -8.0f, -4.0f, 9, 8, 8),
            PartPose.offset(0.0f, 12.0f, 0.0f));
        // Ears stick straight out of the head sides, slightly above center.
        head.addOrReplaceChild("right_ear",
            CubeListBuilder.create()
                .texOffs(34, 0).addBox(-7.5f, -6.0f, -0.5f, 3, 2, 1),
            PartPose.ZERO);
        head.addOrReplaceChild("left_ear",
            CubeListBuilder.create()
                .texOffs(42, 0).addBox(4.5f, -6.0f, -0.5f, 3, 2, 1),
            PartPose.ZERO);

        root.addOrReplaceChild("body",
            CubeListBuilder.create()
                .texOffs(0, 16).addBox(-3.0f, -7.0f, -2.0f, 6, 7, 4),
            PartPose.offset(0.0f, 19.0f, 0.0f));
        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create()
                .texOffs(20, 16).addBox(-1.0f, -1.0f, -1.0f, 2, 6, 2),
            PartPose.offset(-4.0f, 13.0f, 0.0f));
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create()
                .texOffs(28, 16).addBox(-1.0f, -1.0f, -1.0f, 2, 6, 2),
            PartPose.offset(4.0f, 13.0f, 0.0f));
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create()
                .texOffs(36, 16).addBox(-1.0f, 0.0f, -1.0f, 2, 4, 2),
            PartPose.offset(-1.5f, 20.0f, 0.0f));
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create()
                .texOffs(44, 16).addBox(-1.0f, 0.0f, -1.0f, 2, 4, 2),
            PartPose.offset(1.5f, 20.0f, 0.0f));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
