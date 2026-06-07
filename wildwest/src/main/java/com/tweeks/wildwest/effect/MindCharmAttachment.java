package com.tweeks.wildwest.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

/**
 * Tracks a mob mind-charmed by a player.
 *
 * <p>Both fields use {@code optionalFieldOf} with sentinel defaults so that
 * a previously-saved empty map {@code {}} (which can appear in worlds that
 * crashed during the older null-default-supplier autosave bug) deserializes
 * to an "already-expired" charm rather than throwing. The tick handler
 * sees the zero expiry and removes the attachment on its next pass —
 * effectively self-healing the corrupted save.
 */
public record MindCharmAttachment(UUID casterUuid, long expiresAtTick) {

    private static final UUID NIL_UUID = new UUID(0L, 0L);

    public static final MapCodec<MindCharmAttachment> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        UUIDUtil.CODEC.optionalFieldOf("caster", NIL_UUID).forGetter(MindCharmAttachment::casterUuid),
        Codec.LONG.optionalFieldOf("expires_at_tick", 0L).forGetter(MindCharmAttachment::expiresAtTick)
    ).apply(i, MindCharmAttachment::new));
}
