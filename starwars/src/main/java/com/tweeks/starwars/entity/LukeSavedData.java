package com.tweeks.starwars.entity;

import com.mojang.serialization.Codec;
import com.tweeks.starwars.StarWarsMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-server singleton record for Luke. Anchored to the overworld's
 * data storage so reads/writes from any dimension consult the same data.
 *
 * <p>Codec field names ({@code "Alive"}, {@code "CurrentId"}, {@code "Dimension"})
 * and {@code SavedDataType} identifier ({@code "starwars:starwars_luke"})
 * are preserved from the pre-refactor implementation so existing on-disk
 * saves continue to load without migration.
 */
public final class LukeSavedData extends NamedCharacterSavedData {

    private static final String FILE_ID = "starwars_luke";

    public static final Codec<LukeSavedData> CODEC = buildCodec(LukeSavedData::new);

    public static final SavedDataType<LukeSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, FILE_ID),
        LukeSavedData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_CUSTOM_BOSS_EVENTS
    );

    public LukeSavedData() {}

    public static LukeSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
