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
 *   <li>No {@code scale(-1,-1,1)} + {@code rotate(90)} corrective transform
 *   before submitting the model. That pair exists in
 *   {@code AbstractBoatRenderer} to compensate for {@code BoatModel}'s own
 *   sideways cube authoring; {@link LandspeederModel} is authored with the
 *   same jx/jy/jz convention as this codebase's other custom models
 *   (crab, astromech) and needs no correction — confirmed against
 *   decompiled {@code LivingEntityRenderer.setupRotations}, which uses the
 *   same bare {@code Axis.YP.rotationDegrees(180-yRot)} with no extra
 *   flip for standard mob models.</li>
 *   <li>No bubble-column tilt (boat-specific, underwater-only).</li>
 *   <li>Adds bank roll (turn banking) and hover bob — not present on boats
 *   at all — per this task's spec §5.6, applied as extra local-frame
 *   {@code PoseStack} rotations/translation around the same yaw
 *   application point the boat uses for its hurt wobble.</li>
 * </ul>
 */
public class LandspeederRenderer extends EntityRenderer<LandspeederEntity, LandspeederRenderState> {

    /** Hover bob angular rate: 2*PI / 40-tick (~2s) period. */
    private static final float BOB_RATE = (float) (2.0 * Math.PI / 40.0);
    private static final float BOB_AMPLITUDE = 0.03F;
    private static final float MAX_BANK_ROLL_DEGREES = 12.0F;
    private static final float BANK_ROLL_GAIN = 3.0F;

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
        float yawDelta = entity.getYRot() - entity.yRotO;
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
        submitNodeCollector.submitModel(
            this.model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}
