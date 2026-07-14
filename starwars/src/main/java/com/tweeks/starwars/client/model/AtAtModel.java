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
 * AT-AT (256x128, TRUE SCALE) — a four-legged Imperial walker roughly 9
 * blocks tall. Authored in the standard mob convention (feet at model-y 24,
 * body built high on negative model-y, the same frame the {@code Ravager}
 * uses) so {@code MobRenderer} stands it upright. The four legs swing subtly
 * on a diagonal gait (front-left with back-right); a walker's stride is
 * deliberately stiff, so the amplitude is small. Bone names/sizes match the
 * parallel art rig verbatim: {@code body 36x28x64}, {@code neck 8x6x20},
 * {@code head 14x10x18}, {@code leg_fl/fr/bl/br 8x88x8}, {@code foot 12x6x12}.
 */
public class AtAtModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "at_at"), "main");

    private final ModelPart legFrontLeft;
    private final ModelPart legFrontRight;
    private final ModelPart legBackLeft;
    private final ModelPart legBackRight;

    public AtAtModel(ModelPart root) {
        super(root);
        this.legFrontLeft = root.getChild("leg_fl");
        this.legFrontRight = root.getChild("leg_fr");
        this.legBackLeft = root.getChild("leg_bl");
        this.legBackRight = root.getChild("leg_br");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Hull spans world-up 88..116px -> model-y -92..-64.
        root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-18.0f, -28.0f, -32.0f, 36, 28, 64),
            PartPose.offset(0.0f, -64.0f, 0.0f));
        // Neck juts forward and up from the body front, carrying the head.
        root.addOrReplaceChild("neck",
            CubeListBuilder.create().texOffs(0, 92).addBox(-4.0f, -6.0f, -20.0f, 8, 6, 20),
            PartPose.offsetAndRotation(0.0f, -92.0f, -32.0f, -0.35f, 0.0f, 0.0f));
        // Command head at the neck's end.
        root.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(120, 92).addBox(-7.0f, -10.0f, -18.0f, 14, 10, 18),
            PartPose.offset(0.0f, -96.0f, -46.0f));

        // Four 88-tall legs from the body corners to the feet plane (y=24),
        // each with a broad foot nested at its base so it swings with the leg.
        addLeg(root, "leg_fl", 0, 108, -14.0f, -28.0f);
        addLeg(root, "leg_fr", 40, 108, 14.0f, -28.0f);
        addLeg(root, "leg_bl", 80, 108, -14.0f, 28.0f);
        addLeg(root, "leg_br", 120, 108, 14.0f, 28.0f);

        return LayerDefinition.create(mesh, 256, 128);
    }

    private static void addLeg(PartDefinition root, String name, int legU, int footU, float x, float z) {
        PartDefinition leg = root.addOrReplaceChild(name,
            CubeListBuilder.create().texOffs(legU, 0).addBox(-4.0f, 0.0f, -4.0f, 8, 88, 8),
            PartPose.offset(x, -64.0f, z));
        leg.addOrReplaceChild("foot",
            CubeListBuilder.create().texOffs(footU, 0).addBox(-6.0f, 82.0f, -6.0f, 12, 6, 12),
            PartPose.ZERO);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float swing = state.walkAnimationPos * 0.4F;
        float amount = 0.35F * state.walkAnimationSpeed;   // stiff, subtle gait
        this.legFrontLeft.xRot = Mth.cos(swing) * amount;
        this.legBackRight.xRot = Mth.cos(swing) * amount;
        this.legFrontRight.xRot = Mth.cos(swing + (float) Math.PI) * amount;
        this.legBackLeft.xRot = Mth.cos(swing + (float) Math.PI) * amount;
    }
}
