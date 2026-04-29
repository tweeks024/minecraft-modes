package com.tweeks.securityguard.item;

import com.tweeks.securitycore.ai.StunEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The Security Guard's baton, wielded by both Guards (in their off-AI hand
 * via the renderer) and by players who pick one up from the creative tab.
 * Iron-tier weapon; on every successful hit, applies the same Slowness II +
 * Weakness I + knockback bundle that the Guard's AI applies via
 * {@code StunningMeleeGoal}, so player swings and AI swings feel identical.
 *
 * Extends plain {@link Item} (not {@code SwordItem}) because the moddev
 * compile classpath for NeoForge 26.1.2.30-beta strips the {@code SwordItem}
 * / {@code Tier} / {@code Tiers} hierarchy in favor of pure data-component
 * configuration. Iron-tier feel is reproduced via the durability + attribute
 * modifiers attached at registration, plus the manual {@code hurtAndBreak}
 * call below to mirror what {@code SwordItem.postHurtEnemy} would have done.
 */
public class BatonItem extends Item {

    private static final int STUN_DURATION_TICKS = 60;
    private static final int SLOWNESS_AMPLIFIER = 1;
    private static final int WEAKNESS_AMPLIFIER = 0;
    private static final double KNOCKBACK_STRENGTH = 0.2;

    public BatonItem(Properties properties) {
        super(properties);
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.postHurtEnemy(stack, target, attacker);
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        StunEffects.applyStun(attacker, target,
            STUN_DURATION_TICKS, SLOWNESS_AMPLIFIER, WEAKNESS_AMPLIFIER, KNOCKBACK_STRENGTH);
    }
}
