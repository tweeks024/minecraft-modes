package com.tweeks.wildwest.event;

import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.entity.RedstoneGolemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Construction trigger for the Redstone Golem. Listens for a
 * {@link PrimedTnt} joining the level. When the TNT's block position is the
 * "head" of a T-shaped redstone-block frame, the handler consumes the four
 * redstone blocks, discards the TNT, and spawns a
 * {@link RedstoneGolemEntity} at the torso position.
 *
 * <p>The T-shape (with the head at the TNT position) is:
 * <pre>
 *           [TNT]            &lt;- head (TNT block position)
 *   [redstone][redstone][redstone]   &lt;- shoulders (head.below())
 *           [redstone]      &lt;- torso (head.below(2))
 * </pre>
 *
 * <p>We listen on {@link EntityJoinLevelEvent} rather than
 * {@code BlockEvent.EntityPlaceEvent} because placing a redstone_block adjacent
 * to a TNT block instantly primes the TNT via {@code TntBlock.onPlace}, so by
 * the time placement events fire the TNT block is already gone and a
 * {@link PrimedTnt} entity is in the world. Hooking the entity-join event
 * cleanly catches that auto-prime path.
 *
 * <p>Auto-registered on the GAME bus via {@link EventBusSubscriber}, mirroring
 * the {@code ZombieVirusHandler} pattern.
 *
 * <p>API notes (NeoForge 26.1.2):
 * <ul>
 *   <li>{@code EntityJoinLevelEvent#getLevel()} returns a {@link Level}
 *       directly — no cast needed.</li>
 *   <li>NBT {@code CompoundTag#getBoolean(String)} has been replaced by
 *       {@code getBooleanOr(String, boolean)} in this version; we use the
 *       latter to read the consumed-marker flag.</li>
 *   <li>{@code EntityType#create(Level, EntitySpawnReason)} replaces the
 *       legacy {@code MobSpawnType} enum; mirrored from
 *       {@code HerobrineMeteorGoal}.</li>
 *   <li>{@code DustParticleOptions(int, float)} — packed {@code 0xRRGGBB}
 *       color int plus a scale. (The {@code Vector3f}-based constructor that
 *       appears in some other NeoForge builds is not present on this
 *       runtime classpath; the int form is what compiles here.)</li>
 *   <li>{@code Entity#snapTo(double, double, double, float, float)} replaces
 *       the legacy {@code moveTo} 5-arg overload for hard-positioning a
 *       just-spawned entity.</li>
 *   <li>{@code Level#random} is protected; use {@code Level#getRandom()}.</li>
 * </ul>
 */
@EventBusSubscriber(modid = WildWestMod.MOD_ID)
public final class RedstoneGolemConstructionHandler {

    public static final String CONSUMED_TAG = "wildwest:golem_consumed";

    private RedstoneGolemConstructionHandler() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof PrimedTnt primedTnt)) return;
        if (event.getLevel().isClientSide()) return;
        // Re-entry guard: this handler discards the PrimedTnt at the end of
        // spawnGolem(), but mark the entity first so any duplicate join events
        // (loaded from disk, mod re-fires, etc.) bail out cleanly.
        if (primedTnt.getPersistentData().getBooleanOr(CONSUMED_TAG, false)) return;

        Level level = event.getLevel();
        BlockPos headPos = primedTnt.blockPosition();

        Direction.Axis matchedAxis = null;
        if (tryMatch(level, headPos, Direction.Axis.X)) {
            matchedAxis = Direction.Axis.X;
        } else if (tryMatch(level, headPos, Direction.Axis.Z)) {
            matchedAxis = Direction.Axis.Z;
        }
        if (matchedAxis != null) {
            spawnGolem(level, headPos, primedTnt, matchedAxis);
        }
    }

    /**
     * Tests whether {@code headPos} is the head of a T-pattern oriented along
     * {@code axis}: the four shoulder/torso positions below must all be
     * redstone blocks. Returns true on match.
     */
    private static boolean tryMatch(Level level, BlockPos headPos, Direction.Axis axis) {
        BlockPos shoulderCenter = headPos.below();
        BlockPos shoulderPos = shoulderCenter.relative(axis, 1);
        BlockPos shoulderNeg = shoulderCenter.relative(axis, -1);
        BlockPos torso = headPos.below(2);
        return isRedstone(level, shoulderCenter)
            && isRedstone(level, shoulderPos)
            && isRedstone(level, shoulderNeg)
            && isRedstone(level, torso);
    }

    private static boolean isRedstone(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.REDSTONE_BLOCK);
    }

    private static void spawnGolem(Level level, BlockPos headPos, PrimedTnt primedTnt, Direction.Axis axis) {
        BlockPos shoulderCenter = headPos.below();
        BlockPos torso = headPos.below(2);
        BlockPos shoulderPos = shoulderCenter.relative(axis, 1);
        BlockPos shoulderNeg = shoulderCenter.relative(axis, -1);

        level.setBlockAndUpdate(shoulderPos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(shoulderNeg, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(shoulderCenter, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(torso, Blocks.AIR.defaultBlockState());

        // Mark + discard the TNT so it doesn't explode and so any duplicate
        // join events that fire for it bail out.
        primedTnt.getPersistentData().putBoolean(CONSUMED_TAG, true);
        primedTnt.discard();

        Vec3 spawnAt = Vec3.atBottomCenterOf(torso);
        RedstoneGolemEntity golem = ModEntities.REDSTONE_GOLEM.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (golem == null) {
            WildWestMod.LOGGER.error(
                "Redstone Golem failed to spawn at {} after consuming construction materials; entity type returned null",
                spawnAt);
            return;
        }
        golem.snapTo(spawnAt.x, spawnAt.y, spawnAt.z, 0.0F, 0.0F);
        if (!level.addFreshEntity(golem)) {
            WildWestMod.LOGGER.error(
                "Redstone Golem rejected by level.addFreshEntity at {}; construction materials lost", spawnAt);
            return;
        }

        level.playSound(null, spawnAt.x, spawnAt.y, spawnAt.z,
            SoundEvents.IRON_GOLEM_REPAIR, SoundSource.HOSTILE, 1.0F, 1.0F);

        if (level instanceof ServerLevel serverLevel) {
            // This NeoForge version's DustParticleOptions constructor takes a
            // packed 0xRRGGBB int (not the Vector3f form some other versions
            // expose). 0xFF0000 = redstone red.
            DustParticleOptions dust = new DustParticleOptions(0xFF0000, 1.5F);
            var rng = level.getRandom();
            for (int i = 0; i < 20; i++) {
                double dx = rng.nextGaussian() * 0.5;
                double dy = rng.nextDouble() * 1.5;
                double dz = rng.nextGaussian() * 0.5;
                serverLevel.sendParticles(dust,
                    spawnAt.x + dx, spawnAt.y + dy, spawnAt.z + dz,
                    1, 0, 0, 0, 0);
            }
        }
    }
}
