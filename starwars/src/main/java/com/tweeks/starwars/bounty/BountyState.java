package com.tweeks.starwars.bounty;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A player's active bounty, stored as an attachment. {@code remaining == 0}
 * means the target is eliminated and the player can collect their reward at
 * any terminal.
 */
public record BountyState(String targetId, int total, int remaining, int reward) {
    public static final MapCodec<BountyState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.STRING.fieldOf("Target").forGetter(BountyState::targetId),
        Codec.INT.fieldOf("Total").forGetter(BountyState::total),
        Codec.INT.fieldOf("Remaining").forGetter(BountyState::remaining),
        Codec.INT.fieldOf("Reward").forGetter(BountyState::reward)
    ).apply(i, BountyState::new));

    public BountyState withRemaining(int remaining) {
        return new BountyState(targetId, total, Math.max(0, remaining), reward);
    }

    public boolean complete() {
        return remaining <= 0;
    }

    public int killed() {
        return total - remaining;
    }
}
