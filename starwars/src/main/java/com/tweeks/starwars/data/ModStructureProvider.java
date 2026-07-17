package com.tweeks.starwars.data;

import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.world.EchoBaseStructure;
import com.tweeks.starwars.world.EscapePodStructure;
import com.tweeks.starwars.world.EwokVillageStructure;
import com.tweeks.starwars.world.FerrixTownStructure;
import com.tweeks.starwars.world.ImperialOutpostStructure;
import com.tweeks.starwars.world.JabbaPalaceStructure;
import com.tweeks.starwars.world.JediRuinStructure;
import com.tweeks.starwars.world.KraytSkeletonStructure;
import com.tweeks.starwars.world.MoistureFarmStructure;
import com.tweeks.starwars.world.MosEisleyStructure;
import com.tweeks.starwars.world.SandcrawlerStructure;
import com.tweeks.starwars.world.VaderCastleStructure;
import com.tweeks.starwars.world.WampaCaveStructure;
import com.tweeks.starwars.world.XwingWreckStructure;
import com.tweeks.starwars.world.YodaHutStructure;
import com.tweeks.starwars.world.planet.PlanetBiomes;
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

    public static final ResourceKey<Structure> JEDI_RUIN = ResourceKey.create(
        Registries.STRUCTURE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "jedi_ruin"));

    public static final ResourceKey<StructureSet> JEDI_RUIN_SET = ResourceKey.create(
        Registries.STRUCTURE_SET,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "jedi_ruins"));

    public static final ResourceKey<Structure> MOISTURE_FARM = ResourceKey.create(
        Registries.STRUCTURE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "moisture_farm"));

    public static final ResourceKey<StructureSet> MOISTURE_FARM_SET = ResourceKey.create(
        Registries.STRUCTURE_SET,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "moisture_farms"));

    public static final ResourceKey<Structure> SANDCRAWLER = ResourceKey.create(
        Registries.STRUCTURE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "sandcrawler"));

    public static final ResourceKey<StructureSet> SANDCRAWLER_SET = ResourceKey.create(
        Registries.STRUCTURE_SET,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "sandcrawlers"));

    public static final ResourceKey<Structure> FERRIX_TOWN = ResourceKey.create(
        Registries.STRUCTURE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "ferrix_town"));

    public static final ResourceKey<StructureSet> FERRIX_TOWN_SET = ResourceKey.create(
        Registries.STRUCTURE_SET,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "ferrix_towns"));

    public static final ResourceKey<Structure> KRAYT_SKELETON = ResourceKey.create(
        Registries.STRUCTURE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "krayt_skeleton"));

    public static final ResourceKey<StructureSet> KRAYT_SKELETON_SET = ResourceKey.create(
        Registries.STRUCTURE_SET,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "krayt_skeletons"));

    public static final ResourceKey<Structure> MOS_EISLEY = ResourceKey.create(
        Registries.STRUCTURE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "mos_eisley"));

    public static final ResourceKey<StructureSet> MOS_EISLEY_SET = ResourceKey.create(
        Registries.STRUCTURE_SET,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "mos_eisleys"));

    public static final ResourceKey<Structure> YODA_HUT = ResourceKey.create(
        Registries.STRUCTURE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "yoda_hut"));

    public static final ResourceKey<StructureSet> YODA_HUT_SET = ResourceKey.create(
        Registries.STRUCTURE_SET,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "yoda_huts"));

    public static final ResourceKey<Structure> XWING_WRECK = ResourceKey.create(
        Registries.STRUCTURE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "xwing_wreck"));

    public static final ResourceKey<StructureSet> XWING_WRECK_SET = ResourceKey.create(
        Registries.STRUCTURE_SET,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "xwing_wrecks"));

    public static final ResourceKey<Structure> ECHO_BASE = ResourceKey.create(
        Registries.STRUCTURE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "echo_base"));

    public static final ResourceKey<StructureSet> ECHO_BASE_SET = ResourceKey.create(
        Registries.STRUCTURE_SET,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "echo_bases"));

    public static final ResourceKey<Structure> WAMPA_CAVE = ResourceKey.create(
        Registries.STRUCTURE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "wampa_cave"));

    public static final ResourceKey<StructureSet> WAMPA_CAVE_SET = ResourceKey.create(
        Registries.STRUCTURE_SET,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "wampa_caves"));

    public static final ResourceKey<Structure> EWOK_VILLAGE = ResourceKey.create(
        Registries.STRUCTURE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "ewok_village"));

    public static final ResourceKey<StructureSet> EWOK_VILLAGE_SET = ResourceKey.create(
        Registries.STRUCTURE_SET,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "ewok_villages"));

    public static final ResourceKey<Structure> VADER_CASTLE = ResourceKey.create(
        Registries.STRUCTURE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "vader_castle"));

    public static final ResourceKey<StructureSet> VADER_CASTLE_SET = ResourceKey.create(
        Registries.STRUCTURE_SET,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "vader_castles"));

    public static final ResourceKey<Structure> JABBA_PALACE = ResourceKey.create(
        Registries.STRUCTURE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "jabba_palace"));

    public static final ResourceKey<StructureSet> JABBA_PALACE_SET = ResourceKey.create(
        Registries.STRUCTURE_SET,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "jabba_palaces"));

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
                biomes.getOrThrow(Biomes.BADLANDS),
                biomes.getOrThrow(PlanetBiomes.ALDHANI_HIGHLANDS),
                biomes.getOrThrow(PlanetBiomes.JUNDLAND_WASTES)),
            Map.of(),                                  // no spawn overrides
            GenerationStep.Decoration.SURFACE_STRUCTURES,
            TerrainAdjustment.BEARD_THIN)));

        ctx.register(JEDI_RUIN, new JediRuinStructure(new Structure.StructureSettings(
            HolderSet.direct(
                biomes.getOrThrow(Biomes.FOREST),
                biomes.getOrThrow(Biomes.JUNGLE),
                biomes.getOrThrow(PlanetBiomes.JUNDLAND_WASTES)),
            Map.of(),                                  // no spawn overrides
            GenerationStep.Decoration.SURFACE_STRUCTURES,
            TerrainAdjustment.BEARD_THIN)));

        ctx.register(MOISTURE_FARM, new MoistureFarmStructure(new Structure.StructureSettings(
            HolderSet.direct(
                biomes.getOrThrow(PlanetBiomes.DUNE_SEA),
                biomes.getOrThrow(PlanetBiomes.JUNDLAND_WASTES)),
            Map.of(),                                  // no spawn overrides
            GenerationStep.Decoration.SURFACE_STRUCTURES,
            TerrainAdjustment.BEARD_THIN)));

        ctx.register(SANDCRAWLER, new SandcrawlerStructure(new Structure.StructureSettings(
            HolderSet.direct(
                biomes.getOrThrow(PlanetBiomes.DUNE_SEA)),
            Map.of(),                                  // no spawn overrides
            GenerationStep.Decoration.SURFACE_STRUCTURES,
            TerrainAdjustment.BEARD_THIN)));

        ctx.register(FERRIX_TOWN, new FerrixTownStructure(new Structure.StructureSettings(
            HolderSet.direct(
                biomes.getOrThrow(PlanetBiomes.ALDHANI_HIGHLANDS)),
            Map.of(),                                  // no spawn overrides
            GenerationStep.Decoration.SURFACE_STRUCTURES,
            TerrainAdjustment.BEARD_THIN)));

        ctx.register(KRAYT_SKELETON, new KraytSkeletonStructure(new Structure.StructureSettings(
            HolderSet.direct(
                biomes.getOrThrow(PlanetBiomes.DUNE_SEA),
                biomes.getOrThrow(PlanetBiomes.JUNDLAND_WASTES)),
            Map.of(),                                  // no spawn overrides
            GenerationStep.Decoration.SURFACE_STRUCTURES,
            TerrainAdjustment.BEARD_THIN)));

        ctx.register(MOS_EISLEY, new MosEisleyStructure(new Structure.StructureSettings(
            HolderSet.direct(
                biomes.getOrThrow(PlanetBiomes.DUNE_SEA),
                biomes.getOrThrow(PlanetBiomes.JUNDLAND_WASTES)),
            Map.of(),                                  // no spawn overrides
            GenerationStep.Decoration.SURFACE_STRUCTURES,
            TerrainAdjustment.BEARD_THIN)));

        ctx.register(YODA_HUT, new YodaHutStructure(new Structure.StructureSettings(
            HolderSet.direct(
                biomes.getOrThrow(PlanetBiomes.DAGOBAH_SWAMP)),
            Map.of(),                                  // no spawn overrides
            GenerationStep.Decoration.SURFACE_STRUCTURES,
            TerrainAdjustment.BEARD_THIN)));

        ctx.register(XWING_WRECK, new XwingWreckStructure(new Structure.StructureSettings(
            HolderSet.direct(
                biomes.getOrThrow(PlanetBiomes.DAGOBAH_SWAMP)),
            Map.of(),                                  // no spawn overrides
            GenerationStep.Decoration.SURFACE_STRUCTURES,
            TerrainAdjustment.NONE)));                 // half-sunk wreck: no beard under the sunken nose

        ctx.register(ECHO_BASE, new EchoBaseStructure(new Structure.StructureSettings(
            HolderSet.direct(
                biomes.getOrThrow(PlanetBiomes.HOTH_PLAINS)),
            Map.of(),                                  // no spawn overrides
            GenerationStep.Decoration.SURFACE_STRUCTURES,
            TerrainAdjustment.BEARD_THIN)));

        ctx.register(WAMPA_CAVE, new WampaCaveStructure(new Structure.StructureSettings(
            HolderSet.direct(
                biomes.getOrThrow(PlanetBiomes.HOTH_PLAINS)),
            Map.of(),                                  // no spawn overrides
            GenerationStep.Decoration.SURFACE_STRUCTURES,
            TerrainAdjustment.BEARD_THIN)));

        ctx.register(EWOK_VILLAGE, new EwokVillageStructure(new Structure.StructureSettings(
            HolderSet.direct(
                biomes.getOrThrow(PlanetBiomes.ENDOR_FOREST)),
            Map.of(),                                  // no spawn overrides
            GenerationStep.Decoration.SURFACE_STRUCTURES,
            TerrainAdjustment.BEARD_THIN)));

        ctx.register(VADER_CASTLE, new VaderCastleStructure(new Structure.StructureSettings(
            HolderSet.direct(
                biomes.getOrThrow(PlanetBiomes.MUSTAFAR_WASTES)),
            Map.of(),                                  // no spawn overrides
            GenerationStep.Decoration.SURFACE_STRUCTURES,
            TerrainAdjustment.BEARD_THIN)));           // beard the tall keep onto the lava plains

        ctx.register(JABBA_PALACE, new JabbaPalaceStructure(new Structure.StructureSettings(
            HolderSet.direct(
                biomes.getOrThrow(PlanetBiomes.DUNE_SEA),
                biomes.getOrThrow(PlanetBiomes.JUNDLAND_WASTES)),
            Map.of(),                                  // no spawn overrides
            GenerationStep.Decoration.SURFACE_STRUCTURES,
            TerrainAdjustment.BEARD_THIN)));           // beard the domed fortress onto the dunes
    }

    public static void bootstrapSets(BootstrapContext<StructureSet> ctx) {
        var structures = ctx.lookup(Registries.STRUCTURE);
        ctx.register(ESCAPE_POD_SET, new StructureSet(
            structures.getOrThrow(ESCAPE_POD),
            new RandomSpreadStructurePlacement(24, 8, RandomSpreadType.LINEAR, 1977100501)));

        ctx.register(IMPERIAL_OUTPOST_SET, new StructureSet(
            structures.getOrThrow(IMPERIAL_OUTPOST),
            new RandomSpreadStructurePlacement(40, 12, RandomSpreadType.LINEAR, 1977100502)));

        ctx.register(JEDI_RUIN_SET, new StructureSet(
            structures.getOrThrow(JEDI_RUIN),
            new RandomSpreadStructurePlacement(40, 12, RandomSpreadType.LINEAR, 1977100503)));

        ctx.register(MOISTURE_FARM_SET, new StructureSet(
            structures.getOrThrow(MOISTURE_FARM),
            new RandomSpreadStructurePlacement(24, 8, RandomSpreadType.LINEAR, 1977100504)));

        ctx.register(SANDCRAWLER_SET, new StructureSet(
            structures.getOrThrow(SANDCRAWLER),
            new RandomSpreadStructurePlacement(40, 12, RandomSpreadType.LINEAR, 1977100505)));

        ctx.register(FERRIX_TOWN_SET, new StructureSet(
            structures.getOrThrow(FERRIX_TOWN),
            new RandomSpreadStructurePlacement(28, 10, RandomSpreadType.LINEAR, 1977100506)));

        ctx.register(KRAYT_SKELETON_SET, new StructureSet(
            structures.getOrThrow(KRAYT_SKELETON),
            new RandomSpreadStructurePlacement(32, 10, RandomSpreadType.LINEAR, 1977100507)));

        ctx.register(MOS_EISLEY_SET, new StructureSet(
            structures.getOrThrow(MOS_EISLEY),
            new RandomSpreadStructurePlacement(48, 16, RandomSpreadType.LINEAR, 1977100508)));

        ctx.register(YODA_HUT_SET, new StructureSet(
            structures.getOrThrow(YODA_HUT),
            new RandomSpreadStructurePlacement(24, 8, RandomSpreadType.LINEAR, 1977100509)));

        ctx.register(XWING_WRECK_SET, new StructureSet(
            structures.getOrThrow(XWING_WRECK),
            new RandomSpreadStructurePlacement(28, 9, RandomSpreadType.LINEAR, 1977100510)));

        ctx.register(ECHO_BASE_SET, new StructureSet(
            structures.getOrThrow(ECHO_BASE),
            new RandomSpreadStructurePlacement(40, 12, RandomSpreadType.LINEAR, 1977100511)));

        ctx.register(WAMPA_CAVE_SET, new StructureSet(
            structures.getOrThrow(WAMPA_CAVE),
            new RandomSpreadStructurePlacement(20, 7, RandomSpreadType.LINEAR, 1977100512)));

        ctx.register(EWOK_VILLAGE_SET, new StructureSet(
            structures.getOrThrow(EWOK_VILLAGE),
            new RandomSpreadStructurePlacement(28, 9, RandomSpreadType.LINEAR, 1977100513)));

        ctx.register(VADER_CASTLE_SET, new StructureSet(
            structures.getOrThrow(VADER_CASTLE),
            new RandomSpreadStructurePlacement(40, 12, RandomSpreadType.LINEAR, 1977100514)));

        // A rare landmark — spaced wide so a palace is a genuine find.
        ctx.register(JABBA_PALACE_SET, new StructureSet(
            structures.getOrThrow(JABBA_PALACE),
            new RandomSpreadStructurePlacement(48, 16, RandomSpreadType.LINEAR, 1977100515)));
    }
}
