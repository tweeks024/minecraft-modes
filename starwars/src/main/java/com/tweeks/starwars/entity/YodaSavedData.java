package com.tweeks.starwars.entity;

import com.mojang.serialization.Codec;
import com.tweeks.starwars.StarWarsMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-server singleton record for Yoda. Anchored to the overworld's
 * data storage so reads/writes from any dimension consult the same data.
 * Mirrors {@link LukeSavedData} exactly; the shared codec shape lives in
 * {@link NamedCharacterSavedData}.
 */
public final class YodaSavedData extends NamedCharacterSavedData {

    private static final String FILE_ID = "starwars_yoda";

    public static final Codec<YodaSavedData> CODEC = buildCodec(YodaSavedData::new);

    public static final SavedDataType<YodaSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, FILE_ID),
        YodaSavedData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_CUSTOM_BOSS_EVENTS
    );

    public YodaSavedData() {}

    public static YodaSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
