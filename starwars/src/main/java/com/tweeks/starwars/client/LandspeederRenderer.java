package com.tweeks.starwars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.client.model.LandspeederModel;
import com.tweeks.starwars.entity.LandspeederEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Landspeeder renderer, donor {@code AbstractBoatRenderer}/{@code BoatRenderer}
 * (the non-living, render-state-pattern renderer for a
 * {@code VehicleEntity}-family entity in this Minecraft version — the
 * {@code createRenderState}/{@code extractRenderState}/{@code submit}
 * override triad, and the {@code submit(...)} pipeline pushing a
 * {@code PoseStack} transform then calling
 * {@code submitNodeCollector.submitModel(...)}, is lifted verbatim from it).
 *
 * <p>Divergences from the boat donor (recorded per task instructions):
 * <ul>
 *   <li>Extends {@link EntityRenderer} directly with a single concrete
 *   class, skipping the {@code AbstractBoatRenderer}/{@code BoatRenderer}
 *   split — that split exists because vanilla has two concrete boat
 *   variants (plain + chest) sharing one base; the landspeeder has only one
 *   variant.</li>
 *   <li>No {@code waterPatchModel}/{@code submitTypeAdditions} — that's
 *   boat-specific (water-surface mask quad), not applicable to a
 *   ground/air hover vehicle.</li>
 *   <li>Model-space corrective transform taken from decompiled
 *   {@code LivingEntityRenderer.submit} (lines ~88-90:
 *   {@code scale(-1,-1,1)} then {@code translate(0,-1.501,0)}) rather than
 *   {@code AbstractBoatRenderer}'s ({@code translate(0,0.375,0)} up front
 *   + {@code scale(-1,-1,1)} + {@code YP(90)} at the end).
 *   {@link LandspeederModel} is authored in the vanilla MOB convention
 *   (y-down, pivot at 24, nose toward -z) that the living renderer's
 *   flip+translate exists to undo — a bare {@link EntityRenderer} applies
 *   NO such transform itself, so without one the model renders upside-down
 *   ~3 blocks above the entity. The boat's extra {@code YP(90)} does NOT
 *   apply: it compensates for {@code BoatModel}'s length-along-X cube
 *   authoring; this model's long axis is Z with the nose at -z, which
 *   {@code YP(180-yRot)} already maps onto the facing direction (at
 *   yRot=0 an entity faces +z, and the 180° turn sends model -z there;
 *   the +z turbines land at the rear). The translate constant is
 *   re-derived for this model's own authored y-range — see
 *   {@link #MODEL_Y_TRANSLATE}.</li>
 *   <li>No bubble-column tilt (boat-specific, underwater-only).</li>
 *   <li>Adds bank roll (turn banking) and hover bob — not present on boats
 *   at all — per this task's spec §5.6. Bob is a world-frame vertical
 *   translate applied before the yaw; hurt wobble and bank are applied at
 *   the same pre-flip point the boat applies its wobble, so (like the
 *   living renderer's setupRotations) they pivot at the entity origin.</li>
 * </ul>
 */
public class LandspeederRenderer extends EntityRenderer<LandspeederEntity, LandspeederRenderState> {

    /** Hover bob angular rate: 2*PI / 40-tick (~2s) period. */
    private static final float BOB_RATE = (float) (2.0 * Math.PI / 40.0);
    private static final float BOB_AMPLITUDE = 0.03F;
    private static final float MAX_BANK_ROLL_DEGREES = 12.0F;
    private static final float BANK_ROLL_GAIN = 3.0F;

    /**
     * Post-flip vertical translate, blocks — this model's analog of
     * {@code LivingEntityRenderer}'s {@code -1.501}. Derivation: a vertex
     * authored at local y=j units sits, after the body part's
     * {@code PartPose.offset(0,24,0)} pivot ({@code
     * ModelPart.translateAndRotate} translates by 24/16), at model-space
     * y_m=(24+j)/16 blocks, still y-down. submit() composes
     * yaw/wobble/bank, then {@code scale(-1,-1,1)}, then
     * {@code translate(0, T, 0)}, so a point maps to world
     * y = -(y_m + T), with the rotations pivoting at the entity origin
     * (they precede the flip+translate, exactly like
     * {@code LivingEntityRenderer.setupRotations}). The model's lowest
     * authored point is the hull/nose underside at j=23.9 (jy 18.9 + jh 5)
     * -> y_m = 47.9/16 = 2.99375; placing it at the geometry caution's
     * "~0.1 blocks above the entity origin" gives
     * T = -(2.99375 + 0.1) = -3.09375. Sanity: hull top j=18.9 -> world
     * 0.4125 (upright, above the underside); windshield j=14.9..18.9 ->
     * world 0.4125..0.6625, flush on and above the hull; seats
     * j=20.9..22.9 -> world 0.1625..0.2875, recessed into the hull.
     */
    private static final float MODEL_Y_TRANSLATE = -(47.9F / 16.0F + 0.1F); // -3.09375

    private static final Identifier TEXTURE =
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "textures/entity/landspeeder.png");

    private final LandspeederModel model;

    public LandspeederRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new LandspeederModel(context.bakeLayer(LandspeederModel.LAYER_LOCATION));
        // Boat uses 0.8; the speeder's bounding box (2.0 wide) is larger.
        this.shadowRadius = 1.0F;
    }

    @Override
    public LandspeederRenderState createRenderState() {
        return new LandspeederRenderState();
    }

    @Override
    public void extractRenderState(LandspeederEntity entity, LandspeederRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot(partialTicks);
        state.hurtTime = entity.getHurtTime() - partialTicks;
        state.hurtDir = entity.getHurtDir();
        state.damageTime = Math.max(entity.getDamage() - partialTicks, 0.0F);
        // Yaw-rate bank: raw per-tick delta (not the interpolated yRot
        // above), matching spec §5.6's "proportional to yaw rate."
        // wrapDegrees guards the +-360 spike when yRot crosses the wrap
        // boundary (e.g. 359.5 -> 0.5 is a 1-degree turn, not -359).
        float yawDelta = Mth.wrapDegrees(entity.getYRot() - entity.yRotO);
        state.bankRoll = Mth.clamp(yawDelta * BANK_ROLL_GAIN, -MAX_BANK_ROLL_DEGREES, MAX_BANK_ROLL_DEGREES);
        // ageInTicks (= tickCount + partialTicks) is set by super above.
        state.bobOffset = Mth.sin(state.ageInTicks * BOB_RATE) * BOB_AMPLITUDE;
    }

    @Override
    public void submit(LandspeederRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0F, state.bobOffset, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
        float hurt = state.hurtTime;
        if (hurt > 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(hurt) * hurt * state.damageTime / 10.0F * state.hurtDir));
        }
        if (state.bankRoll != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(state.bankRoll));
        }
        // Undo the model's y-down/mob-convention authoring — the exact
        // LivingEntityRenderer.submit flip+translate pair, with the
        // translate re-derived for this model (see MODEL_Y_TRANSLATE).
        // Applied after the rotations so those pivot at the entity origin.
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, MODEL_Y_TRANSLATE, 0.0F);
        submitNodeCollector.submitModel(
            this.model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}
