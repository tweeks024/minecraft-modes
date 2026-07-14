package com.tweeks.starwars.client.model;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.VehicleRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/**
 * T-65 X-wing geometry (128x128), authored in the landspeeder's y-down /
 * pivot-24 convention so the shared vehicle renderer's flip+translate stands
 * it upright. The four wings are separate bones splayed in the S-foil
 * configuration — the top pair rotated {@code +12.5°} and the bottom pair
 * {@code -12.5°} about Z — matching the parallel art rig exactly. Bone names
 * and sizes verbatim: {@code fuselage 6x6x26}, {@code nose 4x4x10},
 * {@code cockpit 4x3x6}, {@code wing_tl/tr/bl/br 14x1x10}, {@code engine x4
 * 3x3x8}.
 */
public class XwingModel extends EntityModel<VehicleRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "xwing"), "main");

    /** S-foil splay, radians. */
    private static final float SPLAY = (float) Math.toRadians(12.5);

    public XwingModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartPose origin = PartPose.offset(0.0f, 24.0f, 0.0f);

        root.addOrReplaceChild("fuselage",
            CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, 13.0f, -13.0f, 6, 6, 26), origin);
        root.addOrReplaceChild("nose",
            CubeListBuilder.create().texOffs(0, 40).addBox(-2.0f, 14.0f, -23.0f, 4, 4, 10), origin);
        root.addOrReplaceChild("cockpit",
            CubeListBuilder.create().texOffs(0, 56).addBox(-2.0f, 10.0f, -3.0f, 4, 3, 6), origin);

        // S-foils: wings pivot at their rear-inboard roots and splay about Z.
        root.addOrReplaceChild("wing_tl",
            CubeListBuilder.create().texOffs(64, 0).addBox(-14.0f, 0.0f, -5.0f, 14, 1, 10),
            PartPose.offsetAndRotation(-1.0f, 13.0f, 8.0f, 0.0f, 0.0f, SPLAY));
        root.addOrReplaceChild("wing_tr",
            CubeListBuilder.create().texOffs(64, 12).addBox(0.0f, 0.0f, -5.0f, 14, 1, 10),
            PartPose.offsetAndRotation(1.0f, 13.0f, 8.0f, 0.0f, 0.0f, -SPLAY));
        root.addOrReplaceChild("wing_bl",
            CubeListBuilder.create().texOffs(64, 24).addBox(-14.0f, 0.0f, -5.0f, 14, 1, 10),
            PartPose.offsetAndRotation(-1.0f, 17.0f, 8.0f, 0.0f, 0.0f, -SPLAY));
        root.addOrReplaceChild("wing_br",
            CubeListBuilder.create().texOffs(64, 36).addBox(0.0f, 0.0f, -5.0f, 14, 1, 10),
            PartPose.offsetAndRotation(1.0f, 17.0f, 8.0f, 0.0f, 0.0f, SPLAY));

        // Engines at the four wing roots.
        root.addOrReplaceChild("engine_tl",
            CubeListBuilder.create().texOffs(96, 0).addBox(-4.0f, 11.0f, 6.0f, 3, 3, 8), origin);
        root.addOrReplaceChild("engine_tr",
            CubeListBuilder.create().texOffs(96, 12).addBox(1.0f, 11.0f, 6.0f, 3, 3, 8), origin);
        root.addOrReplaceChild("engine_bl",
            CubeListBuilder.create().texOffs(96, 24).addBox(-4.0f, 16.0f, 6.0f, 3, 3, 8), origin);
        root.addOrReplaceChild("engine_br",
            CubeListBuilder.create().texOffs(96, 36).addBox(1.0f, 16.0f, 6.0f, 3, 3, 8), origin);

        return LayerDefinition.create(mesh, 128, 128);
    }
}
