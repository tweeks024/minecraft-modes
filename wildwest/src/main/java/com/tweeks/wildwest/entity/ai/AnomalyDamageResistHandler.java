package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.entity.AnomalyEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Reduces incoming damage to a disguised Anomaly by 25%. Encourages the
 * surprise-then-bite rhythm: the first hit on a disguised one feels weak,
 * so the player commits — and then the maw opens.
 *
 * <p>Uses {@link LivingIncomingDamageEvent} (fires earliest in the damage
 * pipeline, exposes pre-armor amount) rather than {@code LivingDamageEvent.Pre}
 * because the resist semantics are "this hit was softer than expected before
 * any other reduction" — applying the multiplier earliest keeps the math
 * intuitive when armor/other reductions stack downstream.
 */
@EventBusSubscriber(modid = WildWestMod.MOD_ID)
public final class AnomalyDamageResistHandler {
    private AnomalyDamageResistHandler() {}

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof AnomalyEntity anomaly && !anomaly.isRevealed()) {
            event.setAmount(event.getAmount() * AnomalyEntity.DISGUISED_DAMAGE_MULTIPLIER);
        }
    }
}
