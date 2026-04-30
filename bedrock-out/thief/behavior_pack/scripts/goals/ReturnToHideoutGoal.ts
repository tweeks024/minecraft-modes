// TODO LLM: cache miss; run :translate --with-llm to translate
// Goal: com.tweeks.thief.entity.ai.ReturnToHideoutGoal
//
// This file is a placeholder. Either:
//   1. Run `./gradlew :translator:translate --with-llm` with ANTHROPIC_API_KEY set, or
//   2. Hand-translate the Java below to @minecraft/server event handlers.

/*
Original Java source — translate this:

package com.tweeks.thief.entity.ai;

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
 * Paths the Thief to its hideout chest, deposits all stolen items, then
 * stops. Triggered when the Thief is carrying ≥1 stolen item or below
 * 30% HP. Aborts if the hideout chest is missing.
 */
public class ReturnToHideoutGoal extends Goal {

    private static final double DEPOSIT_RANGE_SQR = 2.25;
    private static final int OPEN_TICKS = 20;

    private final ThiefEntity thief;
    private int openTicker;
    private boolean opened;

    public ReturnToHideoutGoal(ThiefEntity thief) {
        this.thief = thief;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (thief.getHideoutPos().isEmpty()) return false;
        if (!hasReasonToReturn()) return false;
        return getChestEntity().isPresent();
    }

    @Override
    public boolean canContinueToUse() {
        return thief.getHideoutPos().isPresent()
            && hasReasonToReturn()
            && getChestEntity().isPresent();
    }

    private boolean hasReasonToReturn() {
        return hasStolenItems() || thief.getHealth() < thief.getMaxHealth() * 0.3f;
    }

    @Override
    public void start() {
        openTicker = 0;
        opened = false;
        thief.getHideoutPos().ifPresent(pos ->
            thief.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.7));
    }

    @Override
    public void stop() {
        if (opened) {
            getChestEntity().ifPresent(c -> c.stopOpen(thief));
            thief.setOpenContainerPos(null);
            opened = false;
        }
        thief.getNavigation().stop();
    }

    @Override
    public void tick() {
        Optional<BlockPos> hideout = thief.getHideoutPos();
        if (hideout.isEmpty()) return;
        BlockPos pos = hideout.get();
        thief.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        if (thief.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) <= DEPOSIT_RANGE_SQR) {
            if (!opened) {
                getChestEntity().ifPresent(c -> {
                    thief.setOpenContainerPos(pos);
                    c.startOpen(thief);
                    opened = true;
                });
            }
            openTicker++;
            if (openTicker >= OPEN_TICKS) {
                getChestEntity().ifPresent(this::depositInto);
                stop();
            }
        }
    }

    private boolean hasStolenItems() {
        Container c = thief.getStolenItems();
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (!c.getItem(i).isEmpty()) return true;
        }
        return false;
    }

    private Optional<ChestBlockEntity> getChestEntity() {
        return thief.getHideoutPos().flatMap(pos -> {
            BlockEntity be = thief.level().getBlockEntity(pos);
            return be instanceof ChestBlockEntity c ? Optional.of(c) : Optional.empty();
        });
    }

    private void depositInto(ChestBlockEntity chest) {
        Container src = thief.getStolenItems();
        for (int i = 0; i < src.getContainerSize(); i++) {
            ItemStack stack = src.getItem(i);
            if (stack.isEmpty()) continue;
            ItemStack remainder = tryInsert(chest, stack);
            if (remainder.isEmpty()) {
                src.setItem(i, ItemStack.EMPTY);
            } else {
                BlockPos pos = thief.getHideoutPos().orElse(thief.blockPosition());
                thief.level().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                    thief.level(), pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, remainder));
                src.setItem(i, ItemStack.EMPTY);
            }
        }
        chest.setChanged();
    }

    private ItemStack tryInsert(ChestBlockEntity chest, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < chest.getContainerSize() && !remaining.isEmpty(); i++) {
            ItemStack slot = chest.getItem(i);
            if (slot.isEmpty()) {
                chest.setItem(i, remaining.copyAndClear());
                return ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(slot, remaining)) {
                int room = slot.getMaxStackSize() - slot.getCount();
                int moved = Math.min(room, remaining.getCount());
                slot.grow(moved);
                remaining.shrink(moved);
            }
        }
        return remaining;
    }
}

*/

// Empty handler so Bedrock's script engine accepts the file.
export {};
