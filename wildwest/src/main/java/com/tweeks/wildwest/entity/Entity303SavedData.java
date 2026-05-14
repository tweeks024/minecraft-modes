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
 * Per-server singleton record for Entity 303. Anchored to the overworld's
 * data storage so reads/writes from any dimension consult the same data.
 *
 * <p>Wraps {@link Entity303State} (pure POJO; unit-tested). Mirrors
 * {@link HerobrineSavedData} — both bosses can be alive simultaneously, but
 * each is independently singleton.
 */
public final class Entity303SavedData extends SavedData {

    private static final String FILE_ID = "wildwest_entity_303";

    public static final Codec<Entity303SavedData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.BOOL.fieldOf("Alive").forGetter(sd -> sd.state.isAlive()),
            UUIDUtil.STRING_CODEC.optionalFieldOf("CurrentId")
                .forGetter(sd -> Optional.ofNullable(sd.state.getCurrentId())),
            Codec.STRING.optionalFieldOf("Dimension")
                .forGetter(sd -> Optional.ofNullable(sd.state.getDimensionId()))
        ).apply(instance, Entity303SavedData::fromCodec)
    );

    public static final SavedDataType<Entity303SavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, FILE_ID),
        Entity303SavedData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_CUSTOM_BOSS_EVENTS
    );

    private final Entity303State state = new Entity303State();

    public Entity303SavedData() {}

    private static Entity303SavedData fromCodec(boolean alive,
                                                Optional<UUID> currentId,
                                                Optional<String> dimensionId) {
        Entity303SavedData sd = new Entity303SavedData();
        if (alive && currentId.isPresent() && dimensionId.isPresent()) {
            sd.state.setAlive(currentId.get(), dimensionId.get());
        }
        return sd;
    }

    public static Entity303SavedData get(MinecraftServer server) {
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
