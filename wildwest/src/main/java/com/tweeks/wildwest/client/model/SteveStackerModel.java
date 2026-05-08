package com.tweeks.wildwest.client.model;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.client.SteveStackerRenderState;
import net.minecraft.client.model.EntityModel;
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
 * Steve Stacker boss model: three vanilla humanoid skeletons stacked vertically.
 *
 * <p>The bottom Steve always sits at the entity's render origin (y=0). The middle and
 * top Steves are offset upward by one and two Steve-heights respectively. As the synced
 * {@code STACK_HEIGHT} byte shrinks (handled in the entity), the upper sub-roots are
 * hidden so the visible stack drops from 3 → 2 → 1 Steves while the bottom Steve always
 * stays visible at the feet.
 */
public class SteveStackerModel extends EntityModel<SteveStackerRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "steve_stacker"),
        "main");

    /** Vanilla humanoid is ~32 model-units tall feet-to-crown. */
    private static final float STEVE_PIXEL_HEIGHT = 32.0f;

    private static final String[] HUMANOID_PARTS = {
        "head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg"
    };

    private final ModelPart steveTop;
    private final ModelPart steveMid;
    private final ModelPart steveBot;

    public SteveStackerModel(ModelPart root) {
        super(root);
        this.steveBot = root.getChild("steve_bot");
        this.steveMid = root.getChild("steve_mid");
        this.steveTop = root.getChild("steve_top");
        // Hat overlay disabled per spec — the vendored Steve skin's hat region is
        // transparent, so this is also a tiny render-cost saver (3 hats × no quads).
        this.steveBot.getChild("hat").visible = false;
        this.steveMid.getChild("hat").visible = false;
        this.steveTop.getChild("hat").visible = false;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Bottom Steve at the entity origin; middle one Steve above; top two Steves above.
        // Negative Y is up in MC model space.
        attachHumanoidAt(root, "steve_bot", 0.0f);
        attachHumanoidAt(root, "steve_mid", -STEVE_PIXEL_HEIGHT);
        attachHumanoidAt(root, "steve_top", -STEVE_PIXEL_HEIGHT * 2.0f);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /**
     * Attach a vanilla humanoid skeleton under {@code parent} at the given y-offset.
     * Reuses {@link HumanoidModel#createMesh} as the canonical source for the cube layout.
     */
    private static void attachHumanoidAt(PartDefinition parent, String name, float yOffset) {
        PartDefinition holder = parent.addOrReplaceChild(
            name,
            CubeListBuilder.create(),
            PartPose.offset(0.0f, yOffset, 0.0f));
        MeshDefinition source = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition srcRoot = source.getRoot();
        for (String childName : HUMANOID_PARTS) {
            PartDefinition child = srcRoot.getChild(childName);
            holder.addOrReplaceChild(childName, child);
        }
    }

    @Override
    public void setupAnim(SteveStackerRenderState state) {
        super.setupAnim(state);

        byte stack = state.stackHeight;
        steveBot.visible = stack >= 1;
        steveMid.visible = stack >= 2;
        steveTop.visible = stack >= 3;

        // Phase-offset wobble: top leads, bottom trails.
        animateLimbs(steveTop, state, +0.3f);
        animateLimbs(steveMid, state, 0.0f);
        animateLimbs(steveBot, state, -0.3f);
    }

    private static void animateLimbs(ModelPart steve, SteveStackerRenderState state, float phaseOffset) {
        ModelPart leftArm = steve.getChild("left_arm");
        ModelPart rightArm = steve.getChild("right_arm");
        ModelPart leftLeg = steve.getChild("left_leg");
        ModelPart rightLeg = steve.getChild("right_leg");

        float walk = state.walkAnimationPos;
        float speed = Math.min(state.walkAnimationSpeed, 1.0f);
        float swing = (float) Math.cos(walk * 0.6662f + phaseOffset) * 1.4f * speed;

        rightArm.xRot = -swing;
        leftArm.xRot = swing;
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
    }
}
