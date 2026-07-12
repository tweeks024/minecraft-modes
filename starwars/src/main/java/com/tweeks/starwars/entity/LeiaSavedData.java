package com.tweeks.starwars.entity;

import com.mojang.serialization.Codec;
import com.tweeks.starwars.StarWarsMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-server singleton record for Princess Leia. Anchored to the overworld's
 * data storage so reads/writes from any dimension consult the same data.
 * Same five-part shape as {@link LukeSavedData}.
 */
public final class LeiaSavedData extends NamedCharacterSavedData {

    private static final String FILE_ID = "starwars_leia";

    public static final Codec<LeiaSavedData> CODEC = buildCodec(LeiaSavedData::new);

    public static final SavedDataType<LeiaSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, FILE_ID),
        LeiaSavedData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_CUSTOM_BOSS_EVENTS
    );

    public LeiaSavedData() {}

    public static LeiaSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
