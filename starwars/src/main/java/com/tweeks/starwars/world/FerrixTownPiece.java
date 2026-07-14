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
 * A single Ferrix town fragment, generated as a scattered feature.
 *
 * <p>Extends {@link ScatteredFeaturePiece} so it inherits vanilla's proven
 * placement machinery: a random horizontal orientation, local-to-world
 * coordinate mapping through the oriented bounding box, terrain-height snapping
 * via {@code updateAverageGroundHeight}, and persistence of the derived ground
 * height plus dimensions across save/reload — the same pattern as
 * {@link EscapePodPiece}.
 */
public class FerrixTownPiece extends ScatteredFeaturePiece {

    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(
        Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "chests/ferrix_town"));

    /**
     * @param minBlockX world X of the chunk's min corner (the town's local x=0)
     * @param minBlockZ world Z of the chunk's min corner (the town's local z=0)
     */
    public FerrixTownPiece(RandomSource random, int minBlockX, int minBlockZ) {
        super(ModStructures.FERRIX_TOWN_PIECE.get(), minBlockX, 64, minBlockZ,
            FerrixTownLayout.SIZE_X, FerrixTownLayout.SIZE_Y, FerrixTownLayout.SIZE_Z,
            getRandomHorizontalDirection(random));
    }

    public FerrixTownPiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.FERRIX_TOWN_PIECE.get(), tag);
    }

    // addAdditionalSaveData is inherited from ScatteredFeaturePiece: it persists
    // Width/Height/Depth and the derived HPos so reloads reproduce the same box.

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        // Snap the box to the average ground height under it before placing any
        // blocks; bails out (leaving the town ungenerated in this chunk pass)
        // if no columns of the box fall inside the current chunk.
        if (!this.updateAverageGroundHeight(level, box, 0)) {
            return;
        }
        for (var p : FerrixTownLayout.placements()) {
            var state = switch (p.kind()) {
                // Ferrix masonry: mostly brick with a deterministic scatter of
                // stone brick, so the walls read weathered rather than uniform.
                case WALL -> ((p.dx() + p.dy() + p.dz()) % 3 == 0
                    ? Blocks.STONE_BRICKS : Blocks.BRICKS).defaultBlockState();
                case TOWER_WALL -> Blocks.BRICKS.defaultBlockState();
                case ROOF -> Blocks.DEEPSLATE_TILES.defaultBlockState();
                case FLOOR -> Blocks.SMOOTH_STONE.defaultBlockState();
                case WINDOW -> Blocks.GLASS.defaultBlockState();
                case DOOR_AIR, AIR -> Blocks.AIR.defaultBlockState();
                case BELL -> Blocks.BELL.defaultBlockState();
                case CHEST -> null;                          // handled via createChest
                case STORMTROOPER -> Blocks.AIR.defaultBlockState(); // carve air so the patrol doesn't suffocate on slopes; entity spawn below
            };
            if (state != null) {
                this.placeBlock(level, state, p.dx(), p.dy(), p.dz(), box);
            } else if (p.kind() == FerrixTownLayout.Kind.CHEST) {
                this.createChest(level, box, random, p.dx(), p.dy(), p.dz(), LOOT);
            }
        }

        // Imperial patrol: spawn the stormtroopers in the street at generation
        // time. Coordinates are mapped local->world through the oriented box
        // (getWorldPos), so the patrol rotates/mirrors with the piece — same
        // pattern as ImperialOutpostPiece's garrison loop. Skipped if a marker
        // falls outside the current chunk's box (the piece may straddle chunk
        // borders).
        for (var p : FerrixTownLayout.placements()) {
            if (p.kind() != FerrixTownLayout.Kind.STORMTROOPER) continue;
            EntityType<? extends Mob> type = ModEntities.STORMTROOPER.get();
            BlockPos worldPos = this.getWorldPos(p.dx(), p.dy(), p.dz());
            if (!box.isInside(worldPos)) continue;
            Mob mob = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
            if (mob == null) continue;
            mob.setPersistenceRequired();
            mob.snapTo(worldPos.getX() + 0.5, worldPos.getY(), worldPos.getZ() + 0.5,
                random.nextFloat() * 360.0F, 0.0F);
            // finalizeSpawn is load-bearing: SwMob#finalizeSpawn equips the mob's
            // weapon, and addFreshEntityWithPassengers does not call it.
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                EntitySpawnReason.STRUCTURE, null);
            level.addFreshEntityWithPassengers(mob);
        }
    }
}
