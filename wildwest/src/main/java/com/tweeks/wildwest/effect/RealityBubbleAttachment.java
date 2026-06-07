package com.tweeks.wildwest.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-bat attachment marking it as a "Thanos-bubble" placeholder. When
 * the timer expires, a fresh entity of {@code originalTypeId} is spawned
 * at the bat's position (at full HP — we don't persist the original
 * mob's state to keep this simple).
 *
 * <p>Fields are {@code optionalFieldOf} with empty/zero defaults so that
 * empty {@code {}} attachment maps (left in worlds from the older
 * autosave-crash bug) deserialize cleanly. An empty {@code originalTypeId}
 * is treated as "can't restore" by the tick handler — the bubble stays
 * as a bat instead of crashing the world load.
 */
public record RealityBubbleAttachment(String originalTypeId, long expiresAtTick) {
    public static final MapCodec<RealityBubbleAttachment> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.STRING.optionalFieldOf("original_type", "").forGetter(RealityBubbleAttachment::originalTypeId),
        Codec.LONG.optionalFieldOf("expires_at_tick", 0L).forGetter(RealityBubbleAttachment::expiresAtTick)
    ).apply(i, RealityBubbleAttachment::new));
}
