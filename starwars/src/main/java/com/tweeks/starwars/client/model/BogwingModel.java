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
 * Bogwing: small swamp flapper. A 4x3x6 body with a 3x3x4 head at the
 * front and two broad flat 10x1x6 wings that beat continuously
 * (age-driven, bat-style). Texture 32x32.
 */
public class BogwingModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "bogwing"), "main");

    private final ModelPart rightWing;
    private final ModelPart leftWing;

    public BogwingModel(ModelPart root) {
        super(root);
        this.rightWing = root.getChild("right_wing");
        this.leftWing = root.getChild("left_wing");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // UVs and geometry match tools/bogwing.bbmodel exactly (the rig
        // rides high in model space — an airborne flier).
        root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, 0.0f, -3.0f, 4, 3, 6),
            PartPose.offset(0.0f, 7.0f, 0.0f));
        root.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(0, 9).addBox(-1.5f, -2.0f, -4.0f, 3, 3, 4),
            PartPose.offset(0.0f, 7.0f, -3.0f));
        // Wings hinge at the body sides and sweep outward.
        root.addOrReplaceChild("right_wing",
            CubeListBuilder.create().texOffs(0, 16).addBox(-10.0f, 0.0f, -3.0f, 10, 1, 6),
            PartPose.offset(-2.0f, 7.0f, 0.0f));
        root.addOrReplaceChild("left_wing",
            CubeListBuilder.create().texOffs(0, 23).addBox(0.0f, 0.0f, -3.0f, 10, 1, 6),
            PartPose.offset(2.0f, 7.0f, 0.0f));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        // Continuous flap — this thing never lands.
        float flap = Mth.cos(state.ageInTicks * 1.3F) * 0.9F;
        this.rightWing.zRot = flap;
        this.leftWing.zRot = -flap;
    }
}
