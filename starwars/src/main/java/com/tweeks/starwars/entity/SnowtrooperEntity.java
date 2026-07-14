package com.tweeks.starwars.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Snowtrooper: a stormtrooper issued cold-weather kit. Identical to
 * {@link StormtrooperEntity} in every behavioral respect — same faction,
 * blaster, goals, attributes, and model geometry — except it cannot
 * freeze in powder snow. The visual difference is texture-only (see the
 * snowtrooper renderer).
 */
public class SnowtrooperEntity extends StormtrooperEntity {

    public SnowtrooperEntity(EntityType<? extends SnowtrooperEntity> type, Level level) {
        super(type, level);
    }

    /** Cold-weather armor: immune to freezing / powder-snow damage. */
    @Override
    public boolean canFreeze() {
        return false;
    }
}
