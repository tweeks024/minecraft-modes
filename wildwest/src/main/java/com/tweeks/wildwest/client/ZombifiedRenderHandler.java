package com.tweeks.wildwest.client;

import com.google.common.reflect.TypeToken;
import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.effect.ModEffects;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/**
 * Applies a green-outline tint to any living entity that has the ZOMBIFIED mob effect.
 *
 * <p>In MC 26.x the legacy {@code RenderSystem.setShaderColor} API was removed;
 * color tinting is now done via {@link LivingEntityRenderer#getModelTint} using an
 * ARGB int stored in the render pipeline. Rather than subclassing every renderer,
 * we use the NeoForge render-state modifier system to flag the render state and then
 * set {@link LivingEntityRenderState#outlineColor} to a green hue in the Pre event,
 * producing a distinctive green glow on infected entities. The field is reset to its
 * vanilla value each frame inside {@code EntityRenderer.extractRenderState}, so no
 * Post cleanup is required.
 */
@EventBusSubscriber(modid = WildWestMod.MOD_ID, value = Dist.CLIENT)
public final class ZombifiedRenderHandler {
    private ZombifiedRenderHandler() {}

    /** Render-data key storing whether the entity had ZOMBIFIED active at extract time. */
    public static final ContextKey<Boolean> IS_ZOMBIFIED =
        new ContextKey<>(Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "is_zombified"));

    /** Solid green outline colour used to signal the infection. */
    private static final int GREEN_OUTLINE = 0xFF22AA22;

    /**
     * Registers the render-state modifier on the mod event bus.
     * Called from {@link ClientSetup} during
     * {@link RegisterRenderStateModifiersEvent}.
     *
     * @param event the mod-bus event (fired before any rendering begins)
     */
    @SuppressWarnings("UnstableApiUsage")
    public static void registerModifier(RegisterRenderStateModifiersEvent event) {
        // TypeToken lets the compiler resolve the E=LivingEntity, S=LivingEntityRenderState
        // bounds that the raw Class<LivingEntityRenderer> overload cannot infer.
        event.registerEntityModifier(
            new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {},
            (LivingEntity entity, LivingEntityRenderState state) -> {
                if (entity.hasEffect(ModEffects.ZOMBIFIED)) {
                    state.setRenderData(IS_ZOMBIFIED, Boolean.TRUE);
                }
            }
        );
    }

    /**
     * Before the entity is submitted for rendering, override its outline colour to
     * green if it is carrying the ZOMBIFIED effect.
     *
     * <p>The outline colour is normally set to the team colour (or 0) during
     * {@code EntityRenderer.extractRenderState}; our override is therefore applied
     * after that extraction, just before submission.</p>
     */
    @SubscribeEvent
    public static void pre(RenderLivingEvent.Pre<?, ?, ?> event) {
        Boolean flag = event.getRenderState().getRenderData(IS_ZOMBIFIED);
        if (Boolean.TRUE.equals(flag)) {
            event.getRenderState().outlineColor = GREEN_OUTLINE;
        }
    }
}
