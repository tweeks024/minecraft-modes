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
 * Dragonsnake: low serpent — a 6x4x8 head with four 5x4x8 body segments
 * (seg0..seg3) chained straight behind it along +Z, hugging the ground.
 * Segments fishtail side-to-side with increasing phase down the chain.
 * Texture 64x64.
 */
public class DragonsnakeModel extends EntityModel<LivingEntityRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "dragonsnake"), "main");

    private final ModelPart[] segments = new ModelPart[4];

    public DragonsnakeModel(ModelPart root) {
        super(root);
        for (int i = 0; i < 4; i++) {
            this.segments[i] = root.getChild("seg" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // UVs and geometry match tools/dragonsnake.bbmodel exactly.
        // Head at the front (flat on the ground, snout at z -20).
        root.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, -2.0f, -8.0f, 6, 4, 8),
            PartPose.offset(0.0f, 22.0f, -12.0f));
        // Four segments chained straight behind.
        int[][] uv = {{28, 0}, {0, 12}, {26, 12}, {0, 24}};
        for (int i = 0; i < 4; i++) {
            root.addOrReplaceChild("seg" + i,
                CubeListBuilder.create().texOffs(uv[i][0], uv[i][1]).addBox(-2.5f, 0.0f, 0.0f, 5, 4, 8),
                PartPose.offset(0.0f, 20.0f, -12.0f + i * 8.0f));
        }

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        // Fishtail: amplitude and phase grow toward the tail. Blend a slow
        // idle slither (age-based) with the movement-driven wave.
        float wave = state.walkAnimationPos * 0.6F + state.ageInTicks * 0.05F;
        float drive = 0.15F + 0.5F * state.walkAnimationSpeed;
        for (int i = 0; i < 4; i++) {
            this.segments[i].yRot = Mth.cos(wave - i * 0.6F) * drive * 0.25F * (i + 1) / 4.0F;
        }
    }
}
