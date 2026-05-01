package com.tweeks.craftee;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Per-tick set-bonus handler for the Craftee armor set. Subscribes to
 * the NeoForge game event bus from {@link CrafteeMod}'s constructor.
 *
 * <p>Three persistent {@link AttributeModifier}s are kept in sync with
 * {@link #isWearingFullSet}:
 * <ol>
 *   <li>{@code MOVEMENT_SPEED}: +20% (multiplicative on base).</li>
 *   <li>{@code JUMP_STRENGTH}: +0.30 (additive).</li>
 *   <li>{@code STEP_HEIGHT}:  +0.50 (additive — clears a full block).</li>
 * </ol>
 *
 * <p>The handler is idempotent: if the desired present/absent state
 * already matches the modifier's actual presence on the attribute, it is
 * a no-op for that tick. See the design spec for why per-tick beats
 * event-driven here, and why no logout/death cleanup is needed.
 */
public final class SetBonusHandler {
    private SetBonusHandler() {}

    private static final Identifier SPEED_ID =
        Identifier.fromNamespaceAndPath(CrafteeMod.MOD_ID, "set_bonus_speed");
    private static final Identifier JUMP_ID =
        Identifier.fromNamespaceAndPath(CrafteeMod.MOD_ID, "set_bonus_jump");
    private static final Identifier STEP_ID =
        Identifier.fromNamespaceAndPath(CrafteeMod.MOD_ID, "set_bonus_step");

    private static final AttributeModifier SPEED_MOD =
        new AttributeModifier(SPEED_ID, 0.20, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    private static final AttributeModifier JUMP_MOD =
        new AttributeModifier(JUMP_ID, 0.30, AttributeModifier.Operation.ADD_VALUE);
    private static final AttributeModifier STEP_MOD =
        new AttributeModifier(STEP_ID, 0.50, AttributeModifier.Operation.ADD_VALUE);

    /** True iff {@code entity} has all four craftee-skin pieces equipped
     *  in the matching armor slots. */
    public static boolean isWearingFullSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(Registration.CRAFTEE_HELMET.get())
            && entity.getItemBySlot(EquipmentSlot.CHEST).is(Registration.CRAFTEE_CHESTPLATE.get())
            && entity.getItemBySlot(EquipmentSlot.LEGS).is(Registration.CRAFTEE_LEGGINGS.get())
            && entity.getItemBySlot(EquipmentSlot.FEET).is(Registration.CRAFTEE_BOOTS.get());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        // Server-only: vanilla syncs attribute modifiers to clients via
        // ClientboundUpdateAttributesPacket. Running this on the logical
        // client too would double-add the modifier to the client-side
        // entity (causing prediction jitter at high speed).
        if (player.level().isClientSide()) return;
        boolean fullSet = isWearingFullSet(player);
        syncModifier(player, Attributes.MOVEMENT_SPEED, SPEED_MOD, fullSet);
        syncModifier(player, Attributes.JUMP_STRENGTH, JUMP_MOD, fullSet);
        syncModifier(player, Attributes.STEP_HEIGHT, STEP_MOD, fullSet);
    }

    private static void syncModifier(LivingEntity entity,
                                     Holder<Attribute> attribute,
                                     AttributeModifier mod,
                                     boolean shouldHave) {
        AttributeInstance inst = entity.getAttribute(attribute);
        if (inst == null) return;
        boolean has = inst.hasModifier(mod.id());
        if (shouldHave && !has) {
            inst.addPermanentModifier(mod);
        } else if (!shouldHave && has) {
            inst.removeModifier(mod.id());
        }
    }
}
