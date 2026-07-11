package com.tweeks.starwars.faction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record AlignmentAttachment(int score) {
    public static final MapCodec<AlignmentAttachment> CODEC =
        RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("Score").forGetter(AlignmentAttachment::score)
        ).apply(instance, AlignmentAttachment::new));
}
