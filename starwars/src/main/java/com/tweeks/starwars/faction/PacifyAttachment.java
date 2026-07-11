package com.tweeks.starwars.faction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Mind Trick pacification: the mob's targeting is suppressed until the given gameTime tick. */
public record PacifyAttachment(long until) {
    public static final MapCodec<PacifyAttachment> CODEC =
        RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.LONG.fieldOf("Until").forGetter(PacifyAttachment::until)
        ).apply(instance, PacifyAttachment::new));
}
