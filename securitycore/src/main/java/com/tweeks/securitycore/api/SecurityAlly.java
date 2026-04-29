package com.tweeks.securitycore.api;

/**
 * Marker for entities that Security Pack defenders should never target.
 * Reserved for future use (e.g. allied mobs, pet wolves a Guard owner has tamed).
 * The Guard's current target predicate uses class checks for villagers/players;
 * this marker is the opt-in path for cross-mod entities that want to be treated
 * as friendly.
 */
public interface SecurityAlly {
}
