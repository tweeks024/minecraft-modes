package com.tweeks.starwars.faction;

/**
 * Marker interface for faction-war participants. The decoupling seam
 * (SecurityHostile pattern): target goals test for this interface, never
 * for concrete entity classes.
 */
public interface SwCombatant {
    SwFaction getFaction();
}
