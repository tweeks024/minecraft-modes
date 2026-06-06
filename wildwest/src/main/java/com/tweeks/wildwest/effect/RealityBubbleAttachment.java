package com.tweeks.wildwest.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Per-bat attachment marking it as a "Thanos-bubble" placeholder. When
 * the timer expires, a fresh entity of {@code originalTypeId} is spawned
 * at the bat's position (at full HP — we don't persist the original
 * mob's state to keep this simple).
 */
public record RealityBubbleAttachment(String originalTypeId, long expiresAtTick) {
    public static final MapCodec<RealityBubbleAttachment> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.STRING.fieldOf("original_type").forGetter(RealityBubbleAttachment::originalTypeId),
        Codec.LONG.fieldOf("expires_at_tick").forGetter(RealityBubbleAttachment::expiresAtTick)
    ).apply(i, RealityBubbleAttachment::new));
}
