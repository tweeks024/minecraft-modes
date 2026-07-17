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
 * Rancor: the caged horror — a hunched bipedal reptile roughly 4.2 blocks
 * tall. A thick 22x30x14 torso over stubby 10x20x9 legs, a big 18x15x14 head
 * carrying a heavy 20x4x5 brow, a jutting 14x6x10 underbite jaw and two
 * upward tusks; long 8x28x8 arms hung wide of the torso with 9x5x9 knuckle
 * claws that swing below the body line, and a short 6x6x12 tail. Arms and
 * legs animate on the vanilla walk cycle (knuckle-swing, arms opposite legs);
 * the head keeps a slow menacing sway even at rest. Texture 128x128.
 *
 * Geometry + UVs match tools/rancor.bbmodel and gen_bbmodels.py RANCOR_CUBES
 * exactly (feet at model-y 24, built upward on negative model-y).
 */
public class RancorModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "rancor"), "main");

    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public RancorModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Thick torso: belly at model-y 6, shoulders up at -24.
        root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-11.0f, -30.0f, -7.0f, 22, 30, 14),
            PartPose.offset(0.0f, 6.0f, 0.0f));

        // Big head thrust forward off the shoulders, carrying brow/jaw/tusks.
        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(0, 44).addBox(-9.0f, -13.0f, -15.0f, 18, 15, 14),
            PartPose.offset(0.0f, -26.0f, -4.0f));
        head.addOrReplaceChild("brow",
            CubeListBuilder.create().texOffs(64, 86).addBox(-10.0f, -16.0f, -16.0f, 20, 4, 5),
            PartPose.ZERO);
        head.addOrReplaceChild("jaw",
            CubeListBuilder.create().texOffs(0, 109).addBox(-7.0f, 2.0f, -18.0f, 14, 6, 10),
            PartPose.ZERO);
        head.addOrReplaceChild("right_tusk",
            CubeListBuilder.create().texOffs(120, 0).addBox(-6.0f, -4.0f, -18.0f, 2, 6, 2),
            PartPose.ZERO);
        head.addOrReplaceChild("left_tusk",
            CubeListBuilder.create().texOffs(120, 8).addBox(4.0f, -4.0f, -18.0f, 2, 6, 2),
            PartPose.ZERO);

        // Long arms hung wide of the chest; knuckle claws hang below the fists.
        PartDefinition rightArm = root.addOrReplaceChild("right_arm",
            CubeListBuilder.create().texOffs(0, 73).addBox(-4.0f, -2.0f, -4.0f, 8, 28, 8),
            PartPose.offset(-16.0f, -22.0f, -1.0f));
        rightArm.addOrReplaceChild("right_claw",
            CubeListBuilder.create().texOffs(84, 58).addBox(-4.5f, 26.0f, -5.0f, 9, 5, 9),
            PartPose.ZERO);
        PartDefinition leftArm = root.addOrReplaceChild("left_arm",
            CubeListBuilder.create().texOffs(32, 73).addBox(-4.0f, -2.0f, -4.0f, 8, 28, 8),
            PartPose.offset(16.0f, -22.0f, -1.0f));
        leftArm.addOrReplaceChild("left_claw",
            CubeListBuilder.create().texOffs(84, 72).addBox(-4.5f, 26.0f, -5.0f, 9, 5, 9),
            PartPose.ZERO);

        // Stubby thick legs, feet on the ground plane (model-y 24).
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create().texOffs(72, 0).addBox(-5.0f, 0.0f, -4.5f, 10, 20, 9),
            PartPose.offset(-6.0f, 4.0f, 1.0f));
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().texOffs(72, 29).addBox(-5.0f, 0.0f, -4.5f, 10, 20, 9),
            PartPose.offset(6.0f, 4.0f, 1.0f));

        // Short tail off the lower back.
        root.addOrReplaceChild("tail",
            CubeListBuilder.create().texOffs(48, 109).addBox(-3.0f, 0.0f, 0.0f, 6, 6, 12),
            PartPose.offset(0.0f, 0.0f, 7.0f));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float swing = state.walkAnimationPos * 0.5F;      // slow, lumbering stride
        float amount = 0.7F * state.walkAnimationSpeed;
        this.rightLeg.xRot = Mth.cos(swing) * amount;
        this.leftLeg.xRot = Mth.cos(swing + (float) Math.PI) * amount;
        // Knuckle-swing arms, opposite the legs.
        this.rightArm.xRot = Mth.cos(swing + (float) Math.PI) * 0.6F * amount;
        this.leftArm.xRot = Mth.cos(swing) * 0.6F * amount;
        // Slow menacing head sway, on even when idle.
        this.head.yRot = Mth.cos(state.ageInTicks * 0.05F) * 0.15F;
        this.head.xRot = Mth.sin(state.ageInTicks * 0.03F) * 0.06F;
    }
}
