package com.tweeks.wildwest.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Right-click ranged tool-wrecker dropped by The Agent. Raycasts up to
 * 8 blocks for a {@link LivingEntity}; on hit, damages a random non-empty
 * equipment slot's item by 50 durability. 16 uses before crumbling.
 *
 * <p>Block-hit / air = no-op (no use consumed, no cooldown).
 */
public class CursedTomeItem extends Item {

    public static final int MAX_USES = 16;
    public static final int DURABILITY_DAMAGE = 50;
    public static final int COOLDOWN_TICKS = 100; // 5 s
    public static final double RAYCAST_RANGE = 8.0;

    public CursedTomeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack tome = player.getItemInHand(hand);
        // Defer fully to the server's decision. The server-side branch below
        // calls player.swing(hand) on a hit (which packets the animation back
        // to the client). A speculative CONSUME here would show the swing
        // animation on the client even when the server takes the no-op miss
        // path, desyncing visuals from durability/cooldown.
        if (!(level instanceof ServerLevel sl)) return InteractionResult.PASS;

        // Raycast for a LivingEntity along the look vector.
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.x * RAYCAST_RANGE, look.y * RAYCAST_RANGE, look.z * RAYCAST_RANGE);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
            player, eye, end,
            player.getBoundingBox().expandTowards(look.scale(RAYCAST_RANGE)).inflate(1.0),
            e -> e instanceof LivingEntity && e != player && e.isAlive(),
            RAYCAST_RANGE);

        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
            // Miss — no consume, no cooldown.
            return InteractionResult.PASS;
        }

        // Build the set of populated, damageable slots on the target.
        EnumSet<EquipmentSlot> candidates = EnumSet.noneOf(EquipmentSlot.class);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack equipped = target.getItemBySlot(slot);
            if (!equipped.isEmpty() && equipped.isDamageableItem()) {
                candidates.add(slot);
            }
        }

        CursedTomeSlotPicker.IntRng rng = sl.getRandom()::nextInt;
        EquipmentSlot picked = CursedTomeSlotPicker.pick(candidates, rng);

        if (picked != null) {
            ItemStack victim = target.getItemBySlot(picked);
            victim.hurtAndBreak(DURABILITY_DAMAGE, target, picked);
        }

        // Visual + audio feedback regardless of whether the target had a damageable item
        // (the use still consumes — player got their on-hit, the target just had nothing
        // to break).
        sl.sendParticles(ParticleTypes.SOUL,
            target.getX(), target.getY() + 1.0, target.getZ(),
            24, 0.5, 0.5, 0.5, 0.02);
        sl.playSound(null, target.getX(), target.getY(), target.getZ(),
            SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6f, 0.7f);

        // Consume one use of the tome.
        tome.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        player.getCooldowns().addCooldown(tome, COOLDOWN_TICKS);
        player.swing(hand);
        return InteractionResult.CONSUME;
    }
}
