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
 * Echo Base, the rebel snow bunker on Hoth, generated as a scattered feature.
 *
 * <p>Extends {@link ScatteredFeaturePiece} so it inherits vanilla's proven
 * placement machinery: a random horizontal orientation, local-to-world
 * coordinate mapping through the oriented bounding box, terrain-height snapping
 * via {@code updateAverageGroundHeight}, and persistence of the derived ground
 * height plus dimensions across save/reload — the same pattern as
 * {@link EscapePodPiece}.
 */
public class EchoBasePiece extends ScatteredFeaturePiece {

    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(
        Registries.LOOT_TABLE,
        Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "chests/echo_base"));

    /**
     * @param minBlockX world X of the chunk's min corner (the base's local x=0)
     * @param minBlockZ world Z of the chunk's min corner (the base's local z=0)
     */
    public EchoBasePiece(RandomSource random, int minBlockX, int minBlockZ) {
        super(ModStructures.ECHO_BASE_PIECE.get(), minBlockX, 64, minBlockZ,
            EchoBaseLayout.SIZE_X, EchoBaseLayout.SIZE_Y, EchoBaseLayout.SIZE_Z,
            getRandomHorizontalDirection(random));
    }

    public EchoBasePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.ECHO_BASE_PIECE.get(), tag);
    }

    // addAdditionalSaveData is inherited from ScatteredFeaturePiece: it persists
    // Width/Height/Depth and the derived HPos so reloads reproduce the same box.

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager,
                            ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pos) {
        // Snap the box to the average ground height under it before placing any
        // blocks; bails out (leaving the base ungenerated in this chunk pass)
        // if no columns of the box fall inside the current chunk.
        if (!this.updateAverageGroundHeight(level, box, 0)) {
            return;
        }
        for (var p : EchoBaseLayout.placements()) {
            var state = switch (p.kind()) {
                case WALL, ROOF -> Blocks.SNOW_BLOCK.defaultBlockState();
                case ICE, FLOOR -> Blocks.PACKED_ICE.defaultBlockState();
                case FRAME, GEN_CASE -> Blocks.IRON_BLOCK.defaultBlockState();
                case GEN_CORE -> Blocks.REDSTONE_BLOCK.defaultBlockState();
                case BED_HEAD -> Blocks.WHITE_WOOL.defaultBlockState();
                case BED_FOOT -> Blocks.RED_WOOL.defaultBlockState();
                case HANGAR_AIR, DOOR_AIR, AIR -> Blocks.AIR.defaultBlockState();
                case CHEST -> null;                          // handled via createChest
                case REBEL, ASTROMECH -> Blocks.AIR.defaultBlockState(); // carve air so the garrison doesn't suffocate on slopes; entity spawn below
            };
            if (state != null) {
                this.placeBlock(level, state, p.dx(), p.dy(), p.dz(), box);
            } else if (p.kind() == EchoBaseLayout.Kind.CHEST) {
                this.createChest(level, box, random, p.dx(), p.dy(), p.dz(), LOOT);
            }
        }

        // Garrison: spawn the rebel defenders at generation time. Coordinates
        // are mapped local->world through the oriented box (getWorldPos), so
        // the garrison rotates/mirrors with the piece — same pattern as
        // ImperialOutpostPiece's garrison loop. Skipped if a marker falls
        // outside the current chunk's box (the piece may straddle chunk
        // borders).
        for (var p : EchoBaseLayout.placements()) {
            EntityType<? extends Mob> type = switch (p.kind()) {
                case REBEL -> ModEntities.REBEL_TROOPER.get();
                case ASTROMECH -> ModEntities.ASTROMECH.get();
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
            // mob's weapon, and addFreshEntityWithPassengers does not call it.
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                EntitySpawnReason.STRUCTURE, null);
            level.addFreshEntityWithPassengers(mob);
        }
    }
}
