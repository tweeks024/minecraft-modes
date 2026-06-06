package com.tweeks.wildwest.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;

public record RealityBubbleAttachment(CompoundTag originalNbt, String originalTypeId, long expiresAtTick) {
    public static final MapCodec<RealityBubbleAttachment> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        CompoundTag.CODEC.fieldOf("original_nbt").forGetter(RealityBubbleAttachment::originalNbt),
        Codec.STRING.fieldOf("original_type").forGetter(RealityBubbleAttachment::originalTypeId),
        Codec.LONG.fieldOf("expires_at_tick").forGetter(RealityBubbleAttachment::expiresAtTick)
    ).apply(i, RealityBubbleAttachment::new));
}
