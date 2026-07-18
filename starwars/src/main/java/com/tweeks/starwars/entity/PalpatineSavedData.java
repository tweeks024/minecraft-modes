package com.tweeks.starwars.entity;

import com.mojang.serialization.Codec;
import com.tweeks.starwars.StarWarsMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-server singleton record for the Emperor. Anchored to the overworld's
 * data storage so reads/writes from any dimension consult the same data —
 * mirrors {@link VaderSavedData} exactly.
 */
public final class PalpatineSavedData extends NamedCharacterSavedData {

    private static final String FILE_ID = "starwars_palpatine";

    public static final Codec<PalpatineSavedData> CODEC = buildCodec(PalpatineSavedData::new);

    public static final SavedDataType<PalpatineSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, FILE_ID),
        PalpatineSavedData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_CUSTOM_BOSS_EVENTS
    );

    public PalpatineSavedData() {}

    public static PalpatineSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
