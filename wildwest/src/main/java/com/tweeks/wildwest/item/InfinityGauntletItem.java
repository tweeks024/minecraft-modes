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
            case SPACE -> castSpace(level, player);
            case TIME -> castTime(level, player);
            case SOUL -> castSoul(level, player);
            default -> false;
        };
    }

    private boolean castSoul(ServerLevel level, ServerPlayer player) {
        double maxDist = 16.0;
        net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        net.minecraft.world.phys.Vec3 end = eye.add(look.scale(maxDist));
        AABB rayAabb = player.getBoundingBox().expandTowards(look.scale(maxDist)).inflate(0.5);

        net.minecraft.world.phys.EntityHitResult hit =
            net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player, eye, end, rayAabb,
                e -> e != player && e.isAlive() && e instanceof LivingEntity,
                maxDist * maxDist);

        if (hit == null) return false;

        LivingEntity target = (LivingEntity) hit.getEntity();
        target.hurt(WildWestDamageTypes.infinitySoul(player), 4.0f);
        player.heal(4.0f);

        for (int i = 0; i <= 16; i++) {
            double t = i / 16.0;
            level.sendParticles(ParticleTypes.SOUL,
                eye.x + (target.getX() - eye.x) * t,
                eye.y + (target.getY() + target.getBbHeight() / 2 - eye.y) * t,
                eye.z + (target.getZ() - eye.z) * t,
                1, 0, 0, 0, 0);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.SOUL_SAND_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
        return true;
    }

    private boolean castTime(ServerLevel level, ServerPlayer player) {
        double radius = 6.0;
        AABB area = player.getBoundingBox().inflate(radius);
        int durationTicks = 160;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (target == player) continue;
            if (!(target instanceof Enemy)) continue;
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.SLOWNESS, durationTicks, 3));
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MINING_FATIGUE, durationTicks, 2));
        }
        level.sendParticles(ParticleTypes.GLOW,
            player.getX(), player.getY() + 1.0, player.getZ(),
            40, radius * 0.5, 0.5, radius * 0.5, 0.0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1.0f, 1.5f);
        return true;
    }

    private boolean castSpace(ServerLevel level, ServerPlayer player) {
        double maxDist = 32.0;
        net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        net.minecraft.world.phys.Vec3 end = eye.add(look.scale(maxDist));

        net.minecraft.world.phys.BlockHitResult hit = level.clip(
            new net.minecraft.world.level.ClipContext(eye, end,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));

        net.minecraft.world.phys.Vec3 target;
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
            target = end;
        } else {
            target = hit.getLocation().subtract(look.scale(1.0));
        }

        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
            player.getX(), player.getY() + 1.0, player.getZ(),
            20, 0.3, 0.5, 0.3, 0.01);

        player.teleportTo(target.x, target.y, target.z);
        player.fallDistance = 0.0f;

        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
            target.x, target.y + 1.0, target.z,
            20, 0.3, 0.5, 0.3, 0.01);
        level.playSound(null, target.x, target.y, target.z,
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        return true;
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
