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
 * Princess Leia: humanoid skeleton + twin cinnamon-bun cubes on the head
 * (matches princess_leia.bbmodel: (-5.5,-5,-1.5) and (3.5,-5,-1.5) 2x3x3 @
 * UV(56,0)/(56,6)) + a robe-skirt layer cube on the body, same idiom as
 * {@link JediKnightModel}'s robe_skirt ((-4.5,12,-2.5) 9x7x5 @ UV(32,32)).
 */
public class PrincessLeiaModel extends HumanoidModel<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "princess_leia"),
        "main");

    public PrincessLeiaModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition head = mesh.getRoot().getChild("head");
        head.addOrReplaceChild("bun_right",
            CubeListBuilder.create().texOffs(56, 0)
                .addBox(-5.5f, -5.0f, -1.5f, 2, 3, 3),
            PartPose.ZERO);
        head.addOrReplaceChild("bun_left",
            CubeListBuilder.create().texOffs(56, 6)
                .addBox(3.5f, -5.0f, -1.5f, 2, 3, 3),
            PartPose.ZERO);
        PartDefinition body = mesh.getRoot().getChild("body");
        body.addOrReplaceChild("robe_skirt",
            CubeListBuilder.create().texOffs(32, 32)
                .addBox(-4.5f, 12.0f, -2.5f, 9, 7, 5),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
