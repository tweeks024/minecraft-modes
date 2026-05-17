package com.tweeks.wildwest.entity;

import com.mojang.serialization.Codec;
import com.tweeks.wildwest.WildWestMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-server singleton record for The Agent (Entity 303). Anchored to the
 * overworld's data storage so reads/writes from any dimension consult the
 * same data.
 *
 * <p>Codec field names + SavedDataType identifier preserved from the pre-refactor
 * implementation so existing on-disk saves load without migration.
 */
public final class AgentSavedData extends BossSingletonSavedData {

    private static final String FILE_ID = "wildwest_the_agent";

    public static final Codec<AgentSavedData> CODEC = buildCodec(AgentSavedData::new);

    public static final SavedDataType<AgentSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, FILE_ID),
        AgentSavedData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_CUSTOM_BOSS_EVENTS
    );

    public AgentSavedData() {}

    public static AgentSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
