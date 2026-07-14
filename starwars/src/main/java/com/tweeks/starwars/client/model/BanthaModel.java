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
 * Bantha: massive shaggy quadruped. 14x10x22 barrel body with a 16x5x24
 * wool-skirt overlay hanging under it, a 10x9x10 head slung low at the
 * front carrying two 2x2x6 horns angled out to the sides, and four
 * 4x14x4 corner legs (leg0 FR, leg1 FL, leg2 BR, leg3 BL) that swing on
 * the vanilla walk cycle in diagonal pairs. Texture 128x64.
 */
public class BanthaModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "bantha"), "main");

    private final ModelPart leg0;
    private final ModelPart leg1;
    private final ModelPart leg2;
    private final ModelPart leg3;
    private final ModelPart head;

    public BanthaModel(ModelPart root) {
        super(root);
        this.leg0 = root.getChild("leg0");
        this.leg1 = root.getChild("leg1");
        this.leg2 = root.getChild("leg2");
        this.leg3 = root.getChild("leg3");
        this.head = root.getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // UVs and geometry match tools/bantha.bbmodel exactly.
        // Barrel body: top of the legs to 24 px up (model-y 0..10).
        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-7.0f, 0.0f, -11.0f, 14, 10, 22),
            PartPose.offset(0.0f, 0.0f, 0.0f));
        // Wool skirt: wider/longer shell hanging below the body line.
        body.addOrReplaceChild("wool_skirt",
            CubeListBuilder.create().texOffs(0, 32).addBox(-8.0f, 7.0f, -12.0f, 16, 5, 24),
            PartPose.ZERO);

        // Head slung low at the front.
        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(72, 0).addBox(-5.0f, -2.0f, -10.0f, 10, 9, 10),
            PartPose.offset(0.0f, 2.0f, -11.0f));
        // Side horns sweeping forward along the head flanks.
        head.addOrReplaceChild("right_horn",
            CubeListBuilder.create().texOffs(112, 0).addBox(-7.0f, 1.0f, -8.0f, 2, 2, 6),
            PartPose.ZERO);
        head.addOrReplaceChild("left_horn",
            CubeListBuilder.create().texOffs(112, 8).addBox(5.0f, 1.0f, -8.0f, 2, 2, 6),
            PartPose.ZERO);

        // Corner legs: leg0 front-right, leg1 front-left, leg2 back-right,
        // leg3 back-left.
        CubeListBuilder legFR = CubeListBuilder.create().texOffs(96, 19).addBox(-2.0f, 0.0f, -2.0f, 4, 14, 4);
        CubeListBuilder legFL = CubeListBuilder.create().texOffs(112, 19).addBox(-2.0f, 0.0f, -2.0f, 4, 14, 4);
        CubeListBuilder legBR = CubeListBuilder.create().texOffs(96, 37).addBox(-2.0f, 0.0f, -2.0f, 4, 14, 4);
        CubeListBuilder legBL = CubeListBuilder.create().texOffs(112, 37).addBox(-2.0f, 0.0f, -2.0f, 4, 14, 4);
        root.addOrReplaceChild("leg0", legFR, PartPose.offset(-5.0f, 10.0f, -8.0f));
        root.addOrReplaceChild("leg1", legFL, PartPose.offset(5.0f, 10.0f, -8.0f));
        root.addOrReplaceChild("leg2", legBR, PartPose.offset(-5.0f, 10.0f, 8.0f));
        root.addOrReplaceChild("leg3", legBL, PartPose.offset(5.0f, 10.0f, 8.0f));

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        // Quadruped gait: diagonal pairs in phase (cow-style).
        float swing = state.walkAnimationPos * 0.6662F;
        float amount = 0.9F * state.walkAnimationSpeed;
        this.leg0.xRot = Mth.cos(swing) * amount;
        this.leg3.xRot = Mth.cos(swing) * amount;
        this.leg1.xRot = Mth.cos(swing + (float) Math.PI) * amount;
        this.leg2.xRot = Mth.cos(swing + (float) Math.PI) * amount;
        // Ponderous head sway.
        this.head.yRot = Mth.cos(state.walkAnimationPos * 0.3331F) * 0.08F * state.walkAnimationSpeed;
    }
}
