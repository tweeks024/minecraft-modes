package com.tweeks.starwars.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Shared base for per-boss singleton {@link SavedData} records. Each subclass
 * supplies its own on-disk identifier, codec, and {@link net.minecraft.world.level.saveddata.SavedDataType}
 * — the actual state shape and the codec field names are inherited.
 *
 * <p>NBT compatibility note: field names {@code "Alive"}, {@code "CurrentId"},
 * {@code "Dimension"} match the original per-boss SavedData classes byte-for-byte,
 * so existing on-disk {@code starwars_vader.dat} and other character dat files continue to load
 * through their respective subclasses without migration.
 */
public abstract class NamedCharacterSavedData extends SavedData {

    protected final SingletonState state = new SingletonState();

    protected NamedCharacterSavedData() {}

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

    /**
     * Builds the shared DFU codec for any subclass. Subclasses provide a no-arg
     * factory; codec applies the three fields onto a fresh instance.
     */
    protected static <T extends NamedCharacterSavedData> Codec<T> buildCodec(Supplier<T> factory) {
        return RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("Alive").forGetter(sd -> sd.state.isAlive()),
            UUIDUtil.STRING_CODEC.optionalFieldOf("CurrentId")
                .forGetter(sd -> Optional.ofNullable(sd.state.getCurrentId())),
            Codec.STRING.optionalFieldOf("Dimension")
                .forGetter(sd -> Optional.ofNullable(sd.state.getDimensionId()))
        ).apply(instance, (alive, currentId, dimensionId) -> {
            T sd = factory.get();
            if (alive && currentId.isPresent() && dimensionId.isPresent()) {
                sd.state.setAlive(currentId.get(), dimensionId.get());
            }
            return sd;
        }));
    }
}
