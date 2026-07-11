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
 * Astromech droid model: a fully custom (non-humanoid) skeleton — a
 * cylindrical body, a domed head with an eye lens, and two side legs.
 * Mirrors the class structure of wildwest's {@code CrabModel} (custom
 * {@link EntityModel} keyed to a render state), but — unlike the crab,
 * whose legs are static and only the claws animate — the astromech swings
 * its legs on the standard vanilla walk cycle.
 */
public class AstromechModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "astromech"), "main");

    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public AstromechModel(ModelPart root) {
        super(root);
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Cylindrical body approximated by an 8x10x8 box, pivot at ground.
        root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 20).addBox(-4.0f, -14.0f, -4.0f, 8, 10, 8),
            PartPose.offset(0.0f, 24.0f, 0.0f));
        // Dome head: 8x4x8 on top of the body, slightly inset.
        root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0f, -4.0f, -4.0f, 8, 4, 8)
                .texOffs(32, 0).addBox(-1.5f, -3.0f, -4.5f, 3, 2, 1),   // eye lens
            PartPose.offset(0.0f, 10.0f, 0.0f));
        // Two side legs.
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create().texOffs(32, 20).addBox(-1.0f, 0.0f, -1.5f, 2, 12, 3),
            PartPose.offset(-5.0f, 12.0f, 0.0f));
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().texOffs(42, 20).addBox(-1.0f, 0.0f, -1.5f, 2, 12, 3),
            PartPose.offset(5.0f, 12.0f, 0.0f));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        this.rightLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F)
            * 1.4F * state.walkAnimationSpeed;
        this.leftLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F + (float) Math.PI)
            * 1.4F * state.walkAnimationSpeed;
    }
}
