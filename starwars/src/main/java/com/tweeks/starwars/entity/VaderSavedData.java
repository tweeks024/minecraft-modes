package com.tweeks.starwars.entity;

import com.mojang.serialization.Codec;
import com.tweeks.starwars.StarWarsMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-server singleton record for Vader. Anchored to the overworld's
 * data storage so reads/writes from any dimension consult the same data.
 *
 * <p>Codec field names ({@code "Alive"}, {@code "CurrentId"}, {@code "Dimension"})
 * and {@code SavedDataType} identifier ({@code "starwars:starwars_vader"})
 * are preserved from the pre-refactor implementation so existing on-disk
 * saves continue to load without migration.
 */
public final class VaderSavedData extends NamedCharacterSavedData {

    private static final String FILE_ID = "starwars_vader";

    public static final Codec<VaderSavedData> CODEC = buildCodec(VaderSavedData::new);

    public static final SavedDataType<VaderSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, FILE_ID),
        VaderSavedData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_CUSTOM_BOSS_EVENTS
    );

    public VaderSavedData() {}

    public static VaderSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
