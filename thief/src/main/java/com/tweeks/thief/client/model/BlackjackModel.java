package com.tweeks.thief.client.model;

import com.tweeks.thief.ThiefMod;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Tiny held model for the blackjack — short stubby cylinder. */
public class BlackjackModel extends Model<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(ThiefMod.MOD_ID, "blackjack"),
        "main");

    public BlackjackModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("blackjack",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-0.5f, 0.0f, -0.5f, 1, 4, 1),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16);
    }
}
