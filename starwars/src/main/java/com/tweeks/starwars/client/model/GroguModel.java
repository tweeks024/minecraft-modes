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
 * Grogu: tiny, on the vanilla humanoid skeleton (Yoda pattern — replaced
 * boxes, vanilla bone names so walk/swing and the riding pose animate). An
 * oversized 7x6x6 head (6..12 in bbmodel world-y) with two big 5x1x3 ears
 * flaring off the sides at head mid; a little 4x5x3 body (2..7) under a
 * 5x3x4 robe-skirt overlay; 1x4x1 arms and 1x2x1 legs. Texture 32x32. Bone
 * names + cube sizes are the hand-off contract with the parallel
 * {@code tools/grogu.bbmodel} art rig — do not rename/resize.
 */
public class GroguModel extends HumanoidModel<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "grogu"),
        "main");

    public GroguModel(ModelPart root) {
        super(root);
        this.hat.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();

        // Java bone-y = 24 - (bbmodel world-y); addBox args match the bbmodel.
        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.5f, -6.0f, -3.0f, 7, 6, 6),
            PartPose.offset(0.0f, 18.0f, 0.0f));
        // Big ears sticking straight out of the head sides, at head mid.
        head.addOrReplaceChild("right_ear",
            CubeListBuilder.create()
                .texOffs(0, 12).addBox(-8.5f, -3.5f, -1.5f, 5, 1, 3),
            PartPose.ZERO);
        head.addOrReplaceChild("left_ear",
            CubeListBuilder.create()
                .texOffs(0, 16).addBox(3.5f, -3.5f, -1.5f, 5, 1, 3),
            PartPose.ZERO);

        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create()
                .texOffs(0, 20).addBox(-2.0f, -5.0f, -1.5f, 4, 5, 3),
            PartPose.offset(0.0f, 22.0f, 0.0f));
        // Robe hem draping over the leg tops.
        body.addOrReplaceChild("robe_skirt",
            CubeListBuilder.create()
                .texOffs(14, 20).addBox(-2.5f, -1.0f, -2.0f, 5, 3, 4),
            PartPose.ZERO);

        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create()
                .texOffs(16, 12).addBox(-0.5f, 0.0f, -0.5f, 1, 4, 1),
            PartPose.offset(-2.5f, 17.0f, 0.0f));
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create()
                .texOffs(20, 12).addBox(-0.5f, 0.0f, -0.5f, 1, 4, 1),
            PartPose.offset(2.5f, 17.0f, 0.0f));

        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create()
                .texOffs(24, 12).addBox(-0.5f, 0.0f, -0.5f, 1, 2, 1),
            PartPose.offset(-1.0f, 22.0f, 0.0f));
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create()
                .texOffs(28, 12).addBox(-0.5f, 0.0f, -0.5f, 1, 2, 1),
            PartPose.offset(1.0f, 22.0f, 0.0f));

        return LayerDefinition.create(mesh, 32, 32);
    }
}
