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
 * Tauntaun: bipedal snow lizard. 8x10x14 body leaning on two 4x12x4 hind
 * legs, stubby 3x6x3 fore-arms, a 4x6x4 neck rising from the front
 * carrying the 6x6x8 head, and a 3x3x8 tail out the back for balance.
 * Hind legs drive the walk cycle; arms and tail counter-swing. Texture
 * 64x64.
 */
public class TauntaunModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "tauntaun"), "main");

    private final ModelPart rightHind;
    private final ModelPart leftHind;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart tail;
    private final ModelPart head;

    public TauntaunModel(ModelPart root) {
        super(root);
        this.rightHind = root.getChild("right_hind");
        this.leftHind = root.getChild("left_hind");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.tail = root.getChild("tail");
        this.head = root.getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // UVs and geometry match tools/tauntaun.bbmodel exactly.
        // Body barrel (model-y 2..12).
        root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, 0.0f, -7.0f, 8, 10, 14),
            PartPose.offset(0.0f, 2.0f, 0.0f));
        // Neck rises from the body front.
        root.addOrReplaceChild("neck",
            CubeListBuilder.create().texOffs(44, 0).addBox(-2.0f, -4.0f, -2.0f, 4, 6, 4),
            PartPose.offset(0.0f, 2.0f, -5.0f));
        // Head atop the neck, muzzle forward.
        root.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(0, 24).addBox(-3.0f, -5.0f, -8.0f, 6, 6, 8),
            PartPose.offset(0.0f, -2.0f, -5.0f));
        // Hind legs carry the weight.
        root.addOrReplaceChild("right_hind",
            CubeListBuilder.create().texOffs(0, 40).addBox(-2.0f, 0.0f, -2.0f, 4, 12, 4),
            PartPose.offset(-2.0f, 12.0f, 4.0f));
        root.addOrReplaceChild("left_hind",
            CubeListBuilder.create().texOffs(16, 40).addBox(-2.0f, 0.0f, -2.0f, 4, 12, 4),
            PartPose.offset(2.0f, 12.0f, 4.0f));
        // Stubby fore-arms (boxes hang inboard of the shoulder pivots).
        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create().texOffs(44, 10).addBox(-3.0f, 0.0f, -1.5f, 3, 6, 3),
            PartPose.offset(-4.0f, 5.0f, -5.0f));
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create().texOffs(44, 19).addBox(0.0f, 0.0f, -1.5f, 3, 6, 3),
            PartPose.offset(4.0f, 5.0f, -5.0f));
        // Balancing tail.
        root.addOrReplaceChild("tail",
            CubeListBuilder.create().texOffs(28, 28).addBox(-1.5f, -1.5f, 0.0f, 3, 3, 8),
            PartPose.offset(0.0f, 9.0f, 7.0f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float swing = state.walkAnimationPos * 0.6662F;
        float amount = 1.1F * state.walkAnimationSpeed;
        this.rightHind.xRot = Mth.cos(swing) * amount;
        this.leftHind.xRot = Mth.cos(swing + (float) Math.PI) * amount;
        // Arms paddle opposite their side's leg, at half strength.
        this.rightArm.xRot = Mth.cos(swing + (float) Math.PI) * 0.5F * amount;
        this.leftArm.xRot = Mth.cos(swing) * 0.5F * amount;
        // Tail wags with the stride.
        this.tail.yRot = Mth.cos(swing) * 0.15F * state.walkAnimationSpeed;
        // Head bobs slightly.
        this.head.xRot = Mth.cos(state.walkAnimationPos * 0.3331F) * 0.05F * state.walkAnimationSpeed;
    }
}
