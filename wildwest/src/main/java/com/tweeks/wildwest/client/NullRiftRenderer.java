package com.tweeks.wildwest.client;

import com.tweeks.wildwest.entity.NullRiftEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.NoopRenderer;

/**
 * No-op renderer for {@link NullRiftEntity}. Visuals come from server-dispatched
 * particles (see {@code NullRiftEntity.runTelegraph} / {@code runActive}); the
 * renderer exists only to satisfy NeoForge's per-entity-type renderer
 * registration requirement and avoid missing-renderer log spam.
 */
public class NullRiftRenderer extends NoopRenderer<NullRiftEntity> {

    public NullRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}
