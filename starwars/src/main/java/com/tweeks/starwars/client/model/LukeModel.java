package com.tweeks.starwars.client.model;

import com.tweeks.starwars.StarWarsMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Luke Skywalker: plain humanoid skeleton, no accessory cubes — his black
 * ROTJ tunic, blond hair, and glove stripe are painted directly onto the
 * standard humanoid UV layout. Kept as its own model class (rather than
 * reusing {@code HumanoidModel} bare) so future edits (e.g. a cape or
 * mechanical-hand glove cube) have a dedicated home, matching the sibling
 * mobs' pattern.
 */
public class LukeModel extends HumanoidModel<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "luke_skywalker"),
        "main");

    public LukeModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
