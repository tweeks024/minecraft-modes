package com.tweeks.starwars.client.model;

import com.tweeks.starwars.StarWarsMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Jedi Knight: humanoid + robe skirt (hangs from the hips over the top half
 * of the legs) + hood (inflated head shell, open at the front — the face
 * texture region of the hood strip is left transparent). Vanilla hat overlay
 * hidden to avoid z-fighting with the hood.
 */
public class JediKnightModel extends HumanoidModel<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "jedi_knight"),
        "main");

    public JediKnightModel(ModelPart root) {
        super(root);
        this.hat.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.getChild("body");
        // Robe skirt: hangs from the hips over the top half of the legs.
        body.addOrReplaceChild("robe_skirt",
            CubeListBuilder.create()
                .texOffs(32, 32)
                .addBox(-4.5f, 12.0f, -2.5f, 9, 7, 5),
            PartPose.ZERO);
        PartDefinition head = root.getChild("head");
        // Hood: inflated head shell, open at the front (single box is fine —
        // the face texture region of the hood strip is left transparent).
        head.addOrReplaceChild("hood",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8, new CubeDeformation(0.5f)),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
