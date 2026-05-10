package com.tweeks.wildwest.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tweeks.wildwest.WildWestMod;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Optional;
import java.util.UUID;

/**
 * Per-server singleton record for Herobrine. Anchored to the overworld's
 * data storage so reads/writes from any dimension consult the same data.
 *
 * <p>Wraps {@link HerobrineState} (pure POJO; unit-tested in
 * {@code HerobrineStateTest}). All Minecraft-API glue lives here.
 *
 * <p><b>API note:</b> This NeoForge build (26.1.2.30-beta / MC 26.1.2) uses
 * {@link SavedDataType} with a {@link Codec} for persistence rather than the
 * {@code SavedData.Factory} / {@code save(ValueOutput)} / {@code load(ValueInput)}
 * pattern described in the task spec. The codec approach is the canonical pattern
 * in this version (see {@code WanderingTraderData} and {@code WeatherData}).
 */
public final class HerobrineSavedData extends SavedData {

    private static final String FILE_ID = "wildwest_herobrine";

    /** DFU codec — serialises the three logical fields. */
    public static final Codec<HerobrineSavedData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.BOOL.fieldOf("Alive").forGetter(sd -> sd.state.isAlive()),
            UUIDUtil.STRING_CODEC.optionalFieldOf("CurrentId")
                .forGetter(sd -> Optional.ofNullable(sd.state.getCurrentId())),
            Codec.STRING.optionalFieldOf("Dimension")
                .forGetter(sd -> Optional.ofNullable(sd.state.getDimensionId()))
        ).apply(instance, HerobrineSavedData::fromCodec)
    );

    public static final SavedDataType<HerobrineSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, FILE_ID),
        HerobrineSavedData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_CUSTOM_BOSS_EVENTS
    );

    private final HerobrineState state = new HerobrineState();

    /** No-arg constructor — produces a cleared (not-alive) state. */
    public HerobrineSavedData() {}

    /** Codec reconstruction. */
    private static HerobrineSavedData fromCodec(boolean alive,
                                                Optional<UUID> currentId,
                                                Optional<String> dimensionId) {
        HerobrineSavedData sd = new HerobrineSavedData();
        if (alive && currentId.isPresent() && dimensionId.isPresent()) {
            sd.state.setAlive(currentId.get(), dimensionId.get());
        }
        return sd;
    }

    /**
     * Read/create the singleton record. Anchored to {@code server.overworld()}
     * so callers in other dimensions consult the same file.
     */
    public static HerobrineSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean isAlive() { return this.state.isAlive(); }
    public UUID getCurrentId() { return this.state.getCurrentId(); }

    public ResourceKey<Level> getDimension() {
        String id = this.state.getDimensionId();
        if (id == null) return null;
        return ResourceKey.create(Registries.DIMENSION, Identifier.parse(id));
    }

    public void setAlive(UUID id, ResourceKey<Level> dimension) {
        this.state.setAlive(id, dimension.identifier().toString());
        this.setDirty();
    }

    public void clear() {
        this.state.clear();
        this.setDirty();
    }
}
