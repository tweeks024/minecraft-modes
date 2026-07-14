package com.tweeks.starwars.world.planet;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tweeks.starwars.world.planet.StationLayout.Kind;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.LadderBlock;
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
 * The battle station interior. All structure lives in the pure
 * {@link StationLayout}; this class only maps kinds to block states and
 * feeds chunks, exactly like {@link CoruscantChunkGenerator}. Seeded per
 * world via {@link #createState}.
 */
public class DeathStarChunkGenerator extends ChunkGenerator {
    public static final MapCodec<DeathStarChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(Biome.CODEC.fieldOf("biome").forGetter(g -> g.biome)).apply(i, i.stable(DeathStarChunkGenerator::new))
    );

    private final Holder<Biome> biome;
    private volatile long stationSeed;

    public DeathStarChunkGenerator(Holder<Biome> biome) {
        super(new FixedBiomeSource(biome));
        this.biome = biome;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structureSets, RandomState randomState, long levelSeed) {
        this.stationSeed = levelSeed;
        return super.createState(structureSets, randomState, levelSeed);
    }

    private BlockState stateFor(Kind kind) {
        return switch (kind) {
            case AIR, DOORWAY -> Blocks.AIR.defaultBlockState();
            case BULKHEAD -> Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            case HULL -> Blocks.GRAY_CONCRETE.defaultBlockState();
            case FLOOR -> Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
            case CEILING -> Blocks.GRAY_CONCRETE.defaultBlockState();
            case CORRIDOR_LIGHT, ROOM_LIGHT -> Blocks.SEA_LANTERN.defaultBlockState();
            case REDSTONE_LAMP -> Blocks.REDSTONE_LAMP.defaultBlockState().setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT, true);
            case BARS -> Blocks.IRON_BARS.defaultBlockState();
            case BUNK -> Blocks.CYAN_TERRACOTTA.defaultBlockState();
            case CONSOLE -> Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            case SUPPLY_IRON -> Blocks.IRON_BLOCK.defaultBlockState();
            case SUPPLY_GOLD -> Blocks.GOLD_BLOCK.defaultBlockState();
            case SUPPLY_DIAMOND -> Blocks.DIAMOND_BLOCK.defaultBlockState();
            case SUPPLY_REDSTONE -> Blocks.REDSTONE_BLOCK.defaultBlockState();
            case REACTOR_CASING -> Blocks.POLISHED_BLACKSTONE.defaultBlockState();
            case REACTOR_CORE -> Blocks.SEA_LANTERN.defaultBlockState();
            case LADDER -> Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
        };
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        long seed = this.stationSeed;
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        int minY = Math.max(chunk.getMinY(), StationLayout.BOTTOM_Y);
        int maxY = Math.min(chunk.getMaxY(), StationLayout.TOP_Y);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = chunkPos.getMinBlockX() + x;
                int wz = chunkPos.getMinBlockZ() + z;
                for (int y = minY; y <= maxY; y++) {
                    Kind kind = StationLayout.kindAt(seed, wx, y, wz);
                    if (kind == Kind.AIR || kind == Kind.DOORWAY) {
                        continue;
                    }
                    BlockState state = stateFor(kind);
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
        return StationLayout.safeFloorY(this.stationSeed, x, z);
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
        long seed = this.stationSeed;
        int minY = heightAccessor.getMinY();
        int top = StationLayout.TOP_Y;
        BlockState[] states = new BlockState[Math.max(0, top - minY + 1)];
        for (int y = minY; y <= top; y++) {
            Kind kind = StationLayout.kindAt(seed, x, y, z);
            states[y - minY] = (kind == Kind.AIR || kind == Kind.DOORWAY)
                ? Blocks.AIR.defaultBlockState()
                : stateFor(kind);
        }
        return new NoiseColumn(minY, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> result, RandomState randomState, BlockPos feetPos) {
        StationLayout.RoomType type = StationLayout.roomType(this.stationSeed,
            StationLayout.cellOf(feetPos.getX()), StationLayout.cellOf(feetPos.getZ()),
            Math.floorDiv(feetPos.getY() - StationLayout.BOTTOM_Y, StationLayout.DECK));
        result.add("Death Star cell: " + type);
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor heightAccessor) {
        return StationLayout.SPAWN_DECK_FLOOR + 1;
    }

    @Override
    public int getMinY() {
        return StationLayout.BOTTOM_Y;
    }

    @Override
    public int getGenDepth() {
        return StationLayout.TOP_Y + 8;
    }

    @Override
    public int getSeaLevel() {
        return StationLayout.SPAWN_DECK_FLOOR;
    }
}
