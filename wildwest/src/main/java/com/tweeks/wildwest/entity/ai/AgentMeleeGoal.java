package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.AgentEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Engages target in melee at distance ≤ 3 blocks. Disengages at distance &gt; 4
 * to prevent thrashing across the threshold. Swaps the iron sword into
 * mainhand on engage so attacks use sword damage + Sharpness.
 */
public class AgentMeleeGoal extends MeleeAttackGoal {

    private static final double ENGAGE_RANGE_SQ = 3.0 * 3.0;
    private static final double DISENGAGE_RANGE_SQ = 4.0 * 4.0;

    private final AgentEntity agent;

    public AgentMeleeGoal(AgentEntity boss) {
        super(boss, 1.0, true);
        this.agent = boss;
    }

    @Override
    public boolean canUse() {
        if (!super.canUse()) return false;
        LivingEntity target = this.mob.getTarget();
        if (target == null) return false;
        return this.mob.distanceToSqr(target) <= ENGAGE_RANGE_SQ;
    }

    @Override
    public boolean canContinueToUse() {
        if (!super.canContinueToUse()) return false;
        LivingEntity target = this.mob.getTarget();
        if (target == null) return false;
        return this.mob.distanceToSqr(target) <= DISENGAGE_RANGE_SQ;
    }

    @Override
    public void start() {
        super.start();
        // Swap iron sword into mainhand if not already there.
        if (!this.agent.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.IRON_SWORD)) {
            ItemStack main = this.agent.getItemBySlot(EquipmentSlot.MAINHAND);
            ItemStack off = this.agent.getItemBySlot(EquipmentSlot.OFFHAND);
            this.agent.setItemSlot(EquipmentSlot.MAINHAND, off);
            this.agent.setItemSlot(EquipmentSlot.OFFHAND, main);
        }
    }
}
