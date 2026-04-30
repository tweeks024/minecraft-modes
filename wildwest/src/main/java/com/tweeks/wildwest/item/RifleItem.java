package com.tweeks.wildwest.item;

import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.ModSounds;
import com.tweeks.wildwest.entity.BulletEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RifleItem extends Item {

    public static final int COOLDOWN_TICKS = 40;
    public static final int BOLT_CYCLE_REMAINING = 30;
    public static final float BULLET_VELOCITY = 6.0F;

    public RifleItem(Properties properties) {
        super(properties.stacksTo(1).durability(400));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        BulletEntity bullet = new BulletEntity(ModEntities.BULLET.get(), level, player);
        bullet.setPos(player.getEyePosition());
        bullet.shoot(player.getLookAngle().x, player.getLookAngle().y, player.getLookAngle().z,
            BULLET_VELOCITY, 0.0F);
        level.addFreshEntity(bullet);

        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            ModSounds.RIFLE_FIRE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        return InteractionResult.CONSUME;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof Player player)) return;
        int remaining = remainingCooldownTicks(player, stack);
        if (remaining == BOLT_CYCLE_REMAINING) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.BOLT_CYCLE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    private static int remainingCooldownTicks(Player player, ItemStack stack) {
        ItemCooldowns cooldowns = player.getCooldowns();
        if (!cooldowns.isOnCooldown(stack)) return 0;
        float percent = cooldowns.getCooldownPercent(stack, 0F);
        return Math.round(percent * (float) COOLDOWN_TICKS);
    }
}
