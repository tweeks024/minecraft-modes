package com.tweeks.wildwest.client.model;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.client.AnomalyRenderState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/**
 * Anomaly model: villager-shaped humanoid (head, body/robe, two arms, two
 * legs) plus a separate lower-jaw cube that swings open when the entity is
 * revealed. The jaw hinges at the bottom-front edge of the head cube
 * (y = 0, z = -4 in head-local space) so positive xRot rotates it
 * forward/downward, mouth-agape.
 *
 * <p>Placeholder geometry — real model authored in BlockBench later (see
 * {@code wildwest/tools/anomaly.bbmodel}). Goal here is "not invisible in
 * dev, with a visible jaw-open distinction on reveal."
 */
public class AnomalyModel extends HumanoidModel<AnomalyRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "anomaly"),
        "main");

    /** Open-jaw rotation when revealed (≈70°, forward/down). */
    private static final float JAW_OPEN_RADIANS = (float) Math.toRadians(70.0);

    private final ModelPart lowerJaw;

    public AnomalyModel(ModelPart root) {
        super(root);
        this.hat.visible = false;
        this.lowerJaw = this.head.getChild("lower_jaw");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        // Lower-jaw slab hinged at the lower-front edge of the head cube
        // (head cube spans y in [-8, 0], z in [-4, 4]). Pivot at (0, 0, -4)
        // so the jaw rotates outward and down on reveal.
        head.addOrReplaceChild("lower_jaw",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-4.0f, 0.0f, -4.0f, 8, 2, 8),
            PartPose.offset(0.0f, 0.0f, -4.0f));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(AnomalyRenderState state) {
        super.setupAnim(state);
        this.lowerJaw.xRot = state.revealed ? JAW_OPEN_RADIANS : 0.0f;
    }
}
