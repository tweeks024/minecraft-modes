package com.tweeks.wildwest.client.model;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.client.AnomalyRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Villager-shaped model for the Anomaly. Mesh structure mirrors vanilla
 * {@code VillagerModel} (head with hat + nose, body with jacket, single
 * arms block, two legs) so the entity reads visually as a villager and
 * the standard villager texture maps correctly. A {@code lower_jaw} child
 * is hung off the head and swings open when the entity is revealed.
 *
 * <p>Why a copy and not a subclass of {@code VillagerModel}: vanilla
 * {@code VillagerModel extends EntityModel<VillagerRenderState>} — that
 * type parameter is hard-coded. We carry our own {@link AnomalyRenderState}
 * (no profession / biome data), so the mesh is reproduced here against
 * our render state directly.
 *
 * <p>Texture: the renderer points at vanilla
 * {@code minecraft:textures/entity/villager/villager.png}, so the UV map
 * built here matches the vanilla villager base texture every client has.
 */
public class AnomalyModel extends EntityModel<AnomalyRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "anomaly"),
        "main");

    /** Open-jaw rotation when revealed (≈70°, forward/down). */
    private static final float JAW_OPEN_RADIANS = (float) Math.toRadians(70.0);

    private final ModelPart head;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart lowerJaw;

    public AnomalyModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.lowerJaw = this.head.getChild("lower_jaw");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Head + hat + hat rim + nose (lifted from VillagerModel.createBodyModel)
        PartDefinition head = root.addOrReplaceChild(
            "head",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F),
            PartPose.ZERO);
        PartDefinition hat = head.addOrReplaceChild(
            "hat",
            CubeListBuilder.create()
                .texOffs(32, 0)
                .addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.51F)),
            PartPose.ZERO);
        hat.addOrReplaceChild(
            "hat_rim",
            CubeListBuilder.create()
                .texOffs(30, 47)
                .addBox(-8.0F, -8.0F, -6.0F, 16.0F, 16.0F, 1.0F),
            PartPose.rotation((float) (-Math.PI / 2), 0.0F, 0.0F));
        head.addOrReplaceChild(
            "nose",
            CubeListBuilder.create()
                .texOffs(24, 0)
                .addBox(-1.0F, -1.0F, -6.0F, 2.0F, 4.0F, 2.0F),
            PartPose.offset(0.0F, -2.0F, 0.0F));

        // Lower jaw: hinged at the bottom-front edge of the head cube
        // (head spans y in [-10, 0], z in [-4, 4]; pivot at (0, 0, -4)
        // so positive xRot rotates outward and down on reveal).
        // Texture coordinates aren't a villager-native zone, so the jaw
        // will render with whatever UVs the (32, 47) area maps to on the
        // vanilla villager texture — placeholder until a real model.
        head.addOrReplaceChild(
            "lower_jaw",
            CubeListBuilder.create()
                .texOffs(56, 0)
                .addBox(-4.0F, 0.0F, -4.0F, 8, 2, 8),
            PartPose.offset(0.0F, 0.0F, -4.0F));

        // Body + jacket
        PartDefinition body = root.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(16, 20)
                .addBox(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F),
            PartPose.ZERO);
        body.addOrReplaceChild(
            "jacket",
            CubeListBuilder.create()
                .texOffs(0, 38)
                .addBox(-4.0F, 0.0F, -3.0F, 8.0F, 20.0F, 6.0F, new CubeDeformation(0.5F)),
            PartPose.ZERO);

        // Single crossed-arms block at chest height (the iconic villager pose).
        root.addOrReplaceChild(
            "arms",
            CubeListBuilder.create()
                .texOffs(44, 22).addBox(-8.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F)
                .texOffs(44, 22).addBox(4.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F, true)
                .texOffs(40, 38).addBox(-4.0F, 2.0F, -2.0F, 8.0F, 4.0F, 4.0F),
            PartPose.offsetAndRotation(0.0F, 3.0F, -1.0F, -0.75F, 0.0F, 0.0F));

        // Legs
        root.addOrReplaceChild(
            "right_leg",
            CubeListBuilder.create()
                .texOffs(0, 22)
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
            PartPose.offset(-2.0F, 12.0F, 0.0F));
        root.addOrReplaceChild(
            "left_leg",
            CubeListBuilder.create()
                .texOffs(0, 22).mirror()
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
            PartPose.offset(2.0F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(AnomalyRenderState state) {
        super.setupAnim(state);
        // Head tracking (yaw/pitch from look angles)
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
        this.head.zRot = 0.0F;
        // Walk-cycle leg swing
        this.rightLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.4F * state.walkAnimationSpeed * 0.5F;
        this.leftLeg.xRot = Mth.cos(state.walkAnimationPos * 0.6662F + (float) Math.PI) * 1.4F * state.walkAnimationSpeed * 0.5F;
        this.rightLeg.yRot = 0.0F;
        this.leftLeg.yRot = 0.0F;
        // Maw opens when revealed
        this.lowerJaw.xRot = state.revealed ? JAW_OPEN_RADIANS : 0.0F;
    }
}
