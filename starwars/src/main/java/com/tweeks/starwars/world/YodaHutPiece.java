package com.tweeks.starwars.world;

import com.tweeks.starwars.ModEntities;
import com.tweeks.starwars.StarWarsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
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
 * Yoda's hut on Dagobah, generated as a scattered feature.
 *
 * <p>Extends {@link ScatteredFeaturePiece} so it inherits vanilla's proven
 * placement machinery: a random horizontal orientation, local-to-world
 * coordinate mapping through the oriented bounding box, terrain-height snapping
 * via {@code updateAverageGroundHeight}, and persistence of the derived ground
 * height plus dimensions across save/reload — the same pattern as
 * {@link EscapePodPiece}.
 */
public class YodaHutPiece extends ScatteredFeaturePiece {

    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(
        Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "chests/yoda_hut"));

    /**
     * @param minBlockX world X of the chunk's min corner (the hut's local x=0)
     * @param minBlockZ world Z of the chunk's min corner (the hut's local z=0)
     */
    public YodaHutPiece(RandomSource random, int minBlockX, int minBlockZ) {
        super(ModStructures.YODA_HUT_PIECE.get(), minBlockX, 64, minBlockZ,
            YodaHutLayout.SIZE_X, YodaHutLayout.SIZE_Y, YodaHutLayout.SIZE_Z,
            getRandomHorizontalDirection(random));
    }

    public YodaHutPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.YODA_HUT_PIECE.get(), tag);
    }

    // addAdditionalSaveData is inherited from ScatteredFeaturePiece: it persists
    // Width/Height/Depth and the derived HPos so reloads reproduce the same box.

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        // Snap the box to the average ground height under it before placing any
        // blocks; bails out (leaving the hut ungenerated in this chunk pass)
        // if no columns of the box fall inside the current chunk.
        if (!this.updateAverageGroundHeight(level, box, 0)) {
            return;
        }
        for (var p : YodaHutLayout.placements()) {
            var state = switch (p.kind()) {
                case DOME_MUD -> Blocks.MUD.defaultBlockState();
                case DOME_ROOTS -> Blocks.MUDDY_MANGROVE_ROOTS.defaultBlockState();
                case FLOOR -> Blocks.PACKED_MUD.defaultBlockState();
                case PALLET -> Blocks.BROWN_WOOL.defaultBlockState();
                case POT -> Blocks.CAULDRON.defaultBlockState();
                case AIR -> Blocks.AIR.defaultBlockState();
                case CHEST -> null;                          // handled via createChest
                case YODA -> Blocks.AIR.defaultBlockState(); // carve air so the Master doesn't suffocate on slopes; entity spawn below
            };
            if (state != null) {
                this.placeBlock(level, state, p.dx(), p.dy(), p.dz(), box);
            } else if (p.kind() == YodaHutLayout.Kind.CHEST) {
                this.createChest(level, box, random, p.dx(), p.dy(), p.dz(), LOOT);
            }
        }

        // Yoda: spawn the Master in his hut at generation time. Coordinates
        // are mapped local->world through the oriented box (getWorldPos), so
        // the marker rotates/mirrors with the piece — same pattern as the
        // ImperialOutpostPiece garrison loop. Persistence matches the other
        // pieces; Yoda's singleton logic handles dedupe. Skipped if the marker
        // falls outside the current chunk's box.
        for (var p : YodaHutLayout.placements()) {
            if (p.kind() != YodaHutLayout.Kind.YODA) continue;
            EntityType<? extends Mob> type = ModEntities.YODA.get();
            BlockPos worldPos = this.getWorldPos(p.dx(), p.dy(), p.dz());
            if (!box.isInside(worldPos)) continue;
            Mob mob = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
            if (mob == null) continue;
            mob.setPersistenceRequired();
            mob.snapTo(worldPos.getX() + 0.5, worldPos.getY(), worldPos.getZ() + 0.5,
                random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                EntitySpawnReason.STRUCTURE, null);
            level.addFreshEntityWithPassengers(mob);
        }
    }
}
