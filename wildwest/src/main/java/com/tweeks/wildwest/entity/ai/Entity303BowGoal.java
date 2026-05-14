package com.tweeks.wildwest.entity.ai;

import com.tweeks.wildwest.entity.Entity303Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Primary attack: fires an enchanted Arrow at the target every 2.5 s when
 * the target is at distance 5–20 blocks with line-of-sight. Handles the
 * bow-side of the equipment swap (bow in mainhand, sword in offhand).
 */
public class Entity303BowGoal extends Goal {

    private static final int COOLDOWN_TICKS = 50; // 2.5 s
    private static final double MIN_RANGE = 5.0;
    private static final double MAX_RANGE = 20.0;
    private static final float ARROW_VELOCITY = 1.6f;
    private static final float ARROW_INACCURACY = 6.0f;

    private final Entity303Entity boss;
    private int cooldown = 0;

    public Entity303BowGoal(Entity303Entity boss) {
        this.boss = boss;
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.boss.getTarget();
        if (target == null || !target.isAlive()) return false;
        double dist = this.boss.distanceTo(target);
        if (dist < MIN_RANGE || dist > MAX_RANGE) return false;
        return this.boss.hasLineOfSight(target);
    }

    @Override
    public boolean canContinueToUse() { return false; }

    @Override
    public void start() {
        LivingEntity target = this.boss.getTarget();
        if (target == null) return;
        this.cooldown = COOLDOWN_TICKS;

        if (!(this.boss.level() instanceof ServerLevel sl)) return;

        // Ensure bow is in mainhand. Swap with offhand if needed.
        if (!this.boss.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.BOW)) {
            ItemStack main = this.boss.getItemBySlot(EquipmentSlot.MAINHAND);
            ItemStack off = this.boss.getItemBySlot(EquipmentSlot.OFFHAND);
            this.boss.setItemSlot(EquipmentSlot.MAINHAND, off);
            this.boss.setItemSlot(EquipmentSlot.OFFHAND, main);
        }

        ItemStack bowStack = this.boss.getItemBySlot(EquipmentSlot.MAINHAND);
        ItemStack arrowStack = new ItemStack(Items.ARROW);
        Arrow arrow = new Arrow(sl, this.boss, arrowStack, bowStack);

        // Aim at target eye position with slight upward inheritance so the
        // arc lands on the target instead of dipping short at long range.
        double dx = target.getX() - this.boss.getX();
        double dyEye = target.getEyeY() - arrow.getY();
        double dz = target.getZ() - this.boss.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dyEye + horizontalDist * 0.2, dz, ARROW_VELOCITY, ARROW_INACCURACY);

        sl.playSound(null, this.boss.getX(), this.boss.getY(), this.boss.getZ(),
            SoundEvents.SKELETON_SHOOT, SoundSource.HOSTILE,
            1.0f, 1.0f / (this.boss.getRandom().nextFloat() * 0.4f + 0.8f));
        sl.addFreshEntity(arrow);
    }
}
