package com.tweeks.wildwest.client;

import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.WildWestMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Client-only setup for the wildwest mod.
 *
 * <p>In MC 26.1.2 the legacy {@code ItemProperties.register(...)} API was
 * removed; the bolt-cycle rifle model swap that the plan called {@code
 * bolt_state} is now driven from the item-model JSON via the built-in
 * {@code minecraft:cooldown} numeric property selector
 * (see {@link net.minecraft.client.renderer.item.properties.numeric.Cooldown}).
 * Task 10 will define that JSON; no Java-side predicate registration is
 * needed here.
 */
@EventBusSubscriber(modid = WildWestMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BULLET.get(), BulletRenderer::new);
    }
}
