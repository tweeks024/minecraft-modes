package com.tweeks.starwars.entity;

import com.tweeks.starwars.faction.SwFaction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Rebel trooper: the Rebellion's rank-and-file blaster infantry. Mirrors
 * the stormtrooper wiring on the LIGHT side — {@link SwMob}'s default
 * goals give it the blaster goal (pistol-tier: pistol damage, standard
 * fire interval), cross-faction targeting of EMPIRE combatants, and
 * hurt-by retaliation. CREATURE category with the generic ground spawn
 * rule (like the Jedi Knight).
 */
public class RebelTrooperEntity extends SwMob {

    public static final double MAX_HEALTH = 24.0;
    public static final double MOVEMENT_SPEED = 0.3;

    public RebelTrooperEntity(EntityType<? extends RebelTrooperEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    public SwFaction getFaction() { return SwFaction.LIGHT; }

    @Override
    public boolean usesBlaster() { return true; }

    @Override
    public boolean usesRifleBlaster() { return false; }

    @Override
    protected ItemStack getWeaponStack() {
        return new ItemStack(com.tweeks.starwars.Registration.BLASTER_PISTOL.get());
    }
}
