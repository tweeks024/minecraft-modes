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
 * Jabba the Hutt: a giant slug, wider and longer than tall (~2 blocks tall).
 * A vast 26x20x20 body sitting belly-on-the-ground, tapering back into a
 * 16x8x16 tail; a broad toad-like 18x12x12 head with a wide mouth and big
 * lidded eyes riding up off the body front; two tiny 4x8x4 stubby arms. He
 * never really walks — setupAnim is a gentle idle: a slow belly heave, a lazy
 * head loll/look, and a faint tail undulation, all keyed off ageInTicks.
 * Texture 128x64.
 *
 * Geometry + UVs match tools/jabba.bbmodel and gen_bbmodels.py JABBA_CUBES
 * exactly (belly on the ground plane at model-y 24).
 */
public class JabbaModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "jabba"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart tail;

    public JabbaModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Vast body, belly flat on the ground plane (model-y 24), top at 4.
        root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-13.0f, 0.0f, -12.0f, 26, 20, 20),
            PartPose.offset(0.0f, 4.0f, 0.0f));

        // Broad toad head rising off the body front (jutting forward, -z).
        root.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(64, 40).addBox(-9.0f, -12.0f, -12.0f, 18, 12, 12),
            PartPose.offset(0.0f, 4.0f, -10.0f));

        // Tapering tail dragging back (+z) along the ground.
        root.addOrReplaceChild("tail",
            CubeListBuilder.create().texOffs(0, 40).addBox(-8.0f, 0.0f, 0.0f, 16, 8, 16),
            PartPose.offset(0.0f, 16.0f, 8.0f));

        // Tiny stubby arms on the body's front flanks.
        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create().texOffs(92, 0).addBox(-4.0f, 0.0f, -2.0f, 4, 8, 4),
            PartPose.offset(-13.0f, 8.0f, -6.0f));
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create().texOffs(108, 0).addBox(0.0f, 0.0f, -2.0f, 4, 8, 4),
            PartPose.offset(13.0f, 8.0f, -6.0f));

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        // Gentle idle only — Jabba holds court, he does not stride.
        float t = state.ageInTicks;
        float heave = Mth.cos(t * 0.08F) * 0.04F;   // slow belly breathing
        this.body.xRot = heave;
        this.head.xRot = -heave * 0.5F + Mth.sin(t * 0.05F) * 0.03F;  // lazy loll
        this.head.yRot = Mth.sin(t * 0.06F) * 0.10F;                  // slow look
        this.tail.yRot = Mth.cos(t * 0.05F) * 0.08F;                  // faint undulation
    }
}
