package com.tweeks.starwars.world.planet;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tweeks.starwars.world.planet.CityLayout.CellSpec;
import com.tweeks.starwars.world.planet.CityLayout.Kind;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;

/**
 * The endless city. All layout decisions live in the pure {@link CityLayout};
 * this class only maps layout kinds to block states and feeds chunks. The
 * world seed arrives via {@link #createState} (called once at level startup,
 * before any chunk generates) so the city varies per world.
 */
public class CoruscantChunkGenerator extends ChunkGenerator {
    public static final MapCodec<CoruscantChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(Biome.CODEC.fieldOf("biome").forGetter(g -> g.biome)).apply(i, i.stable(CoruscantChunkGenerator::new))
    );

    private static final int GEN_DEPTH = 256;

    private final Holder<Biome> biome;
    private volatile long citySeed;

    public CoruscantChunkGenerator(Holder<Biome> biome) {
        super(new FixedBiomeSource(biome));
        this.biome = biome;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structureSets, RandomState randomState, long levelSeed) {
        this.citySeed = levelSeed;
        return super.createState(structureSets, randomState, levelSeed);
    }

    private BlockState stateFor(Kind kind, CellSpec spec, int y) {
        return switch (kind) {
            case AIR -> Blocks.AIR.defaultBlockState();
            case BEDROCK -> Blocks.BEDROCK.defaultBlockState();
            // Concrete service levels near the streets, old deepslate bones below.
            case FOUNDATION -> y >= CityLayout.STREET_Y - 16
                ? Blocks.GRAY_CONCRETE.defaultBlockState()
                : Blocks.DEEPSLATE.defaultBlockState();
            case STREET -> Blocks.GRAY_CONCRETE.defaultBlockState();
            case STREET_LINE -> Blocks.WHITE_CONCRETE.defaultBlockState();
            case SIDEWALK -> Blocks.POLISHED_ANDESITE.defaultBlockState();
            case LAMP_POST -> Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            case LAMP_LIGHT -> Blocks.GLOWSTONE.defaultBlockState();
            case WALL -> switch (spec.palette()) {
                case 0 -> Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
                case 1 -> Blocks.WHITE_CONCRETE.defaultBlockState();
                default -> Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            };
            case WALL_ACCENT, ROOF -> switch (spec.palette()) {
                case 0 -> Blocks.GRAY_CONCRETE.defaultBlockState();
                case 1 -> Blocks.SMOOTH_QUARTZ.defaultBlockState();
                default -> Blocks.POLISHED_BASALT.defaultBlockState();
            };
            case ROOF_EDGE -> switch (spec.palette()) {
                case 0 -> Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
                case 1 -> Blocks.WHITE_CONCRETE.defaultBlockState();
                default -> Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            };
            case WINDOW -> Blocks.BLUE_STAINED_GLASS.defaultBlockState();
            case WINDOW_LIT -> Blocks.SEA_LANTERN.defaultBlockState();
            case SPIRE -> Blocks.END_ROD.defaultBlockState();
            case PAD_DECK -> Blocks.SMOOTH_STONE.defaultBlockState();
            case PAD_MARK -> Blocks.YELLOW_CONCRETE.defaultBlockState();
            case PYLON -> Blocks.GRAY_CONCRETE.defaultBlockState();
            case PLAZA_FLOOR -> Blocks.SMOOTH_QUARTZ.defaultBlockState();
            case PLAZA_ACCENT -> Blocks.POLISHED_ANDESITE.defaultBlockState();
            case MONUMENT -> Blocks.IRON_BLOCK.defaultBlockState();
        };
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        long seed = this.citySeed;
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        int minY = chunk.getMinY();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = chunkPos.getMinBlockX() + x;
                int wz = chunkPos.getMinBlockZ() + z;
                CellSpec spec = CityLayout.cellSpec(seed, CityLayout.cellOf(wx), CityLayout.cellOf(wz));
                int top = CityLayout.topY(seed, wx, wz);
                for (int y = Math.max(minY, CityLayout.BOTTOM_Y); y <= top; y++) {
                    Kind kind = CityLayout.kindAt(seed, wx, y, wz);
                    if (kind == Kind.AIR) {
                        continue;
                    }
                    BlockState state = stateFor(kind, spec, y);
                    chunk.setBlockState(pos.set(x, y, z), state);
                    oceanFloor.update(x, y, z, state);
                    worldSurface.update(x, y, z, state);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        ChunkPos center = region.getCenter();
        Holder<Biome> spawnBiome = region.getBiome(center.getWorldPosition().atY(region.getMaxY()));
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
        random.setDecorationSeed(region.getSeed(), center.getMinBlockX(), center.getMinBlockZ());
        NaturalSpawner.spawnMobsForChunkGeneration(region, spawnBiome, center, random);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor, RandomState randomState) {
        return CityLayout.topY(this.citySeed, x, z) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
        long seed = this.citySeed;
        CellSpec spec = CityLayout.cellSpec(seed, CityLayout.cellOf(x), CityLayout.cellOf(z));
        int minY = heightAccessor.getMinY();
        int top = CityLayout.topY(seed, x, z);
        BlockState[] states = new BlockState[Math.max(0, top - minY + 1)];
        for (int y = minY; y <= top; y++) {
            states[y - minY] = stateFor(CityLayout.kindAt(seed, x, y, z), spec, y);
        }
        return new NoiseColumn(minY, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos) {
        CellSpec spec = CityLayout.cellSpec(this.citySeed, CityLayout.cellOf(feetPos.getX()), CityLayout.cellOf(feetPos.getZ()));
        result.add("Coruscant cell: " + spec.type() + " h=" + spec.height() + " palette=" + spec.palette());
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor heightAccessor) {
        return CityLayout.STREET_Y + 2;
    }

    @Override
    public int getMinY() {
        return CityLayout.BOTTOM_Y;
    }

    @Override
    public int getGenDepth() {
        return GEN_DEPTH;
    }

    @Override
    public int getSeaLevel() {
        return CityLayout.STREET_Y;
    }
}
