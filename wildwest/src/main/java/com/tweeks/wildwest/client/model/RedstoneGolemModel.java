package com.tweeks.wildwest.client.model;

import com.tweeks.wildwest.WildWestMod;
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
 * Redstone Golem model: iron-golem-shaped mesh (head, body, two arms, two
 * legs) authored from scratch off the vanilla {@code IronGolemModel}
 * proportions. Uses {@link LivingEntityRenderState} so it can be driven by
 * any vanilla mob renderer without a custom render state subclass.
 *
 * <p>Placeholder mesh — polished BlockBench version comes later. Goal here
 * is "visible, golem-shaped, walks plausibly."
 */
public class RedstoneGolemModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "redstone_golem"),
        "main");

    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public RedstoneGolemModel(ModelPart root) {
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
        root.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -12.0F, -4.0F, 8.0F, 10.0F, 8.0F),
            PartPose.offset(0.0F, -7.0F, -2.0F));
        root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 40).addBox(-9.0F, -2.0F, -6.0F, 18.0F, 12.0F, 11.0F),
            PartPose.offset(0.0F, -7.0F, 0.0F));
        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create().texOffs(60, 21).addBox(-13.0F, -2.5F, -3.0F, 4.0F, 30.0F, 6.0F),
            PartPose.offset(0.0F, -7.0F, 0.0F));
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create().texOffs(60, 58).addBox(9.0F, -2.5F, -3.0F, 4.0F, 30.0F, 6.0F),
            PartPose.offset(0.0F, -7.0F, 0.0F));
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create().texOffs(37, 0).addBox(-3.5F, -3.0F, -3.0F, 6.0F, 16.0F, 5.0F),
            PartPose.offset(-4.0F, 11.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().texOffs(60, 0).mirror().addBox(-3.5F, -3.0F, -3.0F, 6.0F, 16.0F, 5.0F),
            PartPose.offset(5.0F, 11.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float animationPos = state.walkAnimationPos;
        float animationSpeed = state.walkAnimationSpeed;

        this.head.yRot = state.yRot * (float) (Math.PI / 180.0);
        this.head.xRot = state.xRot * (float) (Math.PI / 180.0);

        this.rightLeg.xRot = -1.5F * Mth.triangleWave(animationPos, 13.0F) * animationSpeed;
        this.leftLeg.xRot = 1.5F * Mth.triangleWave(animationPos, 13.0F) * animationSpeed;
        this.rightArm.xRot = (-0.2F + 1.5F * Mth.triangleWave(animationPos, 13.0F)) * animationSpeed;
        this.leftArm.xRot = (-0.2F - 1.5F * Mth.triangleWave(animationPos, 13.0F)) * animationSpeed;
    }
}
