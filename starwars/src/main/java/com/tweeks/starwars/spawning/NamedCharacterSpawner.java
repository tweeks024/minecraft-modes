package com.tweeks.starwars.spawning;

import com.tweeks.starwars.ModEntities;
import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.entity.LukeSavedData;
import com.tweeks.starwars.entity.NamedCharacterSavedData;
import com.tweeks.starwars.entity.ObiWanSavedData;
import com.tweeks.starwars.entity.StormtrooperEntity;
import com.tweeks.starwars.entity.SwMob;
import com.tweeks.starwars.entity.VaderSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.List;
import java.util.Set;

/**
 * Periodic-tick spawner that keeps at most one of each named character
 * (Vader, Luke, Obi-Wan) alive per server, plus a stormtrooper escort for
 * Vader.
 *
 * <p>Approach used: mirrors {@code LawmanVillageSpawner} / {@code
 * AnomalyVillageSpawner} from the wildwest module — a static tick counter
 * gated to {@link Level#OVERWORLD}, incremented on {@link LevelTickEvent.Post}
 * and reset once it hits the check interval. Registration reuses the same
 * mechanism: a bare {@code @EventBusSubscriber(modid = ...)} class with a
 * {@code @SubscribeEvent} handler — NeoForge auto-discovers and subscribes it
 * at classload time, so no explicit wiring in {@link StarWarsMod} is needed
 * (confirmed by checking that {@code WildWestMod} never references its own
 * {@code @EventBusSubscriber} spawner classes either).
 *
 * <p>Entity construction mirrors {@code LawmanVillageSpawner#spawn} (heightmap
 * surface via {@link Heightmap.Types#MOTION_BLOCKING_NO_LEAVES}, {@code
 * EntityType#create} + {@code addFreshEntity}) with one addition: an explicit
 * {@code finalizeSpawn} call between construction and {@code addFreshEntity},
 * following the manual-spawn pattern in {@code ReaperScytheItem} (wildwest).
 * That call is load-bearing here — unlike the lawman/anomaly mobs, {@link
 * SwMob#finalizeSpawn} equips the mob's weapon, and each named character's
 * own {@code finalizeSpawn} override is what actually claims its {@link
 * NamedCharacterSavedData} singleton. The explicit {@code data.setAlive(...)}
 * below is therefore a harmless re-claim of state finalizeSpawn already set.
 *
 * <p>The stormtrooper ring uses the same square-jitter-around-a-center
 * pattern as {@code LeaderEntourageSpawner#spawnFollowers} (random offset
 * within a radius, snapped to the local heightmap), reimplemented here rather
 * than imported because it operates on {@code SwMob}, not wildwest's {@code
 * WildWestMob}, and this module has no dependency on wildwest.
 */
@EventBusSubscriber(modid = StarWarsMod.MOD_ID)
public final class NamedCharacterSpawner {
    private NamedCharacterSpawner() {}

    public static final int CHECK_INTERVAL_TICKS = 1200; // 1 game minute
    public static final float SPAWN_CHANCE = 0.15f;
    public static final int MIN_DISTANCE = 24;
    public static final int MAX_DISTANCE = 40;
    public static final int TROOPER_ESCORT_COUNT = 3;
    public static final int TROOPER_RING_RADIUS = 4;

    private static final Set<ResourceKey<Biome>> VADER_BIOMES =
        Set.of(Biomes.DESERT, Biomes.BADLANDS, Biomes.PLAINS);
    private static final Set<ResourceKey<Biome>> JEDI_BIOMES =
        Set.of(Biomes.FOREST, Biomes.JUNGLE, Biomes.TAIGA, Biomes.PLAINS);

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        // LevelTickEvent fires once per loaded dimension per tick. Named
        // characters only gate on overworld biomes, so restrict to the
        // overworld — this also keeps tickCounter incrementing at 1x per
        // game tick instead of 3x when nether/end are also loaded (mirrors
        // LawmanVillageSpawner / AnomalyVillageSpawner).
        if (sl.dimension() != Level.OVERWORLD) return;
        if (++tickCounter < CHECK_INTERVAL_TICKS) return;
        tickCounter = 0;

