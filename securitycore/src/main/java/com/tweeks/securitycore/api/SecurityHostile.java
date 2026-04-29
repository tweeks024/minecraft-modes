package com.tweeks.securitycore.api;

/**
 * Marker for entities that Security Pack defenders (Guards) should attack on sight.
 * Implementing this interface opts an entity into Guard targeting without requiring
 * it to also implement the vanilla {@link net.minecraft.world.entity.monster.Enemy}
 * interface — useful for mobs that should be hostile only to Guards (not to other
 * vanilla aggressive systems like Iron Golems' default behavior).
 */
public interface SecurityHostile {
}
