package com.tweeks.starwars.world.planet;

import java.util.List;
import java.util.Optional;

import com.mojang.datafixers.util.Pair;
import com.tweeks.starwars.ModEntities;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TimelineTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.timeline.Timeline;

/**
 * Datagen bootstraps for the three planet dimensions: dimension types (26.1
 * environment-attribute skies), noise settings (vanilla overworld router with
 * planet surface rules), biomes, and level stems. Wired into the
 * DatapackBuiltinEntriesProvider in DataGenerators.
 */
public final class PlanetDimensions {
    public static final ResourceKey<NoiseGeneratorSettings> TATOOINE_NOISE =
        ResourceKey.create(Registries.NOISE_SETTINGS, Identifier.fromNamespaceAndPath("starwars", "tatooine"));
    public static final ResourceKey<NoiseGeneratorSettings> ANDOR_NOISE =
        ResourceKey.create(Registries.NOISE_SETTINGS, Identifier.fromNamespaceAndPath("starwars", "andor"));
    public static final ResourceKey<NoiseGeneratorSettings> DAGOBAH_NOISE =
        ResourceKey.create(Registries.NOISE_SETTINGS, Identifier.fromNamespaceAndPath("starwars", "dagobah"));
    public static final ResourceKey<NoiseGeneratorSettings> HOTH_NOISE =
        ResourceKey.create(Registries.NOISE_SETTINGS, Identifier.fromNamespaceAndPath("starwars", "hoth"));

    private PlanetDimensions() {
    }

    // ------------------------------------------------------------------
    // Dimension types

