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
 * Imperial probe droid: a 10x8x10 pod floating high in the hitbox (the
 * entity itself hovers — the pod sits at the top of the 1.6-block box),
 * with a 4x4x2 eye lens front-center, a 1x6x1 antenna on top, and four
 * 1x8x1 manipulator legs dangling from the pod corners (leg0 FR, leg1 FL,
 * leg2 BR, leg3 BL). The whole pod bobs gently and the legs sway with
 * {@code ageInTicks}. Texture 64x64.
 */
public class ProbeDroidModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "probe_droid"), "main");

    private final ModelPart pod;
    private final ModelPart leg0;
    private final ModelPart leg1;
    private final ModelPart leg2;
    private final ModelPart leg3;

    public ProbeDroidModel(ModelPart root) {
        super(root);
        this.pod = root.getChild("pod");
        this.leg0 = root.getChild("leg0");
        this.leg1 = root.getChild("leg1");
        this.leg2 = root.getChild("leg2");
        this.leg3 = root.getChild("leg3");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // UVs and geometry match tools/probe_droid.bbmodel exactly.
        // Pod floats at the hitbox top (model-y -4..4).
        PartDefinition pod = root.addOrReplaceChild("pod",
            CubeListBuilder.create().texOffs(0, 0).addBox(-5.0f, -4.0f, -5.0f, 10, 8, 10),
            PartPose.offset(0.0f, 0.0f, 0.0f));
        // Eye lens, front-center of the pod face.
        pod.addOrReplaceChild("eye",
            CubeListBuilder.create().texOffs(40, 0).addBox(-2.0f, -2.0f, -7.0f, 4, 4, 2),
            PartPose.ZERO);
        // Comms antenna on top.
        pod.addOrReplaceChild("antenna",
            CubeListBuilder.create().texOffs(52, 0).addBox(-0.5f, -10.0f, -0.5f, 1, 6, 1),
            PartPose.ZERO);

        // Dangling manipulator legs at the pod corners: leg0 FR, leg1 FL,
        // leg2 BR, leg3 BL.
        CubeListBuilder legBox0 = CubeListBuilder.create().texOffs(0, 18).addBox(-0.5f, 0.0f, -0.5f, 1, 8, 1);
        CubeListBuilder legBox1 = CubeListBuilder.create().texOffs(4, 18).addBox(-0.5f, 0.0f, -0.5f, 1, 8, 1);
        CubeListBuilder legBox2 = CubeListBuilder.create().texOffs(8, 18).addBox(-0.5f, 0.0f, -0.5f, 1, 8, 1);
        CubeListBuilder legBox3 = CubeListBuilder.create().texOffs(12, 18).addBox(-0.5f, 0.0f, -0.5f, 1, 8, 1);
        root.addOrReplaceChild("leg0", legBox0, PartPose.offset(-4.0f, 4.0f, -4.0f));
        root.addOrReplaceChild("leg1", legBox1, PartPose.offset(4.0f, 4.0f, -4.0f));
        root.addOrReplaceChild("leg2", legBox2, PartPose.offset(-4.0f, 4.0f, 4.0f));
        root.addOrReplaceChild("leg3", legBox3, PartPose.offset(4.0f, 4.0f, 4.0f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        // Idle hover: pod bobs, legs drift out of phase.
        float t = state.ageInTicks;
        this.pod.y += Mth.sin(t * 0.1F) * 0.5F;
        float sway = Mth.cos(t * 0.15F) * 0.08F;
        this.leg0.xRot = sway;
        this.leg1.xRot = -sway;
        this.leg2.zRot = sway;
        this.leg3.zRot = -sway;
        // Legs hang from the pod, so they ride its bob.
        this.leg0.y += Mth.sin(t * 0.1F) * 0.5F;
        this.leg1.y += Mth.sin(t * 0.1F) * 0.5F;
        this.leg2.y += Mth.sin(t * 0.1F) * 0.5F;
        this.leg3.y += Mth.sin(t * 0.1F) * 0.5F;
    }
}
