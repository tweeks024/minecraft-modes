package com.tweeks.wildwest.entity;

import com.mojang.serialization.Codec;
import com.tweeks.wildwest.WildWestMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-server singleton record for the Grim Reaper. Anchored to the
 * overworld's data storage so reads/writes from any dimension consult the
 * same record. Independent from other apex bosses' SavedData — all five
 * can be alive simultaneously.
 */
public final class GrimReaperSavedData extends BossSingletonSavedData {

    private static final String FILE_ID = "wildwest_grim_reaper";

    public static final Codec<GrimReaperSavedData> CODEC = buildCodec(GrimReaperSavedData::new);

    public static final SavedDataType<GrimReaperSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, FILE_ID),
        GrimReaperSavedData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_CUSTOM_BOSS_EVENTS
    );

    public GrimReaperSavedData() {}

    public static GrimReaperSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
