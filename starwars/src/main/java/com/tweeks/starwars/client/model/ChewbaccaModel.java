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
 * Chewbacca: a tall wookiee on the vanilla humanoid skeleton (the BattleDroid
 * pattern — every humanoid box replaced, bone names/pivots kept so walk,
 * swing and the held-bowcaster {@code ItemInHandLayer} all animate). Big
 * 8x8x8 head (20..28 in bbmodel world-y) with a forward 4x3x4 snout and two
 * small 1x2x1 ear cubes; a 8x14x5 chest (6..20); long 3x14x3 arms; stout
 * 3x8x3 legs (0..8). The shag and bandolier strap are texture, not geometry.
 * Texture 64x64. Bone names + cube sizes are the hand-off contract with the
 * parallel {@code tools/chewbacca.bbmodel} art rig — do not rename/resize.
 */
public class ChewbaccaModel extends HumanoidModel<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "chewbacca"),
        "main");

    public ChewbaccaModel(ModelPart root) {
        super(root);
        this.hat.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();

        // Java bone-y = 24 - (bbmodel world-y); addBox args match the bbmodel.
        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8),
            PartPose.offset(0.0f, 4.0f, 0.0f));
        // Muzzle jutting off the front of the face (mid-height).
        head.addOrReplaceChild("snout",
            CubeListBuilder.create()
                .texOffs(32, 0).addBox(-2.0f, -6.0f, -6.0f, 4, 3, 4),
            PartPose.ZERO);
        head.addOrReplaceChild("right_ear",
            CubeListBuilder.create()
                .texOffs(48, 0).addBox(-5.0f, -8.0f, -0.5f, 1, 2, 1),
            PartPose.ZERO);
        head.addOrReplaceChild("left_ear",
            CubeListBuilder.create()
                .texOffs(52, 0).addBox(4.0f, -8.0f, -0.5f, 1, 2, 1),
            PartPose.ZERO);

        root.addOrReplaceChild("body",
            CubeListBuilder.create()
                .texOffs(0, 16).addBox(-4.0f, -14.0f, -2.5f, 8, 14, 5),
            PartPose.offset(0.0f, 18.0f, 0.0f));

        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create()
                .texOffs(26, 16).addBox(-1.5f, 0.0f, -1.5f, 3, 14, 3),
            PartPose.offset(-5.5f, 4.0f, 0.0f));
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create()
                .texOffs(38, 16).addBox(-1.5f, 0.0f, -1.5f, 3, 14, 3),
            PartPose.offset(5.5f, 4.0f, 0.0f));

        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create()
                .texOffs(0, 36).addBox(-1.5f, 0.0f, -1.5f, 3, 8, 3),
            PartPose.offset(-1.5f, 16.0f, 0.0f));
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create()
                .texOffs(12, 36).addBox(-1.5f, 0.0f, -1.5f, 3, 8, 3),
            PartPose.offset(1.5f, 16.0f, 0.0f));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
