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
 * A crashed X-wing half-sunk in the Dagobah swamp, generated as a scattered
 * feature.
 *
 * <p>Extends {@link ScatteredFeaturePiece} so it inherits vanilla's proven
 * placement machinery: a random horizontal orientation, local-to-world
 * coordinate mapping through the oriented bounding box, terrain-height snapping
 * via {@code updateAverageGroundHeight}, and persistence of the derived ground
 * height plus dimensions across save/reload — the same pattern as
 * {@link EscapePodPiece}.
 *
 * <p>The layout's dy runs negative (from {@link XwingWreckLayout#MIN_Y}), so
 * the nose and low wings land below the snapped surface: getWorldY maps
 * negative local y below the box floor, and placeBlock only range-checks
 * against the full-height chunk box. The layout is air-free by design — the
 * piece places solid blocks only and never clears the swamp water around the
 * wreck.
 */
public class XwingWreckPiece extends ScatteredFeaturePiece {

    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(
        Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "chests/xwing_wreck"));

    /**
     * @param minBlockX world X of the chunk's min corner (the wreck's local x=0)
     * @param minBlockZ world Z of the chunk's min corner (the wreck's local z=0)
     */
    public XwingWreckPiece(RandomSource random, int minBlockX, int minBlockZ) {
        super(ModStructures.XWING_WRECK_PIECE.get(), minBlockX, 64, minBlockZ,
            XwingWreckLayout.SIZE_X, XwingWreckLayout.SIZE_Y, XwingWreckLayout.SIZE_Z,
            getRandomHorizontalDirection(random));
    }

    public XwingWreckPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.XWING_WRECK_PIECE.get(), tag);
    }

    // addAdditionalSaveData is inherited from ScatteredFeaturePiece: it persists
    // Width/Height/Depth and the derived HPos so reloads reproduce the same box.

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        // Snap the box to the average ground height under it before placing any
        // blocks; bails out (leaving the wreck ungenerated in this chunk pass)
        // if no columns of the box fall inside the current chunk.
        if (!this.updateAverageGroundHeight(level, box, 0)) {
            return;
        }
        for (var p : XwingWreckLayout.placements()) {
            var state = switch (p.kind()) {
                case FUSELAGE -> Blocks.WHITE_CONCRETE.defaultBlockState();
                case WING -> Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
                case ORANGE -> Blocks.ORANGE_CONCRETE.defaultBlockState();
                case COCKPIT -> Blocks.GLASS.defaultBlockState();
                case CHEST -> null;                          // handled via createChest
            };
            if (state != null) {
                this.placeBlock(level, state, p.dx(), p.dy(), p.dz(), box);
            } else if (p.kind() == XwingWreckLayout.Kind.CHEST) {
                this.createChest(level, box, random, p.dx(), p.dy(), p.dz(), LOOT);
            }
        }
    }
}
