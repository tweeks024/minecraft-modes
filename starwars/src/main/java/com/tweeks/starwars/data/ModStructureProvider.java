package com.tweeks.starwars.data;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.world.EscapePodStructure;
import com.tweeks.starwars.world.ImperialOutpostStructure;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

import java.util.Map;

public final class ModStructureProvider {
    private ModStructureProvider() {}

    public static final ResourceKey<Structure> ESCAPE_POD = ResourceKey.create(
        Registries.STRUCTURE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "escape_pod"));

    public static final ResourceKey<StructureSet> ESCAPE_POD_SET = ResourceKey.create(
        Registries.STRUCTURE_SET,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "escape_pods"));

    public static final ResourceKey<Structure> IMPERIAL_OUTPOST = ResourceKey.create(
        Registries.STRUCTURE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "imperial_outpost"));

    public static final ResourceKey<StructureSet> IMPERIAL_OUTPOST_SET = ResourceKey.create(
        Registries.STRUCTURE_SET,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "imperial_outposts"));

    public static void bootstrapStructures(BootstrapContext<Structure> ctx) {
        var biomes = ctx.lookup(Registries.BIOME);
        ctx.register(ESCAPE_POD, new EscapePodStructure(new Structure.StructureSettings(
            HolderSet.direct(
                biomes.getOrThrow(Biomes.PLAINS),
                biomes.getOrThrow(Biomes.DESERT)),
            Map.of(),                                  // no spawn overrides
            GenerationStep.Decoration.SURFACE_STRUCTURES,
            TerrainAdjustment.BEARD_THIN)));

        ctx.register(IMPERIAL_OUTPOST, new ImperialOutpostStructure(new Structure.StructureSettings(
            HolderSet.direct(
                biomes.getOrThrow(Biomes.DESERT),
                biomes.getOrThrow(Biomes.BADLANDS)),
            Map.of(),                                  // no spawn overrides
            GenerationStep.Decoration.SURFACE_STRUCTURES,
            TerrainAdjustment.BEARD_THIN)));
    }

    public static void bootstrapSets(BootstrapContext<StructureSet> ctx) {
        var structures = ctx.lookup(Registries.STRUCTURE);
        ctx.register(ESCAPE_POD_SET, new StructureSet(
            structures.getOrThrow(ESCAPE_POD),
            new RandomSpreadStructurePlacement(24, 8, RandomSpreadType.LINEAR, 1977100501)));

        ctx.register(IMPERIAL_OUTPOST_SET, new StructureSet(
            structures.getOrThrow(IMPERIAL_OUTPOST),
            new RandomSpreadStructurePlacement(40, 12, RandomSpreadType.LINEAR, 1977100502)));
    }
}
