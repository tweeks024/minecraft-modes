package com.tweeks.wildwest;

import com.tweeks.wildwest.effect.HandSnapshot;
import com.tweeks.wildwest.effect.ModEffects;
import com.tweeks.wildwest.entity.ai.zombified.InfectionImmunity;
import com.tweeks.wildwest.entity.ai.zombified.ZombifiedHostileTargetGoal;
import com.tweeks.wildwest.entity.ai.zombified.ZombifiedMeleeAttackGoal;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Central server-side wiring for the zombie-virus mechanic. Listens to a
 * handful of NeoForge events to drive the FESTERING_WOUND -> ZOMBIFIED ->
 * (cure) state machine, disarm/restore zombified mobs' hand items, handle
 * golden-apple cure interactions, and ensure snapshotted hand items drop
 * on death.
 *
 * <p>Auto-registered on the GAME bus via {@link EventBusSubscriber}, mirroring
 * the {@code BanditLeaderPackSpawner} pattern.
 *
 * <p>API notes (NeoForge 26.1.2):
 * <ul>
 *   <li>{@code MobEffectEvent.Added} / {@code .Remove} / {@code .Expired} are
 *       nested static classes; {@code Remove#getEffect()} returns a
 *       {@code Holder<MobEffect>}, while the others expose
 *       {@code getEffectInstance()}.</li>
 *   <li>{@code LivingDamageEvent.Pre} is the cancellable pre-damage event
 *       used here; multiple {@code @SubscribeEvent} listeners against the
 *       same event class fire independently.</li>
 *   <li>{@code MobEffectInstance.is(Holder<MobEffect>)} accepts our
 *       {@code DeferredHolder<MobEffect, MobEffect>} directly.</li>
 *   <li>{@code MobEffectInstance(Holder, duration, amplifier, ambient,
 *       visible)} is the 5-arg constructor used here.</li>
 * </ul>
 */
@EventBusSubscriber(modid = WildWestMod.MOD_ID)
public final class ZombieVirusHandler {
    private ZombieVirusHandler() {}

    private static final int FESTERING_DURATION_TICKS = 60 * 20;
    private static final int WITHER_DURATION_TICKS = 5 * 20;
    private static final int CURING_SHAKE_DURATION_TICKS = 30 * 20;
    private static final double WITHER_CHANCE = 0.10;

    private static final String KEY_MAIN = "wildwest:pre_zombified_mainhand";
    private static final String KEY_OFF  = "wildwest:pre_zombified_offhand";

    /**
     * Bite spread: a zombified attacker hitting a non-immune target via direct
     * melee applies FESTERING_WOUND (skipped if already ZOMBIFIED) and rolls
     * a 10% chance to also apply 5s of vanilla WITHER. Projectiles and indirect
     * sources are excluded by the {@code getDirectEntity() == getEntity()}
     * guard.
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();
        var source = event.getSource();
        if (!(source.getEntity() instanceof LivingEntity attacker)) return;
        // Direct-hit guard: melee only, not arrows / llama-spit / etc.
        if (source.getDirectEntity() != source.getEntity()) return;
        if (!attacker.hasEffect(ModEffects.ZOMBIFIED)) return;
        if (target.level().isClientSide()) return;
        if (InfectionImmunity.isImmune(target)) return;

        // Festering apply / refresh — skip if target is already turned.
        if (!target.hasEffect(ModEffects.ZOMBIFIED)) {
            target.addEffect(new MobEffectInstance(ModEffects.FESTERING_WOUND,
                FESTERING_DURATION_TICKS, 0, false, true));
        }

        // 10 % wither roll, independent of the festering check.
        if (target.getRandom().nextDouble() < WITHER_CHANCE) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER,
                WITHER_DURATION_TICKS, 0, false, true));
        }
    }

    /**
     * When ZOMBIFIED is applied to a non-Player Mob, snapshot its hand items
     * into persistent NBT and clear the slots. Players keep their items.
     */
    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        var inst = event.getEffectInstance();
        if (inst == null || !inst.is(ModEffects.ZOMBIFIED)) return;
        LivingEntity entity = event.getEntity();
        if (entity instanceof Mob mob && !(entity instanceof Player)) {
            HandSnapshot.snapshotAndClear(mob);
        }
    }

    /**
     * When ZOMBIFIED is removed from a non-Player Mob, restore previously
     * snapshotted hand items. Covers both cure paths (CURING_SHAKE expiry)
     * and any future direct {@code removeEffect} calls.
     */
    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        // is(Holder) is deprecated; compare via the ResourceKey from our DeferredHolder.
        if (!event.getEffect().is(ModEffects.ZOMBIFIED.getKey())) return;
        LivingEntity entity = event.getEntity();
        if (entity instanceof Mob mob && !(entity instanceof Player)) {
            HandSnapshot.restore(mob);
        }
    }

    /**
     * Effect-expiry transitions:
     * <ul>
     *   <li>FESTERING_WOUND expired -> apply infinite ZOMBIFIED (the disarm
     *       happens via {@link #onEffectAdded}).</li>
     *   <li>CURING_SHAKE expired -> remove ZOMBIFIED (restore happens via
     *       {@link #onEffectRemoved}).</li>
     *   <li>ZOMBIFIED expired -> intentionally a no-op here; the
     *       {@link #onEffectRemoved} listener handles restore.</li>
     * </ul>
     */
    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        LivingEntity entity = event.getEntity();
        var inst = event.getEffectInstance();
        if (inst == null) return;

        if (inst.is(ModEffects.FESTERING_WOUND)) {
            entity.addEffect(new MobEffectInstance(ModEffects.ZOMBIFIED,
                -1 /* infinite */, 0, false, true));
        } else if (inst.is(ModEffects.CURING_SHAKE)) {
            entity.removeEffect(ModEffects.ZOMBIFIED);
        }
        // ZOMBIFIED expired path is handled by onEffectRemoved (HandSnapshot.restore).
    }

    /**
     * Independent {@link LivingDamageEvent.Pre} listener that cancels an
     * in-progress cure: if a CURING_SHAKE entity is hit by a Player, drop
     * the cure effect (no automatic re-zombify; the entity is still ZOMBIFIED
     * and stays that way).
     */
    @SubscribeEvent
    public static void onCureInterrupt(LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();
        if (!target.hasEffect(ModEffects.CURING_SHAKE)) return;
        if (event.getSource().getEntity() instanceof Player) {
            target.removeEffect(ModEffects.CURING_SHAKE);
        }
    }

    /**
     * Golden-apple right-click on an entity:
     * <ul>
     *   <li>Has FESTERING_WOUND -> instant cure (remove the effect).</li>
     *   <li>Has ZOMBIFIED but not CURING_SHAKE -> start a 30s CURING_SHAKE.</li>
     *   <li>Otherwise -> no-op, vanilla behavior.</li>
     * </ul>
     *
     * <p>Both client and server cancel the event with {@code SUCCESS} so the
     * client doesn't try to right-click-eat the apple and visual state stays
     * synced. Only the server actually mutates effects and shrinks the stack.
     */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getItemStack().getItem() != Items.GOLDEN_APPLE) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;

        boolean willCureFestering = target.hasEffect(ModEffects.FESTERING_WOUND);
        boolean willStartShake = !willCureFestering
            && target.hasEffect(ModEffects.ZOMBIFIED)
            && !target.hasEffect(ModEffects.CURING_SHAKE);

        if (!willCureFestering && !willStartShake) return;

        // Both sides cancel the event so the client doesn't try to
        // right-click-eat the apple and so client/server visual state stays
        // aligned. CRITICAL: do NOT gate this whole method on isClientSide() —
        // doing so causes ghost-item desync where the client thinks it ate
        // the apple but the server still has it.
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);

        // Server-only mutations.
        if (event.getLevel().isClientSide()) return;

        if (willCureFestering) {
            target.removeEffect(ModEffects.FESTERING_WOUND);
        } else {
            target.addEffect(new MobEffectInstance(ModEffects.CURING_SHAKE,
                CURING_SHAKE_DURATION_TICKS, 0, false, false));
        }
        if (!event.getEntity().getAbilities().instabuild) {
            event.getItemStack().shrink(1);
        }
    }

    /**
     * On entity join, inject the zombified AI goals into every server-side Mob
     * (excluding Players). The goals gate themselves on {@code hasEffect(ZOMBIFIED)}
     * at runtime, so they are effectively dormant until a mob turns.
     *
     * <ul>
     *   <li>{@link ZombifiedHostileTargetGoal} at priority 0 in
     *       {@code targetSelector} — targets any non-immune, non-zombified
     *       living entity when the mob is ZOMBIFIED and not curing.</li>
     *   <li>{@link ZombifiedMeleeAttackGoal} at priority 1 in
     *       {@code goalSelector} — PathfinderMob only (covers most mobs);
     *       provides chase + swing logic while ZOMBIFIED.</li>
     * </ul>
     */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        // Player is not a Mob in this version, so the instanceof guard above already excludes players.

        // Always-on target goal at high priority — gates on ZOMBIFIED at runtime.
        mob.targetSelector.addGoal(0, new ZombifiedHostileTargetGoal(mob));

        // Melee attack goal — only inject for PathfinderMob (which is most things).
        if (mob instanceof net.minecraft.world.entity.PathfinderMob pm) {
            mob.goalSelector.addGoal(1, new ZombifiedMeleeAttackGoal(pm));
        }
    }

    /**
     * If a dying Mob still has snapshotted hand items in persistent NBT (e.g.
     * a zombified skeleton's bow, or picked-up player gear), drop them as
     * ItemEntities. Without this, those items would be permanently deleted
     * since the live equipment slots were cleared at zombification time.
     *
     * <p>Uses the same {@code ItemStack.CODEC + RegistryOps} parse path as
     * {@code HandSnapshot.restore}.
     */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        var pd = mob.getPersistentData();

        var ops = RegistryOps.create(NbtOps.INSTANCE, mob.registryAccess());

        for (String key : new String[] { KEY_MAIN, KEY_OFF }) {
            if (!pd.contains(key)) continue;
            var tag = pd.get(key);
            if (tag != null) {
                ItemStack.CODEC.parse(ops, tag).ifSuccess(stack -> {
                    if (!stack.isEmpty()) {
                        ItemEntity drop = new ItemEntity(mob.level(),
                            mob.getX(), mob.getY(), mob.getZ(), stack);
                        drop.setDefaultPickUpDelay();
                        event.getDrops().add(drop);
                    }
                });
            }
            pd.remove(key);
        }
    }

}
