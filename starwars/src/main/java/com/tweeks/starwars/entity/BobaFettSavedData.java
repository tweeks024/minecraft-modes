package com.tweeks.starwars.entity;

import com.mojang.serialization.Codec;
import com.tweeks.starwars.StarWarsMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-server singleton record for Boba Fett. Anchored to the overworld's
 * data storage so reads/writes from any dimension consult the same data.
 *
 * <p>Codec field names ({@code "Alive"}, {@code "CurrentId"}, {@code "Dimension"})
 * and {@code SavedDataType} identifier ({@code "starwars:starwars_boba_fett"})
 * are preserved from the pre-refactor implementation so existing on-disk
 * saves continue to load without migration.
 */
public final class BobaFettSavedData extends NamedCharacterSavedData {

    private static final String FILE_ID = "starwars_boba_fett";

    public static final Codec<BobaFettSavedData> CODEC = buildCodec(BobaFettSavedData::new);

    public static final SavedDataType<BobaFettSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, FILE_ID),
        BobaFettSavedData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_CUSTOM_BOSS_EVENTS
    );

    public BobaFettSavedData() {}

    public static BobaFettSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
