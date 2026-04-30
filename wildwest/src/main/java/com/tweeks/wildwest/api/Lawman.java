package com.tweeks.wildwest.api;

/**
 * Marker interface for wild-west faction "lawmen" (sheriff, deputy).
 * Used in target-selector predicates so outlaws can identify them as enemies.
 *
 * Empty by design — no methods. Identity check via {@code instanceof Lawman}.
 */
public interface Lawman {
}
