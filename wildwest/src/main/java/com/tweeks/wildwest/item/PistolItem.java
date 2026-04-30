package com.tweeks.wildwest.item;

import com.tweeks.wildwest.Hitscan;
import com.tweeks.wildwest.ModSounds;
import com.tweeks.wildwest.WildWestDamageTypes;
import com.tweeks.wildwest.network.S2CTracerPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PistolItem extends Item {

    public static final double MAX_RANGE = 16.0;
    public static final float DAMAGE = 5.0F;
    public static final int COOLDOWN_TICKS = 8;

    public PistolItem(Properties properties) {
        super(properties.stacksTo(1).durability(300));
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

        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(MAX_RANGE));

        BlockHitResult blockHit = level.clip(new ClipContext(
            start, end,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double blockDist = blockHit.getType() == HitResult.Type.MISS
            ? MAX_RANGE
            : start.distanceTo(blockHit.getLocation());

        List<LivingEntity> nearby = level.getEntitiesOfClass(
            LivingEntity.class,
            new AABB(start, end).inflate(1.0),
            e -> e != player && e.isAlive());

        List<Hitscan.Candidate> candidates = new ArrayList<>();
        Map<String, LivingEntity> byId = new HashMap<>();
        for (LivingEntity e : nearby) {
            var clip = e.getBoundingBox().inflate(0.3).clip(start, end);
            if (clip.isPresent()) {
                String id = e.getUUID().toString();
                candidates.add(new Hitscan.Candidate(id, start.distanceTo(clip.get())));
                byId.put(id, e);
            }
        }

        var hit = Hitscan.firstHitWithinRange(blockDist, candidates);
        Vec3 endPoint = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        if (hit.isPresent()) {
            LivingEntity target = byId.get(hit.get().id());
            target.invulnerableTime = 0;
            target.hurtServer((ServerLevel) level, WildWestDamageTypes.gunshot(player), DAMAGE);
            endPoint = target.position().add(0, target.getBbHeight() * 0.5, 0);
        }

        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            com.tweeks.wildwest.ModSounds.PISTOL_FIRE.get(),
            SoundSource.PLAYERS, 1.0F, 1.0F);

        if (player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                sp, new S2CTracerPacket(start, endPoint));
        }

        return InteractionResult.CONSUME;
    }

    /**
     * Mob-side firing path. Server-side hitscan from shooter's eye toward target,
     * with Gaussian aim inaccuracy so mobs don't pixel-perfect snipe. Sends tracer
     * packet to all players tracking the shooter (NOT including-self — mobs aren't
     * ServerPlayers).
     *
     * No cooldown / durability tracking on the mob. The mob's own AI goal manages
     * its fire-rate timing.
     */
    public static void fireFromMob(LivingEntity shooter, LivingEntity target) {
        Level level = shooter.level();
        if (level.isClientSide()) return;

        Vec3 start = shooter.getEyePosition();
        Vec3 aimAt = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 dir = aimAt.subtract(start).normalize();

        var rand = shooter.getRandom();
        double ax = rand.nextGaussian() * 0.05;
        double ay = rand.nextGaussian() * 0.05;
        double az = rand.nextGaussian() * 0.05;
        dir = new Vec3(dir.x + ax, dir.y + ay, dir.z + az).normalize();

        Vec3 end = start.add(dir.scale(MAX_RANGE));

        BlockHitResult blockHit = level.clip(new ClipContext(
            start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        double blockDist = blockHit.getType() == HitResult.Type.MISS
            ? MAX_RANGE
            : start.distanceTo(blockHit.getLocation());

        List<LivingEntity> nearby = level.getEntitiesOfClass(
            LivingEntity.class,
            new AABB(start, end).inflate(1.0),
            e -> e != shooter && e.isAlive());

        List<Hitscan.Candidate> candidates = new ArrayList<>();
        Map<String, LivingEntity> byId = new HashMap<>();
        for (LivingEntity e : nearby) {
            var clip = e.getBoundingBox().inflate(0.3).clip(start, end);
            if (clip.isPresent()) {
                String id = e.getUUID().toString();
                candidates.add(new Hitscan.Candidate(id, start.distanceTo(clip.get())));
                byId.put(id, e);
            }
        }

        var hit = Hitscan.firstHitWithinRange(blockDist, candidates);
        Vec3 endPoint = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        if (hit.isPresent()) {
            LivingEntity hitTarget = byId.get(hit.get().id());
            hitTarget.invulnerableTime = 0;
            hitTarget.hurtServer((ServerLevel) level,
                WildWestDamageTypes.gunshot(shooter), DAMAGE);
            endPoint = hitTarget.position().add(0, hitTarget.getBbHeight() * 0.5, 0);
        }

        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
            ModSounds.PISTOL_FIRE.get(),
            SoundSource.HOSTILE, 1.0F, 1.0F);

        PacketDistributor.sendToPlayersTrackingEntity(
            shooter, new S2CTracerPacket(start, endPoint));
    }
}
