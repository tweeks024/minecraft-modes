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
 * Darth Maul: the standard vanilla humanoid (head 8x8x8, body 8x12x4, arms
 * 4x12x4, legs 4x12x4 — the {@link VaderModel} replaced-bones idiom, keeping
 * vanilla bone names so walk/swing/held-item animate) plus a crown of Zabrak
 * horns ringing the top of the head.
 *
 * <p><b>Horn-ring layout (reproducible — the art agent's {@code darth_maul.bbmodel}
 * matches this):</b> {@value #HORN_COUNT} horns, each a {@code 1x3x1} cube,
 * children of the {@code head} bone. Head-local space puts the crown (top of
 * the 8x8x8 head cube) at {@code y = -8}. Horn {@code i} pivots at
 * {@code (cos(theta)*R, -8, sin(theta)*R)} with
 * {@code theta = i/HORN_COUNT * 2PI} and {@code R = }{@value #HORN_RING_RADIUS};
 * its cube is {@code addBox(-0.5, -HORN_HEIGHT, -0.5, 1, 3, 1)} so it stands up
 * off the crown. Each is tilted {@value #HORN_TILT} rad radially outward
 * ({@code xRot = -tilt*sin(theta)}, {@code zRot = tilt*cos(theta)}) so the tips
 * splay like a Dathomirian Zabrak's. Bones are named {@code horn_0..horn_9};
 * UVs tile a free {@code 4x4} cell each in the unused {@code u[0..16] x v[32..44]}
 * band (between the right- and left-leg texture columns).
 */
public class MaulModel extends HumanoidModel<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "darth_maul"),
        "main");

    /** 10-horn crown (canonical Zabrak count). */
    public static final int HORN_COUNT = 10;
    public static final int HORN_HEIGHT = 3;
    public static final float HORN_RING_RADIUS = 3.0f;
    public static final float HORN_TILT = 0.35f;
    /** Head-local Y of the crown (top face of the 8x8x8 head cube). */
    private static final float CROWN_Y = -8.0f;

    public MaulModel(ModelPart root) {
        super(root);
        this.hat.visible = false;   // avoid z-fighting the horns against the hat overlay
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");

        for (int i = 0; i < HORN_COUNT; i++) {
            double theta = (i / (double) HORN_COUNT) * Math.PI * 2.0;
            float x = (float) (Math.cos(theta) * HORN_RING_RADIUS);
            float z = (float) (Math.sin(theta) * HORN_RING_RADIUS);
            // Splay each horn's tip radially outward.
            float xRot = (float) (-HORN_TILT * Math.sin(theta));
            float zRot = (float) (HORN_TILT * Math.cos(theta));
            int u = (i % 4) * 4;
            int v = 32 + (i / 4) * 4;
            head.addOrReplaceChild("horn_" + i,
                CubeListBuilder.create()
                    .texOffs(u, v)
                    .addBox(-0.5f, -HORN_HEIGHT, -0.5f, 1, HORN_HEIGHT, 1),
                PartPose.offsetAndRotation(x, CROWN_Y, z, xRot, 0.0f, zRot));
        }

        return LayerDefinition.create(mesh, 64, 64);
    }
}