    public static void bootstrapDimensionTypes(BootstrapContext<DimensionType> ctx) {
        HolderGetter<Timeline> timelines = ctx.lookup(Registries.TIMELINE);
        HolderGetter<WorldClock> clocks = ctx.lookup(Registries.WORLD_CLOCK);
        DimensionType.MonsterSettings overworldMonsters = new DimensionType.MonsterSettings(UniformInt.of(0, 7), 0);

        // Tatooine: bright, dry, huge sky. Sky/fog tints live on the biomes.
        ctx.register(Planet.TATOOINE.dimensionTypeKey(), new DimensionType(
            false, true, false, false, 1.0, -64, 384, 384,
            BlockTags.INFINIBURN_OVERWORLD, 0.0F, overworldMonsters,
            DimensionType.Skybox.OVERWORLD, CardinalLighting.Type.DEFAULT,
            EnvironmentAttributeMap.builder()
                .set(EnvironmentAttributes.CLOUD_HEIGHT, 250.0F)
                .set(EnvironmentAttributes.BED_RULE, BedRule.CAN_SLEEP_WHEN_DARK)
                .set(EnvironmentAttributes.NETHER_PORTAL_SPAWNS_PIGLINS, false)
                .set(EnvironmentAttributes.CAN_START_RAID, false)
                .build(),
            timelines.getOrThrow(TimelineTags.IN_OVERWORLD),
            Optional.of(clocks.getOrThrow(WorldClocks.OVERWORLD))));

        // Andor: cool highland world, ordinary day cycle.
        ctx.register(Planet.ANDOR.dimensionTypeKey(), new DimensionType(
            false, true, false, false, 1.0, -64, 384, 384,
            BlockTags.INFINIBURN_OVERWORLD, 0.0F, overworldMonsters,
            DimensionType.Skybox.OVERWORLD, CardinalLighting.Type.DEFAULT,
            EnvironmentAttributeMap.builder()
                .set(EnvironmentAttributes.CLOUD_HEIGHT, 180.0F)
                .set(EnvironmentAttributes.BED_RULE, BedRule.CAN_SLEEP_WHEN_DARK)
                .set(EnvironmentAttributes.NETHER_PORTAL_SPAWNS_PIGLINS, false)
                .set(EnvironmentAttributes.CAN_START_RAID, false)
                .build(),
            timelines.getOrThrow(TimelineTags.IN_OVERWORLD),
            Optional.of(clocks.getOrThrow(WorldClocks.OVERWORLD))));

        // Dagobah: fog so thick the sun is a rumour. Short fog distances do
        // the heavy lifting; the biome supplies the murk colors.
        ctx.register(Planet.DAGOBAH.dimensionTypeKey(), new DimensionType(
            false, true, false, false, 1.0, -64, 384, 384,
            BlockTags.INFINIBURN_OVERWORLD, 0.0F, overworldMonsters,
            DimensionType.Skybox.OVERWORLD, CardinalLighting.Type.DEFAULT,
            EnvironmentAttributeMap.builder()
                .set(EnvironmentAttributes.FOG_COLOR, 0xFF4A5741)
                .set(EnvironmentAttributes.SKY_COLOR, 0xFF5E6B4F)
                .set(EnvironmentAttributes.FOG_START_DISTANCE, 12.0F)
                .set(EnvironmentAttributes.FOG_END_DISTANCE, 90.0F)
                .set(EnvironmentAttributes.CLOUD_HEIGHT, 110.0F)
                .set(EnvironmentAttributes.BED_RULE, BedRule.CAN_SLEEP_WHEN_DARK)
                .set(EnvironmentAttributes.NETHER_PORTAL_SPAWNS_PIGLINS, false)
                .set(EnvironmentAttributes.CAN_START_RAID, false)
                .build(),
            timelines.getOrThrow(TimelineTags.IN_OVERWORLD),
            Optional.of(clocks.getOrThrow(WorldClocks.OVERWORLD))));

        // Hoth: blinding-bright ice world under a pale crisp sky.
        ctx.register(Planet.HOTH.dimensionTypeKey(), new DimensionType(
            false, true, false, false, 1.0, -64, 384, 384,
            BlockTags.INFINIBURN_OVERWORLD, 0.0F, overworldMonsters,
            DimensionType.Skybox.OVERWORLD, CardinalLighting.Type.DEFAULT,
            EnvironmentAttributeMap.builder()
                .set(EnvironmentAttributes.SKY_COLOR, 0xFFC2D8EA)
                .set(EnvironmentAttributes.FOG_COLOR, 0xFFD8E8F2)
                .set(EnvironmentAttributes.CLOUD_HEIGHT, 210.0F)
                .set(EnvironmentAttributes.BED_RULE, BedRule.CAN_SLEEP_WHEN_DARK)
                .set(EnvironmentAttributes.NETHER_PORTAL_SPAWNS_PIGLINS, false)
                .set(EnvironmentAttributes.CAN_START_RAID, false)
                .set(EnvironmentAttributes.SNOW_GOLEM_MELTS, false)
                .build(),
            timelines.getOrThrow(TimelineTags.IN_OVERWORLD),
            Optional.of(clocks.getOrThrow(WorldClocks.OVERWORLD))));

        // Coruscant: frozen golden-hour sky over the endless city, brighter
        // starfield, violet haze. No world clock — time stands still. Monster
        // spawns ignore light so battle droids patrol the lamplit streets.
        ctx.register(Planet.CORUSCANT.dimensionTypeKey(), new DimensionType(
            true, true, false, false, 1.0, 0, 256, 256,
            BlockTags.INFINIBURN_OVERWORLD, 0.05F,
            new DimensionType.MonsterSettings(net.minecraft.util.valueproviders.ConstantInt.of(15), 15),
            DimensionType.Skybox.OVERWORLD, CardinalLighting.Type.DEFAULT,
            EnvironmentAttributeMap.builder()
                .set(EnvironmentAttributes.SKY_COLOR, 0xFF2B1B4D)
                .set(EnvironmentAttributes.FOG_COLOR, 0xFF53387A)
                .set(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, 0xFFE8A33D)
                .set(EnvironmentAttributes.STAR_BRIGHTNESS, 0.6F)
                .set(EnvironmentAttributes.CLOUD_HEIGHT, 230.0F)
                .set(EnvironmentAttributes.BED_RULE, BedRule.CAN_SLEEP_WHEN_DARK)
                .set(EnvironmentAttributes.NETHER_PORTAL_SPAWNS_PIGLINS, false)
                .set(EnvironmentAttributes.CAN_START_RAID, false)
                .build(),
            timelines.getOrThrow(TimelineTags.IN_OVERWORLD),
            Optional.empty()));
    }

    // ------------------------------------------------------------------
    // Noise settings

