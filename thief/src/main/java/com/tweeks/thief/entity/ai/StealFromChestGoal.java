package com.tweeks.thief.entity.ai;

import com.tweeks.thief.entity.RevealState;
import com.tweeks.thief.entity.ThiefEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.EnumSet;
import java.util.Optional;

/**
 * Finds the nearest chest within 24 blocks, paths to it, opens it for ~1s,
 * transfers up to {@link #MAX_STACKS_PER_VISIT} stacks into the Thief's
 * stolen-items container, then closes the chest. Active only while
 * {@link RevealState#DISGUISED} and only while there's room.
 */
public class StealFromChestGoal extends Goal {

    private static final int SEARCH_RADIUS = 24;
    private static final int OPEN_TICKS = 20;
    private static final int MAX_STACKS_PER_VISIT = 2;
    private static final double APPROACH_RANGE_SQR = 4.0;

    private final ThiefEntity thief;

    @org.jetbrains.annotations.Nullable
    private BlockPos targetChest;
    private int openTicker;
    private boolean opened;

    public StealFromChestGoal(ThiefEntity thief) {
        this.thief = thief;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (thief.getRevealState() != RevealState.DISGUISED) return false;
        if (containerFull(thief.getStolenItems())) return false;
        Optional<BlockPos> chest = findNearestChest();
        if (chest.isEmpty()) return false;
        targetChest = chest.get();
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return targetChest != null
            && thief.getRevealState() == RevealState.DISGUISED
            && !containerFull(thief.getStolenItems())
            && getChestEntity().isPresent();
    }

    @Override
    public void start() {
        openTicker = 0;
        opened = false;
        if (targetChest != null) {
            thief.getNavigation().moveTo(
                targetChest.getX() + 0.5, targetChest.getY(), targetChest.getZ() + 0.5, 0.6);
        }
    }

    @Override
    public void stop() {
        if (opened) {
            getChestEntity().ifPresent(c -> c.stopOpen(thief));
            thief.setOpenContainerPos(null);
        }
        targetChest = null;
        thief.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (targetChest == null) return;
        thief.getLookControl().setLookAt(
            targetChest.getX() + 0.5, targetChest.getY() + 0.5, targetChest.getZ() + 0.5);
        if (thief.distanceToSqr(targetChest.getX() + 0.5, targetChest.getY(), targetChest.getZ() + 0.5)
                <= APPROACH_RANGE_SQR) {
            if (!opened) {
                net.minecraft.world.entity.player.Player observer =
                    thief.level().getNearestPlayer(thief, 16.0);
                if (observer != null && observer.hasLineOfSight(thief)) {
                    if (thief.distanceTo(observer) > 8.0) {
                        thief.enterSuspicious();
                    } else {
                        thief.triggerReveal(observer);
                    }
                    stop();
                    return;
                }
                getChestEntity().ifPresent(c -> {
                    thief.setOpenContainerPos(targetChest);
                    c.startOpen(thief);
                    opened = true;
                });
            }
            openTicker++;
            if (openTicker >= OPEN_TICKS) {
                getChestEntity().ifPresent(this::transferItems);
                stop();
            }
        }
    }

    private Optional<ChestBlockEntity> getChestEntity() {
        if (targetChest == null) return Optional.empty();
        BlockEntity be = thief.level().getBlockEntity(targetChest);
        return be instanceof ChestBlockEntity ce ? Optional.of(ce) : Optional.empty();
    }

    private void transferItems(ChestBlockEntity chest) {
        int stacksTaken = 0;
        for (int i = 0; i < chest.getContainerSize() && stacksTaken < MAX_STACKS_PER_VISIT; i++) {
            ItemStack stack = chest.getItem(i);
            if (stack.isEmpty()) continue;
            if (addToStolen(stack)) {
                chest.setItem(i, ItemStack.EMPTY);
                stacksTaken++;
            }
        }
        chest.setChanged();
    }

    private boolean addToStolen(ItemStack stack) {
        Container c = thief.getStolenItems();
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (c.getItem(i).isEmpty()) {
                c.setItem(i, stack);
                return true;
            }
        }
        return false;
    }

    private static boolean containerFull(Container c) {
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (c.getItem(i).isEmpty()) return false;
        }
        return true;
    }

    private Optional<BlockPos> findNearestChest() {
        BlockPos origin = thief.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockEntity be = thief.level().getBlockEntity(cursor);
                    if (!(be instanceof ChestBlockEntity)) continue;
                    if (thief.getHideoutPos().filter(p -> p.equals(cursor)).isPresent()) continue;
                    double d = cursor.distSqr(origin);
                    if (d < bestDist) {
                        bestDist = d;
                        best = cursor.immutable();
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }
}
