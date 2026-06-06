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
    public net.minecraft.network.chat.Component getName(ItemStack stack) {
        InfinityStone stone = InfinityStone.byIndex(
            stack.getOrDefault(ModDataComponents.ACTIVE_STONE.get(), 0));
        return net.minecraft.network.chat.Component.translatable(
            "item.wildwest.infinity_gauntlet.named",
            net.minecraft.network.chat.Component.translatable(
                "item.wildwest.infinity_gauntlet.stone." + stone.translationSuffix()));
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<net.minecraft.network.chat.Component> adder,
                                net.minecraft.world.item.TooltipFlag flag) {
        InfinityStone stone = InfinityStone.byIndex(
            stack.getOrDefault(ModDataComponents.ACTIVE_STONE.get(), 0));
        adder.accept(net.minecraft.network.chat.Component.translatable(
            "item.wildwest.infinity_gauntlet.tooltip." + stone.translationSuffix())
            .withStyle(net.minecraft.ChatFormatting.GRAY));
        adder.accept(net.minecraft.network.chat.Component.translatable(
            "item.wildwest.infinity_gauntlet.tooltip.swap")
            .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
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
            // Without this client-side cue, FAIL produces no swing/sound/sweep
            // and the player thinks the item is broken. A short low-volume
            // dispenser-empty click is the vanilla "this is on cooldown" idiom.
            if (level.isClientSide()) {
                level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.5f, 1.5f, false);
            }
            return InteractionResult.FAIL;
        }

        if (level.isClientSide()) {
            return InteractionResult.CONSUME;
        }

        // Custom command overrides built-in ability if non-empty. Command is
        // executed at the player's command source (their permission level,
        // their position) — same semantics as a command block running as its
        // placer.
        String customCommand = InfinityCommands.get(
            stack.getOrDefault(ModDataComponents.COMMANDS.get(), InfinityCommands.empty()),
            stone.ordinal());
        boolean success;
        if (!customCommand.isEmpty()) {
            success = castCommand((ServerLevel) level, (ServerPlayer) player, stone, customCommand);
        } else {
            success = castStone(stone, (ServerLevel) level, (ServerPlayer) player, stack);
        }
        if (!success) {
            return InteractionResult.PASS;
        }

        long[] nextCds = InfinityCooldowns.applyCooldown(cds, stone.ordinal(), now, stone.cooldownTicks());
        stack.set(ModDataComponents.COOLDOWNS.get(), nextCds);

        // Drive the vanilla hotbar cooldown sweep for the *active* stone.
        // Vanilla ItemCooldowns is per-item, but here that's intentional:
        // we want the sweep to reflect the currently-displayed stone's
        // cooldown. When the player swaps stones via the radial picker,
        // the packet handler re-syncs this to the newly-active stone
        // (see C2SSetActiveStonePacket.handle).
        player.getCooldowns().addCooldown(stack, stone.cooldownTicks());

        EquipmentSlot slot = hand == InteractionHand.MAIN_HAND
            ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        stack.hurtAndBreak(stone.durabilityCost(), player, slot);
        player.swing(hand);

        return InteractionResult.CONSUME;
    }

    /**
     * Dispatch to the stone's ability. Return false to skip cooldown/durability.
     *
     * <p>The switch is exhaustive over {@link InfinityStone}. If a future
     * stone is added without wiring a case here, the compiler will warn
     * (no default branch covers it) — better to fail loudly at compile
     * time than ship a stone that silently does nothing.
     */
    private boolean castStone(InfinityStone stone, ServerLevel level, ServerPlayer player, ItemStack stack) {
        return switch (stone) {
            case POWER -> castPower(level, player);
            case SPACE -> castSpace(level, player);
            case TIME -> castTime(level, player);
            case SOUL -> castSoul(level, player);
            case MIND -> castMind(level, player);
            case REALITY -> castReality(level, player);
        };
    }

    /**
     * Per-thread re-entrancy guard. If a player-authored command somehow
     * re-invokes {@link #use} on the same gauntlet (e.g. via /trigger or
     * /execute as ... run ... that scripts the player swinging), we
     * short-circuit the inner cast to bound recursion at depth 1. Without
     * this, a hand-crafted loop could blow the server stack.
     */
    private static final ThreadLocal<Boolean> CASTING_COMMAND = ThreadLocal.withInitial(() -> false);

    /**
     * Run an arbitrary command at the player's command source. The command
     * source uses the player's permission level (op vs. non-op), so a
     * survival player can't escalate to op commands via the gauntlet.
     *
     * <p>Output is suppressed via {@link
     * net.minecraft.commands.CommandSourceStack#withSuppressedOutput} so
     * the player's chat isn't spammed with /effect-style feedback on every
     * cast.
     *
     * <p>Returns {@code true} whether the command parsed/succeeded or not:
     * the cast attempt still consumes the cooldown and durability. This
     * mirrors how a command block ticks whether or not its command parses,
     * and stops players from spamming a typo-loaded command with no cost.
     */
    private boolean castCommand(ServerLevel level, ServerPlayer player,
                                InfinityStone stone, String command) {
        if (CASTING_COMMAND.get()) {
            // Re-entry from a player-authored command. Refuse and drop
            // through; the outer cast will still consume the cooldown.
            return true;
        }
        net.minecraft.server.MinecraftServer server = level.getServer();
        if (server == null) return false;
        net.minecraft.commands.CommandSourceStack source =
            player.createCommandSourceStack().withSuppressedOutput();
        CASTING_COMMAND.set(true);
        try {
            server.getCommands().performPrefixedCommand(source, command);
        } catch (Exception ex) {
            // Surface failures to the player but still apply cooldown — the
            // gauntlet "tried" the cast.
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "item.wildwest.infinity_gauntlet.command_failed",
                ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage())
                .withStyle(net.minecraft.ChatFormatting.RED));
        } finally {
            CASTING_COMMAND.set(false);
        }
        // Generic spell-cast feedback so custom commands feel like a cast.
        level.sendParticles(
            new net.minecraft.core.particles.DustParticleOptions(stone.colorRgb() & 0xFFFFFF, 1.5f),
            player.getX(), player.getY() + 1.0, player.getZ(),
            16, 0.4, 0.6, 0.4, 0.05);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6f, 1.4f);
        return true;
    }

    private boolean castReality(ServerLevel level, ServerPlayer player) {
        double maxDist = 8.0;
        net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        net.minecraft.world.phys.Vec3 end = eye.add(look.scale(maxDist));
        AABB rayAabb = player.getBoundingBox().expandTowards(look.scale(maxDist)).inflate(0.5);

        net.minecraft.world.phys.EntityHitResult hit =
            net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player, eye, end, rayAabb,
                e -> e != player && e.isAlive()
                    && e instanceof net.minecraft.world.entity.Mob
                    && e instanceof Enemy,
                maxDist * maxDist);

        if (hit == null) return false;

        net.minecraft.world.entity.Mob original = (net.minecraft.world.entity.Mob) hit.getEntity();

        String typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
            .getKey(original.getType()).toString();

        net.minecraft.world.entity.ambient.Bat bat =
            net.minecraft.world.entity.EntityType.BAT.create(level,
                net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (bat == null) return false;
        bat.snapTo(original.getX(), original.getY(), original.getZ(), original.getYRot(), 0.0f);
        bat.setData(com.tweeks.wildwest.effect.ModAttachments.REALITY_BUBBLE.get(),
            new com.tweeks.wildwest.effect.RealityBubbleAttachment(typeId, level.getGameTime() + 1200));
        // Only discard the original if the bat actually spawned. addFreshEntity
        // can refuse the entity (e.g. spawn event cancelled) — without this
        // check we'd silently delete the targeted hostile and leave the
        // player with no bat replacement.
        if (!level.addFreshEntity(bat)) {
            bat.discard();
            return false;
        }
        original.discard();

        level.sendParticles(ParticleTypes.DUST_PLUME,
            bat.getX(), bat.getY() + 0.5, bat.getZ(),
            30, 0.5, 0.5, 0.5, 0.05);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0f, 1.0f);
        return true;
    }

    private boolean castMind(ServerLevel level, ServerPlayer player) {
        double maxDist = 8.0;
        net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        net.minecraft.world.phys.Vec3 end = eye.add(look.scale(maxDist));
        AABB rayAabb = player.getBoundingBox().expandTowards(look.scale(maxDist)).inflate(0.5);

        net.minecraft.world.phys.EntityHitResult hit =
            net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player, eye, end, rayAabb,
                e -> e != player && e.isAlive() && e instanceof net.minecraft.world.entity.Mob,
                maxDist * maxDist);

        if (hit == null) return false;

        net.minecraft.world.entity.Mob target = (net.minecraft.world.entity.Mob) hit.getEntity();
        long expiry = level.getGameTime() + 300;
        target.setData(com.tweeks.wildwest.effect.ModAttachments.MIND_CHARM.get(),
            new com.tweeks.wildwest.effect.MindCharmAttachment(player.getUUID(), expiry));

        level.sendParticles(ParticleTypes.ENCHANT,
            target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
            30, 0.3, 0.5, 0.3, 0.5);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.PLAYERS, 1.0f, 1.2f);
        return true;
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

        // Clearance check: refuse to drop the player inside a solid block. If
        // the first candidate fails, pull back along the look vector up to
        // 3 times before giving up. Without this, a wall-bounce shot can
        // suffocate the player on landing. Cooldown is NOT consumed on
        // failure (castStone treats `false` as a no-op).
        net.minecraft.world.phys.Vec3 safeTarget = null;
        for (int attempt = 0; attempt < 4; attempt++) {
            net.minecraft.world.phys.Vec3 candidate = attempt == 0
                ? target
                : target.subtract(look.scale(attempt));
            if (level.noCollision(player, player.getBoundingBox().move(
                    candidate.x - player.getX(),
                    candidate.y - player.getY(),
                    candidate.z - player.getZ()))) {
                safeTarget = candidate;
                break;
            }
        }
        if (safeTarget == null) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.3f, 0.5f);
            return false;
        }

        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
            player.getX(), player.getY() + 1.0, player.getZ(),
            20, 0.3, 0.5, 0.3, 0.01);

        player.teleportTo(safeTarget.x, safeTarget.y, safeTarget.z);
        player.fallDistance = 0.0f;

        level.sendParticles(ParticleTypes.REVERSE_PORTAL,
            safeTarget.x, safeTarget.y + 1.0, safeTarget.z,
            20, 0.3, 0.5, 0.3, 0.01);
        level.playSound(null, safeTarget.x, safeTarget.y, safeTarget.z,
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
