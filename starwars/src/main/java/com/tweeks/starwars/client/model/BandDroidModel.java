package com.tweeks.starwars.client.model;

import com.tweeks.starwars.StarWarsMod;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Cantina band droid (64x64): a small humanoid musician holding a horn
 * (its instrument) that juts forward from the right arm. Authored in the
 * standard mob convention (feet at model-y 24). Arms and legs swing on the
 * vanilla walk cycle; the right arm — with the horn nested on its end —
 * sways as it "plays." Bone names/sizes match the parallel art rig verbatim:
 * {@code head 6x6x6} + {@code antenna 1x3x1}, {@code body 6x8x4},
 * {@code right_arm/left_arm 2x8x2}, {@code right_leg/left_leg 2x6x2},
 * {@code horn 2x2x5}.
 */
public class BandDroidModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "band_droid"), "main");

    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public BandDroidModel(ModelPart root) {
        super(root);
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Head (model-y 4..10) with a stub antenna on top (model-y 1..4).
        root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0f, -6.0f, -3.0f, 6, 6, 6)
                .texOffs(0, 12).addBox(-0.5f, -9.0f, -0.5f, 1, 3, 1),   // antenna
            PartPose.offset(0.0f, 10.0f, 0.0f));
        // Torso (model-y 10..18).
        root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(24, 0).addBox(-3.0f, 0.0f, -2.0f, 6, 8, 4),
            PartPose.offset(0.0f, 10.0f, 0.0f));
        // Right arm carries the horn (nested cube pointing forward from the hand).
        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create()
                .texOffs(0, 24).addBox(-1.0f, 0.0f, -1.0f, 2, 8, 2)
                .texOffs(20, 24).addBox(-1.0f, 6.0f, -5.0f, 2, 2, 5),   // horn
            PartPose.offset(-4.0f, 10.0f, 0.0f));
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create().texOffs(12, 24).addBox(-1.0f, 0.0f, -1.0f, 2, 8, 2),
            PartPose.offset(4.0f, 10.0f, 0.0f));
        // Legs (model-y 18..24).
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create().texOffs(32, 24).addBox(-1.0f, 0.0f, -1.0f, 2, 6, 2),
            PartPose.offset(-1.5f, 18.0f, 0.0f));
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().texOffs(40, 24).addBox(-1.0f, 0.0f, -1.0f, 2, 6, 2),
            PartPose.offset(1.5f, 18.0f, 0.0f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float swing = state.walkAnimationPos * 0.6662F;
        float amount = state.walkAnimationSpeed;
        this.rightArm.xRot = Mth.cos(swing + (float) Math.PI) * 1.0F * amount;
        this.leftArm.xRot = Mth.cos(swing) * 1.0F * amount;
        this.rightLeg.xRot = Mth.cos(swing) * 1.2F * amount;
        this.leftLeg.xRot = Mth.cos(swing + (float) Math.PI) * 1.2F * amount;
    }
}
