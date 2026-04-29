package com.tweeks.creeperskin;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 4-piece set-bonus handler for the Creeper Skin armor set. Subscribes to
 * the NeoForge game event bus from {@link CreeperSkinMod}'s constructor.
 *
 * <p>Two bonuses, both gated on {@link #isWearingFullSet} returning true:
 * <ol>
 *   <li>Real {@link Creeper} mobs cannot target the wearer — handled by
 *       {@link #onCreeperTargetChange(LivingChangeTargetEvent)}.</li>
 *   <li>Any creeper-tagged explosion deals zero damage to the wearer —
 *       handled by {@link #onIncomingExplosionDamage(LivingIncomingDamageEvent)}.</li>
 * </ol>
 *
 * <p>Single source of truth for "wearing the full set" is
 * {@link #isWearingFullSet}; both bonus paths call it.
 */
public final class SetBonusHandler {
    private SetBonusHandler() {}

    /** True iff {@code entity} has all four creeper-skin pieces equipped
     *  in the matching armor slots. */
    public static boolean isWearingFullSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(Registration.CREEPER_HELMET.get())
            && entity.getItemBySlot(EquipmentSlot.CHEST).is(Registration.CREEPER_CHESTPLATE.get())
            && entity.getItemBySlot(EquipmentSlot.LEGS).is(Registration.CREEPER_LEGGINGS.get())
            && entity.getItemBySlot(EquipmentSlot.FEET).is(Registration.CREEPER_BOOTS.get());
    }

    /** Bonus 1: a Creeper trying to acquire a full-set wearer as a target
     *  has the new target set to {@code null}, so its targeting goal
     *  fails and it never starts fusing. */
    @SubscribeEvent
    public static void onCreeperTargetChange(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Creeper)) return;
        LivingEntity newTarget = event.getNewAboutToBeSetTarget();
        if (newTarget != null && isWearingFullSet(newTarget)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    /** Bonus 2: incoming damage to a full-set wearer from any creeper
     *  source tagged as an explosion is canceled (zero damage applied).
     *  Other side-effects of the blast (knockback, world block damage)
     *  are unaffected. */
    @SubscribeEvent
    public static void onIncomingExplosionDamage(LivingIncomingDamageEvent event) {
        DamageSource src = event.getSource();
        if (!src.is(DamageTypeTags.IS_EXPLOSION)) return;
        if (!(src.getEntity() instanceof Creeper)) return;
        if (isWearingFullSet(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
