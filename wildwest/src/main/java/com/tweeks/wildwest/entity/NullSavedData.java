package com.tweeks.wildwest.entity;

import com.mojang.serialization.Codec;
import com.tweeks.wildwest.WildWestMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-server singleton record for Null. Anchored to the overworld's data
 * storage so reads/writes from any dimension consult the same data.
 *
 * <p>Independent from Herobrine and Agent singletons — all three can be
 * alive simultaneously.
 */
public final class NullSavedData extends BossSingletonSavedData {

    private static final String FILE_ID = "wildwest_null";

    public static final Codec<NullSavedData> CODEC = buildCodec(NullSavedData::new);

    public static final SavedDataType<NullSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, FILE_ID),
        NullSavedData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_CUSTOM_BOSS_EVENTS
    );

    public NullSavedData() {}

    public static NullSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
