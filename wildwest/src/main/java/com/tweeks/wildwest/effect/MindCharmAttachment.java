package com.tweeks.wildwest.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

public record MindCharmAttachment(UUID casterUuid, long expiresAtTick) {
    public static final MapCodec<MindCharmAttachment> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        UUIDUtil.CODEC.fieldOf("caster").forGetter(MindCharmAttachment::casterUuid),
        Codec.LONG.fieldOf("expires_at_tick").forGetter(MindCharmAttachment::expiresAtTick)
    ).apply(i, MindCharmAttachment::new));
}