    public static void bootstrapNoiseSettings(BootstrapContext<NoiseGeneratorSettings> ctx) {
        NoiseGeneratorSettings base = NoiseGeneratorSettings.overworld(ctx, false, false);
        // Tatooine: overworld terrain shape, but bone dry — ocean basins
        // become dune seas. No aquifers, no ore veins (regular ores still
        // come from biome features).
        ctx.register(TATOOINE_NOISE, new NoiseGeneratorSettings(
            base.noiseSettings(), base.defaultBlock(), Blocks.AIR.defaultBlockState(),
            base.noiseRouter(), tatooineSurface(), base.spawnTarget(), 63,
            false, false, false, false));
        // Andor: watery highlands — lochs and rivers stay.
        ctx.register(ANDOR_NOISE, new NoiseGeneratorSettings(
            base.noiseSettings(), base.defaultBlock(), base.defaultFluid(),
            base.noiseRouter(), andorSurface(), base.spawnTarget(), 63,
            false, true, false, false));
        // Dagobah: sea level up at 64 turns every lowland into marsh.
        ctx.register(DAGOBAH_NOISE, new NoiseGeneratorSettings(
            base.noiseSettings(), base.defaultBlock(), base.defaultFluid(),
            base.noiseRouter(), dagobahSurface(), base.spawnTarget(), 64,
            false, true, false, false));
        // Hoth: frozen ocean world — water stays (the cold biome ices it over).
        ctx.register(HOTH_NOISE, new NoiseGeneratorSettings(
            base.noiseSettings(), base.defaultBlock(), base.defaultFluid(),
            base.noiseRouter(), hothSurface(), base.spawnTarget(), 63,
            false, true, false, false));
    }

    private static SurfaceRules.RuleSource state(net.minecraft.world.level.block.Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }

