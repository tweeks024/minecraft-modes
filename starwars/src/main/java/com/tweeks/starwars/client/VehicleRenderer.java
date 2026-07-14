package com.tweeks.starwars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.VehicleEntity;

/**
 * Shared renderer base for the wave-3 vehicles. Follows
 * {@link LandspeederRenderer}'s boat-donor pipeline (the
 * {@code createRenderState}/{@code extractRenderState}/{@code submit} triad
 * for a non-living {@code VehicleEntity}), minus the landspeeder's bespoke
 * hover-bob and bank-roll. It applies yaw, an optional pitch (starfighters),
 * the {@code VehicleEntity} hurt wobble, then the y-down authoring undo
 * (scale(-1,-1,1) + a per-model vertical translate — see
 * {@link LandspeederRenderer#MODEL_Y_TRANSLATE}). Concrete subclasses supply
 * the model, texture, that translate, whether the airframe pitches, and the
 * shadow radius.
 */
public abstract class VehicleRenderer<T extends VehicleEntity, M extends EntityModel<VehicleRenderState>>
        extends EntityRenderer<T, VehicleRenderState> {

    private final M model;
    private final Identifier texture;
    private final float modelYTranslate;
    private final boolean pitches;

    protected VehicleRenderer(EntityRendererProvider.Context context, M model, Identifier texture,
                              float modelYTranslate, boolean pitches, float shadowRadius) {
        super(context);
        this.model = model;
        this.texture = texture;
        this.modelYTranslate = modelYTranslate;
        this.pitches = pitches;
        this.shadowRadius = shadowRadius;
    }

    @Override
    public VehicleRenderState createRenderState() {
        return new VehicleRenderState();
    }

    @Override
    public void extractRenderState(T entity, VehicleRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot(partialTicks);
        state.xRot = this.pitches ? entity.getXRot(partialTicks) : 0.0f;
        state.hurtTime = entity.getHurtTime() - partialTicks;
        state.hurtDir = entity.getHurtDir();
        state.damageTime = Math.max(entity.getDamage() - partialTicks, 0.0F);
    }

    @Override
    public void submit(VehicleRenderState state, PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
        if (state.xRot != 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(state.xRot));
        }
        float hurt = state.hurtTime;
        if (hurt > 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(
                Mth.sin(hurt) * hurt * state.damageTime / 10.0F * state.hurtDir));
        }
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, this.modelYTranslate, 0.0F);
        submitNodeCollector.submitModel(this.model, state, poseStack, this.texture,
            state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}
