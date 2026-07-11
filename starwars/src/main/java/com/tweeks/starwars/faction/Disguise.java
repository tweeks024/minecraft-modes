package com.tweeks.starwars.faction;

import com.tweeks.starwars.Registration;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * Stormtrooper-armor disguise check. Full set blocks new target ACQUISITION by
 * Empire mobs (StormtrooperEntity, BattleDroidEntity); does not affect mobs
 * already engaged with the player (HurtByTargetGoal unaffected).
 */
public final class Disguise {
    private Disguise() {}

    public static boolean isWearingFullStormtrooperSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(Registration.STORMTROOPER_HELMET.get())
            && entity.getItemBySlot(EquipmentSlot.CHEST).is(Registration.STORMTROOPER_CHESTPLATE.get())
            && entity.getItemBySlot(EquipmentSlot.LEGS).is(Registration.STORMTROOPER_LEGGINGS.get())
            && entity.getItemBySlot(EquipmentSlot.FEET).is(Registration.STORMTROOPER_BOOTS.get());
    }
}