    private static SurfaceRules.RuleSource bedrockFloor() {
        return SurfaceRules.ifTrue(
            SurfaceRules.verticalGradient("starwars:bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)),
            state(Blocks.BEDROCK));
    }

    private static SurfaceRules.RuleSource tatooineSurface() {
        SurfaceRules.RuleSource jundland = SurfaceRules.ifTrue(
            SurfaceRules.isBiome(PlanetBiomes.JUNDLAND_WASTES),
            SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, state(Blocks.RED_SAND)),
                SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SurfaceRules.bandlands()),
                SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR, state(Blocks.ORANGE_TERRACOTTA))));
        SurfaceRules.RuleSource duneSea = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, state(Blocks.SAND)),
            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, state(Blocks.SAND)),
            SurfaceRules.ifTrue(SurfaceRules.VERY_DEEP_UNDER_FLOOR, state(Blocks.SANDSTONE)),
            SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR, state(Blocks.SANDSTONE)));
        return SurfaceRules.sequence(bedrockFloor(), jundland, duneSea);
    }

    private static SurfaceRules.RuleSource andorSurface() {
        SurfaceRules.RuleSource grassOrDirt = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0), state(Blocks.GRASS_BLOCK)),
            state(Blocks.DIRT));
        SurfaceRules.RuleSource floor = SurfaceRules.sequence(
            // Bare stone crowns above ~y105, coarse scree patches, grass below.
            SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(105), 0), state(Blocks.STONE)),
            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.4, Double.MAX_VALUE), state(Blocks.COARSE_DIRT)),
            grassOrDirt);
        return SurfaceRules.sequence(
            bedrockFloor(),
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, floor),
            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, state(Blocks.DIRT)));
    }

    private static SurfaceRules.RuleSource dagobahSurface() {
        SurfaceRules.RuleSource mudOrGrass = SurfaceRules.sequence(
            SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.2, Double.MAX_VALUE), state(Blocks.MUD)),
            SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0), state(Blocks.GRASS_BLOCK)),
            state(Blocks.MUD));
        return SurfaceRules.sequence(
            bedrockFloor(),
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, mudOrGrass),
            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, SurfaceRules.sequence(
                SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 0.2, Double.MAX_VALUE), state(Blocks.MUD)),
                state(Blocks.DIRT))));
    }

    private static SurfaceRules.RuleSource hothSurface() {
        return SurfaceRules.sequence(
            bedrockFloor(),
            SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.sequence(
                // Glacier crags above the snowline, sparse powder-snow traps
                // on the flats, snowfields everywhere else.
                SurfaceRules.ifTrue(SurfaceRules.yBlockCheck(VerticalAnchor.absolute(96), 0), state(Blocks.PACKED_ICE)),
                SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.POWDER_SNOW, 0.45, 0.58), state(Blocks.POWDER_SNOW)),
                state(Blocks.SNOW_BLOCK))),
            SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, state(Blocks.SNOW_BLOCK)),
            SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR, state(Blocks.PACKED_ICE)));
    }

    // ------------------------------------------------------------------
    // Biomes

    public static void bootstrapBiomes(BootstrapContext<Biome> ctx) {
        HolderGetter<PlacedFeature> features = ctx.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = ctx.lookup(Registries.CONFIGURED_CARVER);
        ctx.register(PlanetBiomes.DUNE_SEA, desertBiome(features, carvers, 0xFFBFD3E6, 0xFFE3D3AE));
        ctx.register(PlanetBiomes.JUNDLAND_WASTES, desertBiome(features, carvers, 0xFFC9CBB8, 0xFFD8C29A));
        ctx.register(PlanetBiomes.ALDHANI_HIGHLANDS, highlandsBiome(features, carvers));
        ctx.register(PlanetBiomes.CORUSCANT_CITY, cityBiome(features, carvers));
        ctx.register(PlanetBiomes.DAGOBAH_SWAMP, dagobahBiome(features, carvers));
        ctx.register(PlanetBiomes.HOTH_PLAINS, hothBiome(features, carvers));
    }

    private static Biome dagobahBiome(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        mobs.addSpawn(MobCategory.CREATURE, 10, new MobSpawnSettings.SpawnerData(EntityType.FROG, 2, 5));
        mobs.addSpawn(MobCategory.MONSTER, 6, new MobSpawnSettings.SpawnerData(EntityType.SLIME, 1, 3));
        mobs.addSpawn(MobCategory.MONSTER, 8, new MobSpawnSettings.SpawnerData(ModEntities.DRAGONSNAKE.get(), 1, 2));
        mobs.addSpawn(MobCategory.AMBIENT, 8, new MobSpawnSettings.SpawnerData(ModEntities.BOGWING.get(), 2, 4));

        BiomeGenerationSettings.Builder gen = new BiomeGenerationSettings.Builder(features, carvers);
        BiomeDefaultFeatures.addDefaultCarversAndLakes(gen);
        BiomeDefaultFeatures.addDefaultCrystalFormations(gen);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(gen);
        BiomeDefaultFeatures.addDefaultSprings(gen);
        BiomeDefaultFeatures.addDefaultOres(gen);
        BiomeDefaultFeatures.addDefaultSoftDisks(gen);
        BiomeDefaultFeatures.addMangroveSwampVegetation(gen);
        BiomeDefaultFeatures.addMangroveSwampExtraVegetation(gen);

        return new Biome.BiomeBuilder()
            .hasPrecipitation(true)
            .temperature(0.8F)
            .downfall(1.0F)
            .setAttribute(EnvironmentAttributes.SKY_COLOR, 0xFF5E6B4F)
            .setAttribute(EnvironmentAttributes.FOG_COLOR, 0xFF4A5741)
            .setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, 0xFF232D17)
            .specialEffects(new BiomeSpecialEffects.Builder()
                .waterColor(0x3A5A45)
                .foliageColorOverride(0x495225)
                .grassColorModifier(BiomeSpecialEffects.GrassColorModifier.SWAMP)
                .build())
            .mobSpawnSettings(mobs.build())
            .generationSettings(gen.build())
            .build();
    }

    private static Biome hothBiome(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        mobs.addSpawn(MobCategory.CREATURE, 10, new MobSpawnSettings.SpawnerData(ModEntities.TAUNTAUN.get(), 2, 4));
        mobs.addSpawn(MobCategory.CREATURE, 5, new MobSpawnSettings.SpawnerData(ModEntities.REBEL_TROOPER.get(), 2, 3));
        mobs.addSpawn(MobCategory.MONSTER, 10, new MobSpawnSettings.SpawnerData(ModEntities.SNOWTROOPER.get(), 2, 4));
        mobs.addSpawn(MobCategory.MONSTER, 5, new MobSpawnSettings.SpawnerData(ModEntities.WAMPA.get(), 1, 1));
        mobs.addSpawn(MobCategory.MONSTER, 4, new MobSpawnSettings.SpawnerData(ModEntities.PROBE_DROID.get(), 1, 1));
        // The occasional AT-AT on the march — rare, always solo.
        mobs.addSpawn(MobCategory.MONSTER, 1, new MobSpawnSettings.SpawnerData(ModEntities.AT_AT.get(), 1, 1));

        BiomeGenerationSettings.Builder gen = new BiomeGenerationSettings.Builder(features, carvers);
        BiomeDefaultFeatures.addDefaultCarversAndLakes(gen);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(gen);
        BiomeDefaultFeatures.addDefaultOres(gen);

        return new Biome.BiomeBuilder()
            .hasPrecipitation(true)
            .temperature(-0.8F)
            .downfall(0.5F)
            .setAttribute(EnvironmentAttributes.SKY_COLOR, 0xFFC2D8EA)
            .setAttribute(EnvironmentAttributes.FOG_COLOR, 0xFFD8E8F2)
            .specialEffects(new BiomeSpecialEffects.Builder().waterColor(0x3938C9).build())
            .mobSpawnSettings(mobs.build())
            .generationSettings(gen.build())
            .build();
    }

    private static Biome desertBiome(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers,
                                     int skyColor, int fogColor) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        mobs.addSpawn(MobCategory.MONSTER, 12, new MobSpawnSettings.SpawnerData(ModEntities.STORMTROOPER.get(), 2, 4));
        mobs.addSpawn(MobCategory.MONSTER, 6, new MobSpawnSettings.SpawnerData(EntityType.HUSK, 2, 3));
        mobs.addSpawn(MobCategory.MONSTER, 8, new MobSpawnSettings.SpawnerData(ModEntities.TUSKEN_RAIDER.get(), 1, 3));
        mobs.addSpawn(MobCategory.CREATURE, 4, new MobSpawnSettings.SpawnerData(ModEntities.ASTROMECH.get(), 1, 2));
        mobs.addSpawn(MobCategory.CREATURE, 8, new MobSpawnSettings.SpawnerData(ModEntities.JAWA.get(), 2, 4));
        mobs.addSpawn(MobCategory.CREATURE, 6, new MobSpawnSettings.SpawnerData(ModEntities.BANTHA.get(), 2, 3));

        BiomeGenerationSettings.Builder gen = new BiomeGenerationSettings.Builder(features, carvers);
        BiomeDefaultFeatures.addFossilDecoration(gen);
        BiomeDefaultFeatures.addDefaultCarversAndLakes(gen);
        BiomeDefaultFeatures.addDefaultCrystalFormations(gen);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(gen);
        BiomeDefaultFeatures.addDefaultOres(gen);
        BiomeDefaultFeatures.addDefaultSoftDisks(gen);
        BiomeDefaultFeatures.addDesertVegetation(gen);
        BiomeDefaultFeatures.addDesertExtraVegetation(gen);
        BiomeDefaultFeatures.addDesertExtraDecoration(gen);

        return new Biome.BiomeBuilder()
            .hasPrecipitation(false)
            .temperature(2.0F)
            .downfall(0.0F)
            .setAttribute(EnvironmentAttributes.SKY_COLOR, skyColor)
            .setAttribute(EnvironmentAttributes.FOG_COLOR, fogColor)
            .setAttribute(EnvironmentAttributes.SNOW_GOLEM_MELTS, true)
            .specialEffects(new BiomeSpecialEffects.Builder().waterColor(4159204).build())
            .mobSpawnSettings(mobs.build())
            .generationSettings(gen.build())
            .build();
    }

    private static Biome highlandsBiome(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        mobs.addSpawn(MobCategory.CREATURE, 12, new MobSpawnSettings.SpawnerData(EntityType.SHEEP, 2, 4));
        mobs.addSpawn(MobCategory.CREATURE, 6, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 2, 3));
        mobs.addSpawn(MobCategory.CREATURE, 4, new MobSpawnSettings.SpawnerData(EntityType.FOX, 1, 2));
        mobs.addSpawn(MobCategory.MONSTER, 10, new MobSpawnSettings.SpawnerData(ModEntities.STORMTROOPER.get(), 2, 4));
        mobs.addSpawn(MobCategory.CREATURE, 8, new MobSpawnSettings.SpawnerData(ModEntities.REBEL_TROOPER.get(), 2, 4));

        BiomeGenerationSettings.Builder gen = new BiomeGenerationSettings.Builder(features, carvers);
        BiomeDefaultFeatures.addDefaultCarversAndLakes(gen);
        BiomeDefaultFeatures.addDefaultCrystalFormations(gen);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(gen);
        BiomeDefaultFeatures.addDefaultSprings(gen);
        BiomeDefaultFeatures.addMossyStoneBlock(gen);
        BiomeDefaultFeatures.addDefaultOres(gen);
        BiomeDefaultFeatures.addDefaultSoftDisks(gen);
        gen.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, VegetationPlacements.TREES_TAIGA);
        BiomeDefaultFeatures.addDefaultFlowers(gen);
        BiomeDefaultFeatures.addDefaultGrass(gen);
        BiomeDefaultFeatures.addDefaultExtraVegetation(gen, false);

        return new Biome.BiomeBuilder()
            .hasPrecipitation(true)
            .temperature(0.4F)
            .downfall(0.8F)
            .setAttribute(EnvironmentAttributes.SKY_COLOR, 0xFF7FA3C8)
            .specialEffects(new BiomeSpecialEffects.Builder().waterColor(4159204).build())
            .mobSpawnSettings(mobs.build())
            .generationSettings(gen.build())
            .build();
    }

    private static Biome cityBiome(HolderGetter<PlacedFeature> features, HolderGetter<ConfiguredWorldCarver<?>> carvers) {
        MobSpawnSettings.Builder mobs = new MobSpawnSettings.Builder();
        mobs.addSpawn(MobCategory.MONSTER, 14, new MobSpawnSettings.SpawnerData(ModEntities.BATTLE_DROID.get(), 2, 4));
        mobs.addSpawn(MobCategory.MONSTER, 4, new MobSpawnSettings.SpawnerData(ModEntities.PROBE_DROID.get(), 1, 1));
        mobs.addSpawn(MobCategory.CREATURE, 3, new MobSpawnSettings.SpawnerData(ModEntities.JEDI_KNIGHT.get(), 1, 1));

        return new Biome.BiomeBuilder()
            .hasPrecipitation(false)
            .temperature(0.6F)
            .downfall(0.0F)
            .setAttribute(EnvironmentAttributes.SKY_COLOR, 0xFF2B1B4D)
            .setAttribute(EnvironmentAttributes.FOG_COLOR, 0xFF53387A)
            .specialEffects(new BiomeSpecialEffects.Builder().waterColor(4159204).build())
            .mobSpawnSettings(mobs.build())
            .generationSettings(new BiomeGenerationSettings.Builder(features, carvers).build())
            .build();
    }

    // ------------------------------------------------------------------
    // Level stems

    public static void bootstrapStems(BootstrapContext<LevelStem> ctx) {
        HolderGetter<DimensionType> types = ctx.lookup(Registries.DIMENSION_TYPE);
        HolderGetter<Biome> biomes = ctx.lookup(Registries.BIOME);
        HolderGetter<NoiseGeneratorSettings> noise = ctx.lookup(Registries.NOISE_SETTINGS);

        ctx.register(Planet.TATOOINE.stemKey(), new LevelStem(
            types.getOrThrow(Planet.TATOOINE.dimensionTypeKey()),
            new NoiseBasedChunkGenerator(
                MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(List.of(
                    Pair.of(Climate.parameters(2.0F, -0.7F, -0.2F, 0.4F, 0.0F, 0.0F, 0.0F),
                        biomes.getOrThrow(PlanetBiomes.DUNE_SEA)),
                    Pair.of(Climate.parameters(2.0F, -0.7F, 0.6F, -0.4F, 0.0F, 0.0F, 0.0F),
                        biomes.getOrThrow(PlanetBiomes.JUNDLAND_WASTES))))),
                noise.getOrThrow(TATOOINE_NOISE))));

        ctx.register(Planet.ANDOR.stemKey(), new LevelStem(
            types.getOrThrow(Planet.ANDOR.dimensionTypeKey()),
            new NoiseBasedChunkGenerator(
                new FixedBiomeSource(biomes.getOrThrow(PlanetBiomes.ALDHANI_HIGHLANDS)),
                noise.getOrThrow(ANDOR_NOISE))));

        ctx.register(Planet.CORUSCANT.stemKey(), new LevelStem(
            types.getOrThrow(Planet.CORUSCANT.dimensionTypeKey()),
            new CoruscantChunkGenerator(biomes.getOrThrow(PlanetBiomes.CORUSCANT_CITY))));

        ctx.register(Planet.DAGOBAH.stemKey(), new LevelStem(
            types.getOrThrow(Planet.DAGOBAH.dimensionTypeKey()),
            new NoiseBasedChunkGenerator(
                new FixedBiomeSource(biomes.getOrThrow(PlanetBiomes.DAGOBAH_SWAMP)),
                noise.getOrThrow(DAGOBAH_NOISE))));

        ctx.register(Planet.HOTH.stemKey(), new LevelStem(
            types.getOrThrow(Planet.HOTH.dimensionTypeKey()),
            new NoiseBasedChunkGenerator(
                new FixedBiomeSource(biomes.getOrThrow(PlanetBiomes.HOTH_PLAINS)),
                noise.getOrThrow(HOTH_NOISE))));
    }
}
