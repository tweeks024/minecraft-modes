package com.tweeks.starwars.entity;

import com.mojang.serialization.Codec;
import com.tweeks.starwars.StarWarsMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-server singleton record for Darth Maul. Anchored to the overworld's
 * data storage so reads/writes from any dimension consult the same data —
 * a byte-for-byte mirror of {@link VaderSavedData} (only the on-disk id
 * differs), so the singleton lifecycle is identical to every other named
 * character.
 *
 * <p>Codec field names ({@code "Alive"}, {@code "CurrentId"}, {@code "Dimension"})
 * are inherited from {@link NamedCharacterSavedData}; the {@link SavedDataType}
 * identifier is {@code "starwars:starwars_maul"}.
 */
public final class MaulSavedData extends NamedCharacterSavedData {

    private static final String FILE_ID = "starwars_maul";

    public static final Codec<MaulSavedData> CODEC = buildCodec(MaulSavedData::new);

    public static final SavedDataType<MaulSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, FILE_ID),
        MaulSavedData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_CUSTOM_BOSS_EVENTS
    );

    public MaulSavedData() {}

    public static MaulSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
