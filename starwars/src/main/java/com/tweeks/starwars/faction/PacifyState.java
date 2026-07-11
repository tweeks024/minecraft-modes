package com.tweeks.starwars.faction;

/** Pure mind-trick pacification check: active strictly before the expiry tick. */
public final class PacifyState {
    private PacifyState() {}

    public static boolean isActive(long untilTick, long nowTick) {
        return nowTick < untilTick;
    }
}
