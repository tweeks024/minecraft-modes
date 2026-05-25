package com.tweeks.wildwest.item;

import com.tweeks.wildwest.WildWestDamageTypes;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Piston Gauntlet — Redstone Golem's signature drop. Right-click casts a
 * short forward ray. If a living entity is hit within range, it takes
 * {@value #HIT_DAMAGE} damage and a {@value #HIT_KNOCKBACK} knockback
 * along the player's look vector. Otherwise (block hit or empty), the
 * gauntlet rocket-jumps the player backward along the look vector at
 * {@value #SELF_LAUNCH_VELOCITY} velocity.
 *
 * <p>{@value #COOLDOWN_TICKS}t (1.5 s) cooldown, {@value #DURABILITY}
 * durability.
 */
public class PistonGauntletItem extends Item {

    public static final int COOLDOWN_TICKS = 30; // 1.5 s
    public static final double RAY_DISTANCE = 4.0;
    public static final float HIT_DAMAGE = 4.0f;
    public static final double HIT_KNOCKBACK = 2.0;
    public static final double SELF_LAUNCH_VELOCITY = 1.5;
    public static final int DURABILITY = 250;

    public PistonGauntletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide()) {
            return InteractionResult.CONSUME;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(RAY_DISTANCE));

        BlockHitResult blockHit = level.clip(new ClipContext(eye, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        AABB rayAabb = player.getBoundingBox()
            .expandTowards(look.scale(RAY_DISTANCE)).inflate(0.5);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
            player, eye, end, rayAabb,
            e -> e != player && e.isAlive() && e instanceof LivingEntity,
            RAY_DISTANCE * RAY_DISTANCE);

        boolean hitEntity = entityHit != null
            && (blockHit.getType() == HitResult.Type.MISS
                || entityHit.getLocation().distanceToSqr(eye)
                    < blockHit.getLocation().distanceToSqr(eye));

        EquipmentSlot slot = hand == InteractionHand.MAIN_HAND
            ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;

        if (hitEntity && entityHit.getEntity() instanceof LivingEntity target) {
            target.hurt(WildWestDamageTypes.pistonPunch(player), HIT_DAMAGE);
            target.knockback(HIT_KNOCKBACK, -look.x, -look.z);
        } else {
            player.setDeltaMovement(player.getDeltaMovement().add(
                -look.x * SELF_LAUNCH_VELOCITY,
                -look.y * SELF_LAUNCH_VELOCITY,
                -look.z * SELF_LAUNCH_VELOCITY));
            if (player instanceof ServerPlayer sp) sp.hurtMarked = true;
        }

        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 8; i++) {
                double t = i / 8.0;
                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    eye.x + look.x * RAY_DISTANCE * t,
                    eye.y + look.y * RAY_DISTANCE * t,
                    eye.z + look.z * RAY_DISTANCE * t,
                    1, 0, 0, 0, 0);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 1.0F, 1.0F);

        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        stack.hurtAndBreak(1, player, slot);
        player.swing(hand);

        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                Item.TooltipContext context,
                                TooltipDisplay display,
                                Consumer<Component> adder,
                                TooltipFlag flag) {
        adder.accept(Component.translatable("item.wildwest.piston_gauntlet.tooltip.use")
            .withStyle(ChatFormatting.GRAY));
        adder.accept(Component.translatable("item.wildwest.piston_gauntlet.tooltip.launch")
            .withStyle(ChatFormatting.GRAY));
    }
}
