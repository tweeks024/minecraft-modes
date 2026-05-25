package com.tweeks.wildwest.spawning;

import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.entity.AnomalyEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Periodic-tick Anomaly village-interloper spawner. Mirrors
 * {@link LawmanVillageSpawner}: every {@link #CHECK_INTERVAL_TICKS}, iterate
 * overworld players, resolve which village (if any) each is inside via
 * {@code structureManager().getStructureWithPieceAt(pos, StructureTags.VILLAGE)},
 * roll {@link #SPAWN_CHANCE} to spawn an {@link AnomalyEntity} inside the
 * village's bounding box (capped at {@link #MAX_PER_VILLAGE} per village).
 *
 * Why a tick-poll instead of cancel-and-replace on a FinalizeSpawnEvent:
 *   (a) consistent with the existing village-spawn infra in this module
 *       ({@link LawmanVillageSpawner}, {@link BanditLeaderPackSpawner}),
 *   (b) doesn't fight with raid/breeding/structure spawn reasons.
 *
 * Constants are kept inline as primitive {@code public static final} fields so
 * the constants test JVM can read them without triggering MC bootstrap — javac
 * inlines such constants at compile time, so {@code AnomalyVillageSpawner}
 * itself is never loaded by the test.
 */
@EventBusSubscriber(modid = WildWestMod.MOD_ID)
public final class AnomalyVillageSpawner {
    private AnomalyVillageSpawner() {}

    public static final int CHECK_INTERVAL_TICKS = 6000;  // ~5 game minutes
    public static final float SPAWN_CHANCE = 0.05f;
    public static final int MAX_PER_VILLAGE = 1;

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

        // Dedup villages across players: two players standing in the same
        // village shouldn't trigger two independent spawn rolls per interval.
        Set<Long> seenVillages = new HashSet<>();
        for (ServerPlayer player : sl.players()) {
            BlockPos playerPos = player.blockPosition();
            StructureStart village = sl.structureManager()
                .getStructureWithPieceAt(playerPos, StructureTags.VILLAGE);
            if (village == null || !village.isValid()) continue;

            BoundingBox bb = village.getBoundingBox();
            long key = (((long) bb.minX()) << 32) ^ (bb.minZ() & 0xFFFFFFFFL);
            if (!seenVillages.add(key)) continue;

            AABB villageArea = new AABB(
                bb.minX(), bb.minY(), bb.minZ(),
                bb.maxX() + 1, bb.maxY() + 1, bb.maxZ() + 1);

            int existing = sl.getEntitiesOfClass(AnomalyEntity.class, villageArea).size();
            if (existing >= MAX_PER_VILLAGE) continue;

            if (sl.getRandom().nextFloat() >= SPAWN_CHANCE) continue;

            int x = bb.minX() + sl.getRandom().nextInt(Math.max(1, bb.getXSpan()));
            int z = bb.minZ() + sl.getRandom().nextInt(Math.max(1, bb.getZSpan()));
            int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

            AnomalyEntity anomaly = ModEntities.ANOMALY.get().create(sl, EntitySpawnReason.NATURAL);
            if (anomaly == null) continue;
            anomaly.snapTo((double) x + 0.5, (double) y, (double) z + 0.5,
                sl.getRandom().nextFloat() * 360f, 0f);
            sl.addFreshEntity(anomaly);
        }
    }
}
