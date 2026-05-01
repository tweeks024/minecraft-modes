package com.tweeks.wildwest.spawning;

import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.entity.DeputyEntity;
import com.tweeks.wildwest.entity.SherrifEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Periodic-tick spawner for lawmen (Deputy/Sherrif) inside villages.
 *
 * Approach used: PLAYER-PROXIMITY + STRUCTURE LOOKUP.
 * Every 6000 ticks (~5 game minutes), iterate online players. For each player,
 * use {@code ServerLevel.structureManager().getStructureWithPieceAt(pos, StructureTags.VILLAGE)}
 * to determine whether the player is currently inside a village structure. If so,
 * use the structure's bounding box to count existing lawmen and spawn more if low.
 *
 * Rationale: pure structure-iteration over loaded chunks is brittle across MC minor
 * versions and the chunk-references API exposes references, not direct starts. The
 * thief mod's existing pattern uses {@code getStructureWithPieceAt} successfully and
 * naturally restricts work to where players actually are (which is where lawmen are
 * useful). This trades coverage of unvisited villages for API stability.
 */
@EventBusSubscriber(modid = WildWestMod.MOD_ID)
public final class LawmanVillageSpawner {
    private LawmanVillageSpawner() {}

    private static final int CHECK_INTERVAL_TICKS = 6000;
    private static final int MAX_DEPUTIES_PER_VILLAGE = 2;
    private static final int MAX_SHERRIFS_PER_VILLAGE = 1;
    private static final float DEPUTY_SPAWN_CHANCE = 0.10f;
    private static final float SHERRIF_SPAWN_CHANCE = 0.05f;

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        // LevelTickEvent fires once per loaded dimension per tick. Villages
        // live in the overworld; gating here keeps tickCounter incrementing
        // at 1× per game tick instead of 3× when nether/end are also loaded.
        if (sl.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
        if (++tickCounter < CHECK_INTERVAL_TICKS) return;
        tickCounter = 0;

        for (ServerPlayer player : sl.players()) {
            BlockPos playerPos = player.blockPosition();
            StructureStart village = sl.structureManager()
                .getStructureWithPieceAt(playerPos, StructureTags.VILLAGE);
            if (village == null || !village.isValid()) continue;

            // Use the structure's bounding box as the "village area." Any village tag match
            // is acceptable — bandits spawn in plains/savanna/desert via biome modifier, and
            // lawmen show up wherever there's a village along the way.
            BoundingBox bb = village.getBoundingBox();
            AABB villageArea = new AABB(
                bb.minX(), bb.minY(), bb.minZ(),
                bb.maxX() + 1, bb.maxY() + 1, bb.maxZ() + 1);

            int deputyCount = sl.getEntitiesOfClass(DeputyEntity.class, villageArea).size();
            int sherrifCount = sl.getEntitiesOfClass(SherrifEntity.class, villageArea).size();

            BlockPos center = new BlockPos(
                (bb.minX() + bb.maxX()) / 2,
                (bb.minY() + bb.maxY()) / 2,
                (bb.minZ() + bb.maxZ()) / 2);

            if (deputyCount < MAX_DEPUTIES_PER_VILLAGE && sl.getRandom().nextFloat() < DEPUTY_SPAWN_CHANCE) {
                spawn(sl, ModEntities.DEPUTY.get(), center);
            }
            if (sherrifCount < MAX_SHERRIFS_PER_VILLAGE && sl.getRandom().nextFloat() < SHERRIF_SPAWN_CHANCE) {
                com.tweeks.wildwest.entity.SherrifEntity sherrif = spawn(sl, ModEntities.SHERRIF.get(), center);
                if (sherrif != null) {
                    com.tweeks.wildwest.spawning.LeaderEntourageSpawner.spawnEntourage(
                        sl, sherrif, ModEntities.DEPUTY.get(),
                        net.minecraft.world.entity.animal.equine.Variant.CHESTNUT,
                        net.minecraft.world.entity.animal.equine.Markings.WHITE_FIELD);
                }
            }
        }
    }

    private static <T extends Entity> T spawn(ServerLevel sl, EntityType<T> type, BlockPos pos) {
        int x = pos.getX() + sl.getRandom().nextInt(20) - 10;
        int z = pos.getZ() + sl.getRandom().nextInt(20) - 10;
        int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        T entity = type.create(sl, EntitySpawnReason.NATURAL);
        if (entity == null) return null;
        entity.snapTo((double) x + 0.5, (double) y, (double) z + 0.5, 0.0f, 0.0f);
        sl.addFreshEntity(entity);
        return entity;
    }
}
