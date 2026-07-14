package com.tweeks.starwars.world.gate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.world.planet.Planet;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-level registry of known hyperspace gates, so arrivals reuse a nearby
 * existing gate instead of stamping a new one every trip. Entries are added
 * when a gate is ignited or auto-built; stale entries (film broken, chunk
 * griefed) are pruned lazily when a lookup finds no portal block at the
 * recorded origin.
 */
public final class PortalRecords extends SavedData {

    /** One gate: interior bottom-left cell, plane axis, bound destination. */
    public record GateRecord(BlockPos origin, boolean axisX, Planet destination) {
        public static final Codec<GateRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("Origin").forGetter(GateRecord::origin),
            Codec.BOOL.fieldOf("AxisX").forGetter(GateRecord::axisX),
            Planet.CODEC.fieldOf("Destination").forGetter(GateRecord::destination)
        ).apply(i, GateRecord::new));
    }

    public static final Codec<PortalRecords> CODEC = RecordCodecBuilder.create(i -> i.group(
        GateRecord.CODEC.listOf().fieldOf("Gates").forGetter(r -> List.copyOf(r.gates))
    ).apply(i, PortalRecords::new));

    // No DFU baseline for mod data; the boss SavedData classes use the same
    // stand-in fixer type.
    public static final SavedDataType<PortalRecords> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "starwars_gates"),
        PortalRecords::new,
        CODEC,
        DataFixTypes.SAVED_DATA_CUSTOM_BOSS_EVENTS
    );

    private final List<GateRecord> gates = new ArrayList<>();

    public PortalRecords() {
    }

    private PortalRecords(List<GateRecord> loaded) {
        this.gates.addAll(loaded);
    }

    public static PortalRecords get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<GateRecord> all() {
        return List.copyOf(gates);
    }

    /** Adds a gate, replacing any previous record at the same origin. */
    public void put(GateRecord record) {
        gates.removeIf(g -> g.origin().equals(record.origin()));
        gates.add(record);
        setDirty();
    }

    public void removeAt(BlockPos origin) {
        if (gates.removeIf(g -> g.origin().equals(origin))) {
            setDirty();
        }
    }

    /**
     * Nearest record within {@code radius} blocks (horizontal distance) of
     * {@code target}. Pure — exercised directly by unit tests.
     */
    public static Optional<GateRecord> nearest(List<GateRecord> records, BlockPos target, int radius) {
        long radiusSq = (long) radius * radius;
        return records.stream()
            .filter(g -> horizontalDistSq(g.origin(), target) <= radiusSq)
            .min(Comparator.comparingLong(g -> horizontalDistSq(g.origin(), target)));
    }

    private static long horizontalDistSq(BlockPos a, BlockPos b) {
        long dx = a.getX() - b.getX();
        long dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }
}
