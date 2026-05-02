package com.tweeks.wildwest.client;

import com.google.common.reflect.TypeToken;
import com.tweeks.wildwest.effect.ModEffects;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/**
 * Applies a green outline to any living entity that has the ZOMBIFIED mob effect.
 *
 * <p>In MC 26.x the glow post-chain (outline shader) is only run when
 * {@code haveGlowingEntities} is {@code true}, which is computed at EXTRACT
 * time by checking {@code state.outlineColor != 0}.  The modifier hook runs at
 * extract time, so we set {@link LivingEntityRenderState#outlineColor} directly
 * here — not in a {@code RenderLivingEvent.Pre} handler (which fires at submit
 * time, after the check has already been made).
 */
public final class ZombifiedRenderHandler {
    private ZombifiedRenderHandler() {}

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
        event.registerEntityModifier(
            new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {},
            (LivingEntity entity, LivingEntityRenderState state) -> {
                if (entity.hasEffect(ModEffects.ZOMBIFIED)) {
                    state.outlineColor = GREEN_OUTLINE;
                }
            }
        );
    }
}
