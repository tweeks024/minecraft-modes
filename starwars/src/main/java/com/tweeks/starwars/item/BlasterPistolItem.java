package com.tweeks.starwars.item;

import com.tweeks.starwars.Hitscan;
import com.tweeks.starwars.ModSounds;
import com.tweeks.starwars.StarWarsDamageTypes;
import com.tweeks.starwars.entity.ai.QuickdrawState;
import com.tweeks.starwars.faction.ScoundrelLuck;
import com.tweeks.starwars.network.S2CBlasterTracerPacket;
import net.minecraft.core.particles.ParticleTypes;
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

/**
 * Hitscan blaster weapon. Supports armor set bonus (Scoundrel's Luck) that
 * doubles the first shot against newly acquired targets when wearing full Han Solo set.
 */
public class BlasterPistolItem extends Item {

    public static final double MAX_RANGE = 20.0;
    public static final float DAMAGE = 5.0F;
    public static final int COOLDOWN_TICKS = 10;

    public BlasterPistolItem(Properties properties) {
        super(properties.stacksTo(1).durability(450));
    }

    /**
     * Override to give a subclass a different per-shot damage value while
     * reusing all of the hitscan + tracer + sound logic. Default returns
     * {@link #DAMAGE}.
     */
    public float getDamage() {
        return DAMAGE;
    }

    /**
     * Override to give a subclass a different fire-rate cooldown while
     * reusing all of the hitscan + tracer + sound logic. Default returns
     * {@link #COOLDOWN_TICKS}.
     */
    public int getCooldownTicks() {
        return COOLDOWN_TICKS;
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

        // Mirror vanilla projectile filtering: only hittable entities are
        // candidates. canBeHitByProjectile() folds in isAlive() && isPickable();
        // skip spectators and anything invulnerable to a blaster bolt so the
        // shot passes through them instead of silently "hitting" a no-op target.
        ServerLevel serverLevel = (ServerLevel) level;
        var blasterSource = StarWarsDamageTypes.blasterBolt(player);
        List<LivingEntity> nearby = level.getEntitiesOfClass(
            LivingEntity.class,
            new AABB(start, end).inflate(1.0),
            e -> e != player && e.canBeHitByProjectile() && !e.isSpectator()
                && !e.isInvulnerableTo(serverLevel, blasterSource));

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
            float damage = this.getDamage();
            // Scoundrel's Luck: a full Han Solo set doubles the first shot
            // against each newly acquired target — mark only on a landed
            // hit (misses never consume the ambush). See ScoundrelLuck.
            if (ScoundrelLuck.isWearingFullHanSoloSet(player)) {
                QuickdrawState state = ScoundrelLuck.stateFor(player.getUUID());
                if (state.canAmbush(target.getUUID())) {
                    damage = 2 * this.getDamage();
                    state.markAmbushed(target.getUUID());
                    serverLevel.sendParticles(ParticleTypes.CRIT,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        8, 0.3, 0.3, 0.3, 0.1);
                }
            }
            target.hurtServer(serverLevel, blasterSource, damage);
            endPoint = target.position().add(0, target.getBbHeight() * 0.5, 0);
        }

        EquipmentSlot slot = hand == InteractionHand.MAIN_HAND
            ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        stack.hurtAndBreak(1, player, slot);
        player.getCooldowns().addCooldown(stack, getCooldownTicks());
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            ModSounds.BLASTER_FIRE.get(),
            SoundSource.PLAYERS, 1.0F, 1.0F);

        if (player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                sp, new S2CBlasterTracerPacket(start, endPoint, S2CBlasterTracerPacket.COLOR_LIGHT));
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
        fireFromMob(shooter, target, DAMAGE, S2CBlasterTracerPacket.COLOR_EMPIRE);
    }

    /**
     * A bolt from a mob passes harmlessly through the shooter's own side:
     * its tame owner, and any same-(non-neutral)-faction combatant. Without
     * this, a bolt hits whatever the aim ray reaches first — a companion
     * shooting past its owner would hit the owner, and troopers firing
     * through a squadmate would trigger Empire-vs-Empire infighting.
     */
    private static boolean isFriendlyFire(LivingEntity shooter, LivingEntity candidate) {
        if (shooter instanceof net.minecraft.world.entity.TamableAnimal pet && candidate == pet.getOwner()) {
            return true;
        }
        if (shooter instanceof com.tweeks.starwars.faction.SwCombatant a
            && candidate instanceof com.tweeks.starwars.faction.SwCombatant b) {
            var faction = a.getFaction();
            return faction != com.tweeks.starwars.faction.SwFaction.NEUTRAL && faction == b.getFaction();
        }
        return false;
    }

    public static void fireFromMob(LivingEntity shooter, LivingEntity target,
                                   float damage, int tracerColor) {
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

        // Same vanilla-projectile candidate filter as the player path: only
        // hittable, non-spectator, non-invulnerable entities are candidates.
        ServerLevel serverLevel = (ServerLevel) level;
        var blasterSource = StarWarsDamageTypes.blasterBolt(shooter);
        List<LivingEntity> nearby = level.getEntitiesOfClass(
            LivingEntity.class,
            new AABB(start, end).inflate(1.0),
            e -> e != shooter && e.canBeHitByProjectile() && !e.isSpectator()
                && !e.isInvulnerableTo(serverLevel, blasterSource)
                && !isFriendlyFire(shooter, e));

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
            hitTarget.hurtServer(serverLevel, blasterSource, damage);
            endPoint = hitTarget.position().add(0, hitTarget.getBbHeight() * 0.5, 0);
        }

        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
            ModSounds.BLASTER_FIRE.get(),
            SoundSource.HOSTILE, 1.0F, 1.0F);

        PacketDistributor.sendToPlayersTrackingEntity(
            shooter, new S2CBlasterTracerPacket(start, endPoint, tracerColor));
    }
}