        tryRollCharacter(sl, VaderSavedData.get(sl.getServer()),
            ModEntities.DARTH_VADER.get(), VADER_BIOMES, true);
        tryRollCharacter(sl, LukeSavedData.get(sl.getServer()),
            ModEntities.LUKE_SKYWALKER.get(), JEDI_BIOMES, false);
        tryRollCharacter(sl, ObiWanSavedData.get(sl.getServer()),
            ModEntities.OBI_WAN.get(), JEDI_BIOMES, false);
    }

    /** Core roll, shared by all three characters. */
    private static <T extends SwMob> void tryRollCharacter(
            ServerLevel level,
            NamedCharacterSavedData data,
            EntityType<T> type,
            Set<ResourceKey<Biome>> biomes,
            boolean withTrooperEscort) {
        if (data.isAlive()) return;

        RandomSource random = level.getRandom();
        if (random.nextFloat() >= SPAWN_CHANCE) return;

        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return;
        ServerPlayer player = players.get(random.nextInt(players.size()));

        BlockPos pos = pickSpawnPosition(level, player.blockPosition(), type,
            MIN_DISTANCE, MAX_DISTANCE, random);
        if (pos == null) return;

        if (!isInAnyBiome(level, pos, biomes)) return;

        T mob = spawnAt(level, type, pos);
        if (mob == null) return;
        data.setAlive(mob.getUUID(), level.dimension());

        if (withTrooperEscort) {
            spawnTrooperEscort(level, pos);
        }
    }

    /**
     * Picks a valid ground position at a random distance in
     * {@code [minDistance, maxDistance]} from {@code anchor}: heightmap
     * surface, not in fluid, category-appropriate light/spawn rules.
     *
     * <p>Kept as one parameterized method — taking an arbitrary anchor rather
     * than assuming "random player" — so Tasks 28-29 can re-anchor named
     * characters to a located structure's position without touching the
     * roll/spawn logic above. No anchor-resolution logic beyond "given point"
     * is added here yet (YAGNI until those tasks land).
     */
    private static BlockPos pickSpawnPosition(
            ServerLevel level, BlockPos anchor, EntityType<? extends Mob> type,
            int minDistance, int maxDistance, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        int distance = minDistance + random.nextInt(maxDistance - minDistance + 1);
        int x = anchor.getX() + (int) Math.round(Math.cos(angle) * distance);
        int z = anchor.getZ() + (int) Math.round(Math.sin(angle) * distance);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);

        if (!level.getFluidState(pos).isEmpty()) return null;
        if (!checkCategorySpawnRules(type, level, pos, random)) return null;
        return pos;
    }

    /**
     * Light/placement validity for the mob's {@link MobCategory}, mirroring
     * the split {@code StarWarsMod} already uses when registering natural
     * spawn placements: {@code MONSTER} types (Vader, troopers) get vanilla's
     * darkness-gated monster rule; other categories (Luke, Obi-Wan — {@code
     * CREATURE}) get the permissive generic ground-mob rule.
     */
    private static boolean checkCategorySpawnRules(
            EntityType<? extends Mob> type, ServerLevel level, BlockPos pos, RandomSource random) {
        if (type.getCategory() == MobCategory.MONSTER) {
            return Monster.checkMonsterSpawnRules(type, level, EntitySpawnReason.NATURAL, pos, random);
        }
        return PathfinderMob.checkMobSpawnRules(type, level, EntitySpawnReason.NATURAL, pos, random);
    }

    private static boolean isInAnyBiome(ServerLevel level, BlockPos pos, Set<ResourceKey<Biome>> biomes) {
        for (ResourceKey<Biome> key : biomes) {
            if (level.getBiome(pos).is(key)) return true;
        }
        return false;
    }

    /**
     * Constructs, finalizes (weapon equip + singleton claim for named
     * characters), and adds the entity — mirrors {@code
     * LawmanVillageSpawner#spawn} plus the explicit {@code finalizeSpawn}
     * call from {@code ReaperScytheItem}'s manual-spawn path.
     */
    private static <T extends SwMob> T spawnAt(ServerLevel level, EntityType<T> type, BlockPos pos) {
        T entity = type.create(level, EntitySpawnReason.NATURAL);
        if (entity == null) return null;
        entity.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
        entity.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
        level.addFreshEntity(entity);
        return entity;
    }

    /**
     * Vader's stormtrooper ring: same square-jitter-around-a-center pattern
     * as {@code LeaderEntourageSpawner#spawnFollowers}, re-validated per spot
     * (fluid + spawn rules) and skipped (not retried) on failure.
     */
    private static void spawnTrooperEscort(ServerLevel level, BlockPos center) {
        RandomSource random = level.getRandom();
        EntityType<StormtrooperEntity> trooperType = ModEntities.STORMTROOPER.get();
        for (int i = 0; i < TROOPER_ESCORT_COUNT; i++) {
            BlockPos pos = pickSpawnPosition(level, center, trooperType, 1, TROOPER_RING_RADIUS, random);
            if (pos == null) continue;
            spawnAt(level, trooperType, pos);
        }
    }
}
