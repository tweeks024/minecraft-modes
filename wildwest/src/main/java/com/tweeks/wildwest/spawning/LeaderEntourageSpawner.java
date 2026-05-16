package com.tweeks.wildwest.spawning;

import com.tweeks.wildwest.entity.WildWestMob;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Markings;
import net.minecraft.world.entity.animal.equine.Variant;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Helper that spawns a leader's mount + footsoldier entourage in one shot.
 *
 * Used by both the sherrif and bandit-leader spawn paths so the dance
 * (tame+saddle a horse, mount the leader, scatter 2 followers nearby) lives
 * in exactly one place.
 *
 * <p>API adaptations vs. the original Phase 3 plan (MC 26.1.2 / NeoForge):
 * <ul>
 *   <li>{@code Horse} / {@code Variant} / {@code Markings} live in
 *       {@code net.minecraft.world.entity.animal.equine}, not the historical
 *       {@code .horse} package.</li>
 *   <li>{@code Horse#setVariant} is private. We set the variant via the
 *       data-component path: {@code horse.setComponent(DataComponents.HORSE_VARIANT, variant)}.</li>
 *   <li>{@code Horse#setMarkings} is not exposed publicly and there is no
 *       data-component for markings. We accept the parameter for API parity
 *       and future-proofing but cannot apply it; markings default to
 *       {@link Markings#NONE} (typeVariant upper byte stays 0). If a future
 *       NeoForge version adds an accessor, swap it in here.</li>
 *   <li>{@code AbstractHorse#equipSaddle} is gone. Saddle is now an equipment
 *       slot ({@code EquipmentSlot.SADDLE}); we set it directly via
 *       {@code setItemSlot}.</li>
 *   <li>{@code Mob#startRiding(Entity, boolean)} no longer exists. The mob
 *       override is {@code startRiding(Entity, boolean force, boolean sendEventAndTriggers)}.</li>
 * </ul>
 */
public final class LeaderEntourageSpawner {
    private LeaderEntourageSpawner() {}

    public static final int FOLLOWER_COUNT = 2;
    public static final int FOLLOWER_RADIUS = 4;
    public static final double HORSE_MAX_HEALTH = 30.0;

    public static <T extends WildWestMob> void spawnEntourage(
            ServerLevel level,
            WildWestMob leader,
            EntityType<T> followerType,
            Variant horseVariant,
            Markings horseMarkings) {
        spawnHorseAndMount(level, leader, horseVariant, horseMarkings);
        spawnFollowers(level, leader, followerType);
    }

    private static void spawnHorseAndMount(
            ServerLevel level, WildWestMob leader, Variant variant, Markings markings) {
        Horse horse = new Horse(EntityType.HORSE, level);
        horse.setPos(leader.getX(), leader.getY(), leader.getZ());
        horse.setComponent(DataComponents.HORSE_VARIANT, variant);
        // Markings: no public setter / data-component in this MC version. Default is NONE.
        // Parameter retained for API parity with the plan and future compatibility.
        horse.setTamed(true);
        horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        horse.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HORSE_MAX_HEALTH);
        horse.setHealth((float) HORSE_MAX_HEALTH);
        level.addFreshEntity(horse);
        leader.startRiding(horse, true, false);
    }

    private static <T extends WildWestMob> void spawnFollowers(
            ServerLevel level, WildWestMob leader, EntityType<T> followerType) {
        RandomSource random = level.getRandom();
        BlockPos leaderPos = leader.blockPosition();
        for (int i = 0; i < FOLLOWER_COUNT; i++) {
            int dx = random.nextInt(FOLLOWER_RADIUS * 2 + 1) - FOLLOWER_RADIUS;
            int dz = random.nextInt(FOLLOWER_RADIUS * 2 + 1) - FOLLOWER_RADIUS;
            int x = leaderPos.getX() + dx;
            int z = leaderPos.getZ() + dz;
            // Snap Y to the local heightmap so followers don't spawn embedded
            // in terrain when the leader spawned on uneven ground (hill, cliff).
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            T follower = followerType.create(level, EntitySpawnReason.NATURAL);
            if (follower == null) continue;
            follower.setPos(x + 0.5, y, z + 0.5);
            follower.setPersistenceRequired();
            level.addFreshEntity(follower);
        }
    }
}
