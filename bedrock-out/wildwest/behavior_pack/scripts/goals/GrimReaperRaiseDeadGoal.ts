// TODO LLM: cache miss; run :translate --with-llm to translate
// Goal: com.tweeks.wildwest.entity.ai.GrimReaperRaiseDeadGoal
//
// This file is a placeholder. Either:
//   1. Run `./gradlew :translator:translate --with-llm` with ANTHROPIC_API_KEY set, or
//   2. Hand-translate the Java below to @minecraft/server event handlers.

/*
Original Java source — translate this:

package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.GrimReaperEntity;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Grim Reaper signature ability. On a 10s cooldown, picks 2–3 ground
 * positions within 8 blocks of the current target, plays a 1s emerge
 * animation (dirt particles + sound), then spawns vanilla
 * {@link Skeleton} entities holding iron swords enchanted with
 * {@link Enchantments#SHARPNESS} level 3.
 * Spawned skeletons carry an NBT marker ({@code wildwest:grim_reaper_minion})
 * so {@link GrimReaperEntity#die} can clean them up.
 */
public class GrimReaperRaiseDeadGoal extends Goal {

    public static final String MINION_NBT_KEY = "wildwest:grim_reaper_minion";

    private static final int COOLDOWN_TICKS = 200; // 10 s
    private static final int EMERGE_DELAY_TICKS = 20; // 1 s
    private static final int MAX_PLACEMENT_ATTEMPTS = 10;
    private static final double SUMMON_RADIUS = 8.0;
    private static final int MIN_SKELETONS = 2;
    private static final int MAX_SKELETONS = 3;

    private final GrimReaperEntity reaper;
    private int cooldown = 0;
    private int emergeTimer = 0;
    private final List<BlockPos> pendingSpawns = new ArrayList<>();

    public GrimReaperRaiseDeadGoal(GrimReaperEntity reaper) {
        this.reaper = reaper;
        this.setFlags(EnumSet.noneOf(Goal.Flag.class));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        if (this.reaper.swinging) return false;
        LivingEntity target = this.reaper.getTarget();
        return target != null
            && target.isAlive()
            && this.reaper.distanceToSqr(target) <= 24.0 * 24.0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.emergeTimer > 0 && !this.pendingSpawns.isEmpty();
    }

    @Override
    public void start() {
        LivingEntity target = this.reaper.getTarget();
        if (target == null || !(this.reaper.level() instanceof ServerLevel sl)) {
            // Apply a short grace cooldown so canUse doesn't fire again next tick.
            this.cooldown = 20;
            return;
        }

        this.pendingSpawns.clear();
        RandomSource rng = this.reaper.getRandom();
        int wantCount = MIN_SKELETONS + rng.nextInt(MAX_SKELETONS - MIN_SKELETONS + 1);

        for (int attempt = 0; attempt < MAX_PLACEMENT_ATTEMPTS && this.pendingSpawns.size() < wantCount; attempt++) {
            double dx = (rng.nextDouble() * 2.0 - 1.0) * SUMMON_RADIUS;
            double dz = (rng.nextDouble() * 2.0 - 1.0) * SUMMON_RADIUS;
            int cx = (int) Math.floor(target.getX() + dx);
            int cz = (int) Math.floor(target.getZ() + dz);
            int groundY = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
            BlockPos surface = new BlockPos(cx, groundY, cz);
            if (sl.getFluidState(surface).isEmpty() && sl.getBlockState(surface).isAir()) {
                this.pendingSpawns.add(surface);
            }
        }

        if (this.pendingSpawns.size() < MIN_SKELETONS) {
            this.pendingSpawns.clear();
            this.cooldown = COOLDOWN_TICKS / 2;
            return;
        }

        for (BlockPos pos : this.pendingSpawns) {
            sl.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5,
                20, 0.3, 0.2, 0.3, 0.05);
            sl.playSound(null, pos, SoundEvents.ZOMBIE_VILLAGER_CONVERTED,
                SoundSource.HOSTILE, 0.6f, 0.6f);
        }

        this.emergeTimer = EMERGE_DELAY_TICKS;
    }

    @Override
    public void tick() {
        if (this.emergeTimer <= 0) return;
        if (!(this.reaper.level() instanceof ServerLevel sl)) return;

        if (this.emergeTimer % 4 == 0) {
            for (BlockPos pos : this.pendingSpawns) {
                sl.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()),
                    pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5,
                    4, 0.2, 0.1, 0.2, 0.02);
            }
        }

        this.emergeTimer--;
        if (this.emergeTimer > 0) return;

        // Acquire enchantment holder once for all spawns
        var sharpness = sl.registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(Enchantments.SHARPNESS);

        for (BlockPos pos : this.pendingSpawns) {
            Skeleton skeleton = net.minecraft.world.entity.EntityType.SKELETON.create(sl,
                EntitySpawnReason.MOB_SUMMONED);
            if (skeleton == null) continue;

            skeleton.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);

            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            sword.enchant(sharpness, 3);
            skeleton.setItemSlot(EquipmentSlot.MAINHAND, sword);
            skeleton.setDropChance(EquipmentSlot.MAINHAND, 0.0f);

            skeleton.getPersistentData().putBoolean(MINION_NBT_KEY, true);

            LivingEntity target = this.reaper.getTarget();
            if (target != null) skeleton.setTarget(target);

            sl.addFreshEntity(skeleton);
        }

        this.pendingSpawns.clear();
        this.cooldown = COOLDOWN_TICKS;
    }

    @Override
    public void stop() {
        this.pendingSpawns.clear();
        if (this.emergeTimer > 0) {
            this.cooldown = COOLDOWN_TICKS;
            this.emergeTimer = 0;
        }
    }
}

*/

// Empty handler so Bedrock's script engine accepts the file.
export {};
