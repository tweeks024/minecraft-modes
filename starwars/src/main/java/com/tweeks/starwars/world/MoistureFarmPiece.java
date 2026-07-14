package com.tweeks.starwars.world;

import com.tweeks.starwars.StarWarsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * A single Tatooine moisture farm, generated as a scattered feature.
 *
 * <p>Extends {@link ScatteredFeaturePiece} so it inherits vanilla's proven
 * placement machinery: a random horizontal orientation, local-to-world
 * coordinate mapping through the oriented bounding box, terrain-height snapping
 * via {@code updateAverageGroundHeight}, and persistence of the derived ground
 * height plus dimensions across save/reload — the same pattern as
 * {@link EscapePodPiece}.
 */
public class MoistureFarmPiece extends ScatteredFeaturePiece {

    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(
        Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "chests/moisture_farm"));

    /**
     * @param minBlockX world X of the chunk's min corner (the farm's local x=0)
     * @param minBlockZ world Z of the chunk's min corner (the farm's local z=0)
     */
    public MoistureFarmPiece(RandomSource random, int minBlockX, int minBlockZ) {
        super(ModStructures.MOISTURE_FARM_PIECE.get(), minBlockX, 64, minBlockZ,
            MoistureFarmLayout.SIZE_X, MoistureFarmLayout.SIZE_Y, MoistureFarmLayout.SIZE_Z,
            getRandomHorizontalDirection(random));
    }

    public MoistureFarmPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.MOISTURE_FARM_PIECE.get(), tag);
    }

    // addAdditionalSaveData is inherited from ScatteredFeaturePiece: it persists
    // Width/Height/Depth and the derived HPos so reloads reproduce the same box.

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        // Snap the box to the average ground height under it before placing any
        // blocks; bails out (leaving the farm ungenerated in this chunk pass)
        // if no columns of the box fall inside the current chunk.
        if (!this.updateAverageGroundHeight(level, box, 0)) {
            return;
        }
        for (var p : MoistureFarmLayout.placements()) {
            var state = switch (p.kind()) {
                case DOME -> Blocks.SMOOTH_SANDSTONE.defaultBlockState();
                case FLOOR -> Blocks.SANDSTONE.defaultBlockState();
                case AIR -> Blocks.AIR.defaultBlockState();
                case CHEST, VAPORATOR -> null;               // handled below
            };
            if (state != null) {
                this.placeBlock(level, state, p.dx(), p.dy(), p.dz(), box);
            } else if (p.kind() == MoistureFarmLayout.Kind.CHEST) {
                this.createChest(level, box, random, p.dx(), p.dy(), p.dz(), LOOT);
            } else {
                // GX-8 vaporator: a slim 3-tall condenser mast — two wall-block
                // segments topped with a lightning rod (the sensor spike).
                this.placeBlock(level, Blocks.ANDESITE_WALL.defaultBlockState(),
                    p.dx(), p.dy(), p.dz(), box);
                this.placeBlock(level, Blocks.ANDESITE_WALL.defaultBlockState(),
                    p.dx(), p.dy() + 1, p.dz(), box);
                this.placeBlock(level, Blocks.LIGHTNING_ROD.defaultBlockState(),
                    p.dx(), p.dy() + 2, p.dz(), box);
            }
        }
    }
}
