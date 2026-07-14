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
 * Ewok: a small furry forest native on the vanilla humanoid skeleton (Grogu/
 * Jawa pattern — every vanilla box replaced, vanilla bone names kept so
 * walk/swing/look animate). One oversized 8x7x7 head cube (world-y 10..17)
 * doubles as head + hood — painted as a russet cowl framing a tan face on its
 * front (a separate inner-head + outer-hood cube pair won't box-UV-pack onto
 * 32x32, see {@code tools/gen_bbmodels.py} EWOK_CUBES) — with two 1x2x2 ear
 * bumps on its sides; a 5x6x3 body (5..11), 2x6x2 arms and 2x5x2 legs (feet
 * at world-y 0). Texture 32x32.
 *
 * <p>Bone names + cube sizes/UVs are the hand-off contract with the parallel
 * {@code tools/ewok.bbmodel} art rig (Java bone-y = 24 - bbmodel world-y) — do
 * not rename/resize/re-UV one without the other.
 */
public class EwokModel extends HumanoidModel<HumanoidRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "ewok"),
        "main");

    public EwokModel(ModelPart root) {
        super(root);
        this.hat.visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();

        // Head/hood 8x7x7 (bbmodel world-y 10..17, bone @13 -> Java 11).
        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0f, -4.0f, -3.5f, 8, 7, 7),
            PartPose.offset(0.0f, 11.0f, 0.0f));
        // Two small ear bumps on the head sides.
        head.addOrReplaceChild("ear_right",
            CubeListBuilder.create()
                .texOffs(24, 14).addBox(-5.0f, -3.0f, -1.0f, 1, 2, 2),
            PartPose.ZERO);
        head.addOrReplaceChild("ear_left",
            CubeListBuilder.create()
                .texOffs(24, 18).addBox(4.0f, -3.0f, -1.0f, 1, 2, 2),
            PartPose.ZERO);

        // Body 5x6x3 (world-y 5..11, bone @7 -> Java 17).
        root.addOrReplaceChild("body",
            CubeListBuilder.create()
                .texOffs(0, 14).addBox(-2.5f, -4.0f, -1.5f, 5, 6, 3),
            PartPose.offset(0.0f, 17.0f, 0.0f));

        // Arms 2x6x2 (world-y 5..11, shoulder bone @11 -> Java 13).
        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create()
                .texOffs(16, 14).addBox(-1.0f, 0.0f, -1.0f, 2, 6, 2),
            PartPose.offset(-3.5f, 13.0f, 0.0f));
        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create()
                .texOffs(0, 23).addBox(-1.0f, 0.0f, -1.0f, 2, 6, 2),
            PartPose.offset(3.5f, 13.0f, 0.0f));

        // Legs 2x5x2 (world-y 0..5, hip bone @5 -> Java 19).
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create()
                .texOffs(8, 23).addBox(-1.0f, 0.0f, -1.0f, 2, 5, 2),
            PartPose.offset(-1.0f, 19.0f, 0.0f));
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create()
                .texOffs(16, 23).addBox(-1.0f, 0.0f, -1.0f, 2, 5, 2),
            PartPose.offset(1.0f, 19.0f, 0.0f));

        return LayerDefinition.create(mesh, 32, 32);
    }
}
