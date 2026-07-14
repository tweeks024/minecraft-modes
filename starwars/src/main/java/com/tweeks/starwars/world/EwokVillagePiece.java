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
 * An Ewok treetop village on Endor, generated as a scattered feature.
 *
 * <p>Extends {@link ScatteredFeaturePiece} so it inherits vanilla's proven
 * placement machinery: a random horizontal orientation, local-to-world
 * coordinate mapping through the oriented bounding box, terrain-height snapping
 * via {@code updateAverageGroundHeight}, and persistence of the derived ground
 * height plus dimensions across save/reload — the same pattern as
 * {@link EscapePodPiece}.
 */
public class EwokVillagePiece extends ScatteredFeaturePiece {

    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(
        Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "chests/ewok_village"));

    /**
     * @param minBlockX world X of the chunk's min corner (the village's local x=0)
     * @param minBlockZ world Z of the chunk's min corner (the village's local z=0)
     */
    public EwokVillagePiece(RandomSource random, int minBlockX, int minBlockZ) {
        super(ModStructures.EWOK_VILLAGE_PIECE.get(), minBlockX, 64, minBlockZ,
            EwokVillageLayout.SIZE_X, EwokVillageLayout.SIZE_Y, EwokVillageLayout.SIZE_Z,
            getRandomHorizontalDirection(random));
    }

    public EwokVillagePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.EWOK_VILLAGE_PIECE.get(), tag);
    }

    // addAdditionalSaveData is inherited from ScatteredFeaturePiece: it persists
    // Width/Height/Depth and the derived HPos so reloads reproduce the same box.

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        // Snap the box to the average ground height under it before placing any
        // blocks; bails out (leaving the village ungenerated in this chunk pass)
        // if no columns of the box fall inside the current chunk.
        if (!this.updateAverageGroundHeight(level, box, 0)) {
            return;
        }
        for (var p : EwokVillageLayout.placements()) {
            var state = switch (p.kind()) {
                case STILT -> Blocks.SPRUCE_LOG.defaultBlockState();
                case FLOOR, WALL, BRIDGE -> Blocks.SPRUCE_PLANKS.defaultBlockState();
                case ROOF -> Blocks.HAY_BLOCK.defaultBlockState();
                case RAIL -> Blocks.SPRUCE_FENCE.defaultBlockState();
                case LADDER -> Blocks.LADDER.defaultBlockState();
                case AIR -> Blocks.AIR.defaultBlockState();
                case BONFIRE, CHEST -> null;                 // handled below
                case EWOK -> Blocks.AIR.defaultBlockState();  // carve air so the marker doesn't suffocate; entity spawn below
            };
            if (state != null) {
                this.placeBlock(level, state, p.dx(), p.dy(), p.dz(), box);
            } else if (p.kind() == EwokVillageLayout.Kind.CHEST) {
                this.createChest(level, box, random, p.dx(), p.dy(), p.dz(), LOOT);
            } else {
                // Central bonfire: a 3x3 stone-brick base under a lit campfire.
                for (int bx = -1; bx <= 1; bx++) {
                    for (int bz = -1; bz <= 1; bz++) {
                        this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(),
                            p.dx() + bx, p.dy() - 1, p.dz() + bz, box);
                    }
                }
                this.placeBlock(level, Blocks.CAMPFIRE.defaultBlockState(),
                    p.dx(), p.dy(), p.dz(), box);
            }
        }

        // The village Ewoks: spawn the inhabitants at generation time.
        // Coordinates are mapped local->world through the oriented box
        // (getWorldPos), so the crowd rotates/mirrors with the piece — same
        // pattern as EchoBasePiece's garrison loop. Skipped if a marker falls
        // outside the current chunk's box (a 28x28 village may straddle chunks).
        for (var p : EwokVillageLayout.placements()) {
            EntityType<? extends Mob> type = switch (p.kind()) {
                case EWOK -> ModEntities.EWOK.get();
                default -> null;
            };
            if (type == null) continue;
            BlockPos worldPos = this.getWorldPos(p.dx(), p.dy(), p.dz());
            if (!box.isInside(worldPos)) continue;
            Mob mob = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
            if (mob == null) continue;
            mob.setPersistenceRequired();
            mob.snapTo(worldPos.getX() + 0.5, worldPos.getY(), worldPos.getZ() + 0.5,
                random.nextFloat() * 360.0F, 0.0F);
            // finalizeSpawn is load-bearing: SwMob#finalizeSpawn equips the
            // mob, and addFreshEntityWithPassengers does not call it.
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                EntitySpawnReason.STRUCTURE, null);
            level.addFreshEntityWithPassengers(mob);
        }
    }
}
