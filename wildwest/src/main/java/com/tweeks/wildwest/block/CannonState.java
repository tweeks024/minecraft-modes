package com.tweeks.wildwest.block;

/**
 * Pure immutable representation of a cannon block's state. Has a horizontal
 * facing direction and a loaded boolean. NBT-free; the actual block uses
 * Minecraft's BlockState directly (see CannonBlock). This record is here
 * so the state transitions can be unit-tested without booting Minecraft.
 */
public record CannonState(Facing facing, boolean loaded) {

    public enum Facing { NORTH, SOUTH, EAST, WEST }

    public static CannonState unloaded(Facing facing) {
        return new CannonState(facing, false);
    }

    public CannonState loaded(boolean value) {
        return new CannonState(this.facing, value);
    }

    public CannonState withFacing(Facing newFacing) {
        return new CannonState(newFacing, this.loaded);
    }
}
