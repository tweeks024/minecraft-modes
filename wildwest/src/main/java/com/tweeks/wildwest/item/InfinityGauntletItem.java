package com.tweeks.wildwest.item;

import com.tweeks.wildwest.WildWestDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class InfinityGauntletItem extends Item {

    public static final int DURABILITY = 500;

    public InfinityGauntletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        InfinityStone stone = InfinityStone.byIndex(
            stack.getOrDefault(ModDataComponents.ACTIVE_STONE.get(), 0));

        long[] cds = stack.getOrDefault(
            ModDataComponents.COOLDOWNS.get(), InfinityCooldowns.emptyCooldowns());
        long now = level.getGameTime();
        if (InfinityCooldowns.isOnCooldown(cds, stone.ordinal(), now)) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide()) {
            return InteractionResult.CONSUME;
        }

        boolean success = castStone(stone, (ServerLevel) level, (ServerPlayer) player, stack);
        if (!success) {
            return InteractionResult.PASS;
        }

        long[] nextCds = InfinityCooldowns.applyCooldown(cds, stone.ordinal(), now, stone.cooldownTicks());
        stack.set(ModDataComponents.COOLDOWNS.get(), nextCds);
        player.getCooldowns().addCooldown(stack, stone.cooldownTicks());

        EquipmentSlot slot = hand == InteractionHand.MAIN_HAND
            ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        stack.hurtAndBreak(stone.durabilityCost(), player, slot);
        player.swing(hand);

        return InteractionResult.CONSUME;
    }

    /** Dispatch to the stone's ability. Return false to skip cooldown/durability. */
    private boolean castStone(InfinityStone stone, ServerLevel level, ServerPlayer player, ItemStack stack) {
        return switch (stone) {
            case POWER -> castPower(level, player);
            default -> false;
        };
    }

    private boolean castPower(ServerLevel level, ServerPlayer player) {
        double radius = 5.0;
        AABB area = player.getBoundingBox().inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (target == player) continue;
            if (!(target instanceof Enemy)) continue;
            target.hurt(WildWestDamageTypes.infinityPower(player), 6.0f);
            double dx = target.getX() - player.getX();
            double dz = target.getZ() - player.getZ();
            target.knockback(3.0, -dx, -dz);
        }
        level.sendParticles(ParticleTypes.EXPLOSION,
            player.getX(), player.getY() + 0.5, player.getZ(),
            12, radius * 0.5, 0.5, radius * 0.5, 0.0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.0f);
        return true;
    }
}
