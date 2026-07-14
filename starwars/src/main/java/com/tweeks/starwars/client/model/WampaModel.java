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
 * Wampa: hulking snow ape. 14x12x8 chest over 5x10x5 legs, a 10x8x8 head
 * with two 2x3x2 horns, and 5x14x5 gorilla arms hung wide at the
 * shoulders so their fists swing below the body line. Arms and legs
 * animate on the vanilla walk cycle. Texture 128x64.
 */
public class WampaModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "wampa"), "main");

    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart head;

    public WampaModel(ModelPart root) {
        super(root);
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.head = root.getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // UVs and geometry match tools/wampa.bbmodel exactly.
        // Chest: model-y 0..12 (legs take 14..24).
        root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-7.0f, -12.0f, -4.0f, 14, 12, 8),
            PartPose.offset(0.0f, 12.0f, 0.0f));

        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(44, 0).addBox(-5.0f, -8.0f, -4.0f, 10, 8, 8),
            PartPose.offset(0.0f, 0.0f, 0.0f));
        head.addOrReplaceChild("right_horn",
            CubeListBuilder.create().texOffs(80, 0).addBox(-6.0f, -10.0f, -1.0f, 2, 3, 2),
            PartPose.ZERO);
        head.addOrReplaceChild("left_horn",
            CubeListBuilder.create().texOffs(88, 0).addBox(4.0f, -10.0f, -1.0f, 2, 3, 2),
            PartPose.ZERO);

        // Long arms: shoulders wide of the chest, fists reaching below it.
        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create().texOffs(0, 20).addBox(-2.5f, -2.0f, -2.5f, 5, 14, 5),
            PartPose.offset(-9.5f, 2.0f, 0.0f));
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create().texOffs(20, 20).addBox(-2.5f, -2.0f, -2.5f, 5, 14, 5),
            PartPose.offset(9.5f, 2.0f, 0.0f));

        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create().texOffs(40, 20).addBox(-2.5f, 2.0f, -2.5f, 5, 10, 5),
            PartPose.offset(-3.5f, 12.0f, 0.0f));
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().texOffs(60, 20).addBox(-2.5f, 2.0f, -2.5f, 5, 10, 5),
            PartPose.offset(3.5f, 12.0f, 0.0f));

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float swing = state.walkAnimationPos * 0.6662F;
        float amount = 1.0F * state.walkAnimationSpeed;
        this.rightLeg.xRot = Mth.cos(swing) * amount;
        this.leftLeg.xRot = Mth.cos(swing + (float) Math.PI) * amount;
        // Knuckle-swing arms, opposite the legs.
        this.rightArm.xRot = Mth.cos(swing + (float) Math.PI) * 0.7F * amount;
        this.leftArm.xRot = Mth.cos(swing) * 0.7F * amount;
        // Menacing slow head roll while moving.
        this.head.yRot = Mth.cos(state.walkAnimationPos * 0.3331F) * 0.1F * state.walkAnimationSpeed;
    }
}
