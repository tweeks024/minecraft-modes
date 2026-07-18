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
 * Emperor Palpatine: a hunched old Sith drowned in a heavy hooded robe. A
 * near-black cowl (8x8x7 shell, front-lower window painted transparent)
 * shadows a small 6x6x5 pale gnarled face sunk into the collar; a 9x11x5
 * robe torso flares into an 11x14x7 floor-length skirt that reaches the
 * ground and hides the 3x4x3 leg stubs; 4x10x4 draped sleeves hang off the
 * shoulders with 3x4x3 bony hands emerging forward at the cuffs (where
 * {@code PalpatineLightningGoal}'s arcs spawn). Nothing articulates on a
 * real walk cycle — he stands and gestures. setupAnim drives a slow idle
 * robe sway, a stooped head, and the caster's raised-sleeve posture.
 *
 * Geometry + UVs match tools/palpatine.bbmodel and gen_bbmodels.py
 * PALPATINE_CUBES exactly (feet at model-y 24, built upward on the
 * java_to_bbmodel convention shared by every other custom-skeleton rig).
 * Texture 64x64.
 */
public class PalpatineModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "palpatine"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public PalpatineModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Robe torso (world-y 13..24) flaring into a floor-length skirt
        // (world-y 0..14). Both ride the body bone so they sway as one robe.
        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(36, 0).addBox(-4.5f, 0.0f, -3.0f, 9, 11, 5),
            PartPose.offset(0.0f, 0.0f, 0.0f));
        body.addOrReplaceChild("robe_skirt",
            CubeListBuilder.create().texOffs(0, 0).addBox(-5.5f, 10.0f, -3.5f, 11, 14, 7),
            PartPose.ZERO);

        // Small pale face (world-y 22..28) sunk into the collar, wrapped by a
        // heavier cowl shell (world-y 21..29). The cowl's front-lower window
        // is painted transparent so the shadowed face peeks out beneath it.
        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(30, 21).addBox(-3.0f, -5.0f, -4.0f, 6, 6, 5),
            PartPose.offset(0.0f, 1.0f, 0.0f));
        head.addOrReplaceChild("cowl",
            CubeListBuilder.create().texOffs(0, 21).addBox(-4.0f, -6.0f, -4.5f, 8, 8, 7),
            PartPose.ZERO);

        // Wide draped sleeves off the shoulders (world-y 12..22) with bony
        // hands emerging forward at the cuffs (world-y 8..12, z ahead of the
        // sleeve front) — the spawn point for the Force-lightning arcs.
        PartDefinition rightArm = root.addOrReplaceChild("right_arm",
            CubeListBuilder.create().texOffs(0, 36).addBox(-2.0f, 0.0f, -2.0f, 4, 10, 4),
            PartPose.offset(-6.0f, 2.0f, 0.0f));
        rightArm.addOrReplaceChild("right_hand",
            CubeListBuilder.create().texOffs(44, 36).addBox(-1.5f, 10.0f, -3.5f, 3, 4, 3),
            PartPose.ZERO);
        PartDefinition leftArm = root.addOrReplaceChild("left_arm",
            CubeListBuilder.create().texOffs(16, 36).addBox(-2.0f, 0.0f, -2.0f, 4, 10, 4),
            PartPose.offset(6.0f, 2.0f, 0.0f));
        leftArm.addOrReplaceChild("left_hand",
            CubeListBuilder.create().texOffs(44, 43).addBox(-1.5f, 10.0f, -3.5f, 3, 4, 3),
            PartPose.ZERO);

        // Short leg stubs (world-y 0..4), all but hidden under the robe hem.
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create().texOffs(32, 36).addBox(-1.5f, 0.0f, -1.5f, 3, 4, 3),
            PartPose.offset(-2.0f, 20.0f, 0.0f));
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().texOffs(32, 43).addBox(-1.5f, 0.0f, -1.5f, 3, 4, 3),
            PartPose.offset(2.0f, 20.0f, 0.0f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        // Slow idle sway of the whole robed figure — the robe swishing at rest.
        this.body.zRot = Mth.cos(t * 0.045F) * 0.05F;
        this.body.yRot = Mth.sin(t * 0.03F) * 0.04F;
        // Hunched head: a forward stoop plus a slow, sinister sway.
        this.head.xRot = 0.30F + Mth.sin(t * 0.05F) * 0.05F;
        this.head.yRot = Mth.cos(t * 0.04F) * 0.10F;
        // Caster's posture: sleeves raised forward, hands held out where the
        // Force-lightning spawns, drawn together in front. A faint waver keeps
        // it menacing rather than frozen.
        float raise = -1.30F + Mth.sin(t * 0.06F) * 0.06F;
        this.rightArm.xRot = raise;
        this.leftArm.xRot = raise;
        this.rightArm.zRot = 0.18F;
        this.leftArm.zRot = -0.18F;
        // He mostly stands; only the faintest shuffle of the hidden legs.
        float swing = state.walkAnimationPos * 0.6F;
        float amount = 0.25F * state.walkAnimationSpeed;
        this.rightLeg.xRot = Mth.cos(swing) * amount;
        this.leftLeg.xRot = Mth.cos(swing + (float) Math.PI) * amount;
    }
}
