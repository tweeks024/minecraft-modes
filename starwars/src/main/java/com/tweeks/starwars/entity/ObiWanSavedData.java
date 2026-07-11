package com.tweeks.starwars.entity;

import com.mojang.serialization.Codec;
import com.tweeks.starwars.StarWarsMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-server singleton record for Obi-Wan. Anchored to the overworld's
 * data storage so reads/writes from any dimension consult the same data.
 *
 * <p>Codec field names ({@code "Alive"}, {@code "CurrentId"}, {@code "Dimension"})
 * and {@code SavedDataType} identifier ({@code "starwars:starwars_obi_wan"})
 * are preserved from the pre-refactor implementation so existing on-disk
 * saves continue to load without migration.
 */
public final class ObiWanSavedData extends NamedCharacterSavedData {

    private static final String FILE_ID = "starwars_obi_wan";

    public static final Codec<ObiWanSavedData> CODEC = buildCodec(ObiWanSavedData::new);

    public static final SavedDataType<ObiWanSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, FILE_ID),
        ObiWanSavedData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_CUSTOM_BOSS_EVENTS
    );

    public ObiWanSavedData() {}

    public static ObiWanSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
