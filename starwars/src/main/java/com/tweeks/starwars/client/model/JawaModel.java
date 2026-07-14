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
 * Jawa: a shrunken humanoid (the BattleDroid pattern — every vanilla box
 * replaced, bone names kept so walk/swing animate). Full-size 8x8x8 head
 * (all hood) over a stubby 8x9x4 robe body, 3x9x3 arms, 3x6x3 legs;
 * stacked so the feet touch the ground: legs 18-24, body 9-18, head 1-9
 * in model space. Texture 64x64; head/body/limbs use the vanilla-standard
 * UV slots where the cube sizes match vanilla.
 */
public class JawaModel extends HumanoidModel<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "jawa"),
        "main");

    public JawaModel(ModelPart root) {
        super(root);
        this.hat.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();

        // UVs match tools/jawa.bbmodel exactly (the painted texture's
        // layout); geometry is the same rig grounded (the bbmodel floats
        // 3 px — feet planted here, UVs unaffected).
        root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8),
            PartPose.offset(0.0f, 9.0f, 0.0f));
        root.addOrReplaceChild("body",
            CubeListBuilder.create()
                .texOffs(0, 16).addBox(-4.0f, -9.0f, -2.0f, 8, 9, 4),
            PartPose.offset(0.0f, 18.0f, 0.0f));
        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create()
                .texOffs(24, 16).addBox(-1.5f, -2.0f, -1.5f, 3, 9, 3),
            PartPose.offset(-5.5f, 11.0f, 0.0f));
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create()
                .texOffs(36, 16).addBox(-1.5f, -2.0f, -1.5f, 3, 9, 3),
            PartPose.offset(5.5f, 11.0f, 0.0f));
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create()
                .texOffs(0, 32).addBox(-1.5f, 0.0f, -1.5f, 3, 6, 3),
            PartPose.offset(-1.5f, 18.0f, 0.0f));
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create()
                .texOffs(12, 32).addBox(-1.5f, 0.0f, -1.5f, 3, 6, 3),
            PartPose.offset(1.5f, 18.0f, 0.0f));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
