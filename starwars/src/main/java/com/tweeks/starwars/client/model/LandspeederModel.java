package com.tweeks.starwars.client.model;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.LandspeederRenderState;
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
 * X-34 landspeeder geometry — a single static "body" part, no animated
 * sub-parts (unlike {@code BoatModel}, which also bakes left/right paddles
 * that swing on rowing input; the landspeeder has nothing analogous, so —
 * mirroring wildwest's {@code CrabModel}/starwars's {@code AstromechModel}
 * pattern for fully-custom non-humanoid skeletons in this codebase more
 * closely than the boat donor — this extends {@link EntityModel} directly
 * rather than an {@code AbstractBoatModel}-style paddle-aware base).
 *
 * <p>Cube-for-cube mirror of {@code starwars/tools/gen_bbmodels.py}'s
 * {@code LANDSPEEDER_CUBES} table (the editable source), POST hull-split
 * (Task 13 carry-over fix): the original single hull cube is now
 * {@code hull_front} + {@code hull_rear} (each 16x5x13, abutting seamlessly
 * at z=0) so its box-UV unwrap fits the 64x64 canvas.
 */
public class LandspeederModel extends EntityModel<LandspeederRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "landspeeder"),
        "main");

    public LandspeederModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body",
            CubeListBuilder.create()
                // hull_front / hull_rear: split from the original single
                // 16x5x26 hull cube along z so each half's box-uv
                // footprint (58x18) fits the 64px canvas — see
                // gen_bbmodels.py's LANDSPEEDER_CUBES comment.
                .texOffs(0, 0).addBox(-8.0f, 18.9f, -13.0f, 16.0f, 5.0f, 13.0f)
                .texOffs(0, 18).addBox(-8.0f, 18.9f, 0.0f, 16.0f, 5.0f, 13.0f)
                .texOffs(28, 36).addBox(-6.0f, 19.9f, -19.0f, 12.0f, 4.0f, 6.0f)
                .texOffs(28, 54).addBox(-7.0f, 14.9f, -7.0f, 14.0f, 4.0f, 1.0f)
                .texOffs(28, 46).addBox(-7.0f, 20.9f, -4.0f, 6.0f, 2.0f, 6.0f)
                .texOffs(28, 46).addBox(1.0f, 20.9f, -4.0f, 6.0f, 2.0f, 6.0f)
                .texOffs(0, 36).addBox(-3.0f, 16.9f, 11.0f, 6.0f, 6.0f, 8.0f)
                .texOffs(0, 50).addBox(-10.0f, 17.9f, 10.0f, 5.0f, 5.0f, 9.0f)
                .texOffs(0, 50).addBox(5.0f, 17.9f, 10.0f, 5.0f, 5.0f, 9.0f),
            PartPose.offset(0.0f, 24.0f, 0.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
