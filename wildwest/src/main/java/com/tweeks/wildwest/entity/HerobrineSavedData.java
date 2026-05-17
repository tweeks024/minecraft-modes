package com.tweeks.wildwest.entity;

import com.mojang.serialization.Codec;
import com.tweeks.wildwest.WildWestMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-server singleton record for Herobrine. Anchored to the overworld's
 * data storage so reads/writes from any dimension consult the same data.
 *
 * <p>Codec field names ({@code "Alive"}, {@code "CurrentId"}, {@code "Dimension"})
 * and {@code SavedDataType} identifier ({@code "wildwest:wildwest_herobrine"})
 * are preserved from the pre-refactor implementation so existing on-disk
 * saves continue to load without migration.
 */
public final class HerobrineSavedData extends BossSingletonSavedData {

    private static final String FILE_ID = "wildwest_herobrine";

    public static final Codec<HerobrineSavedData> CODEC = buildCodec(HerobrineSavedData::new);

    public static final SavedDataType<HerobrineSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, FILE_ID),
        HerobrineSavedData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_CUSTOM_BOSS_EVENTS
    );

    public HerobrineSavedData() {}

    public static HerobrineSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
