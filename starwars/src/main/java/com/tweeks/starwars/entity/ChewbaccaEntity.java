package com.tweeks.starwars.entity;

import com.tweeks.starwars.entity.ai.BowcasterAttackGoal;
import com.tweeks.starwars.entity.ai.TargetPredicates;
import com.tweeks.starwars.faction.SwCombatant;
import com.tweeks.starwars.faction.SwFaction;
import com.tweeks.starwars.network.S2CBlasterTracerPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * Chewbacca: a tameable wookiee companion (the wolf-taming pattern on the
 * LIGHT side of the faction war). Wild Chewbacca is neutral — he wanders and
 * only defends himself with his fists ({@link MeleeAttackGoal} off
 * {@link HurtByTargetGoal}), never shooting unprovoked. Feed him a
 * {@code COOKED_PORKCHOP} or {@code COOKED_BEEF} for a chance (~1/3, like
 * wolf bones) to tame him.
 *
 * <p>Once tamed he follows his owner ({@link FollowOwnerGoal}, teleporting
 * when left far behind), sits on a shift-free empty-hand interact
 * ({@link SitWhenOrderedToGoal}), and wields a bowcaster
 * ({@link BowcasterAttackGoal}) against the Empire — proactively hunting
 * EMPIRE {@link SwCombatant}s and defending his owner via
 * {@link OwnerHurtByTargetGoal}/{@link OwnerHurtTargetGoal}. A tamed
 * Chewbacca never despawns and drops no loot; a wild one drops a little
 * string.
 */
public class ChewbaccaEntity extends TamableAnimal implements SwCombatant {

    public static final double MAX_HEALTH = 40.0;
    public static final double MOVEMENT_SPEED = 0.30;
    public static final double ATTACK_DAMAGE = 6.0;
    /** ~1/3 tame chance per feed, mirroring the vanilla wolf. */
    public static final int TAME_CHANCE_DENOM = 3;
    /** Roughly one subtle roar every ~40s of server ticks. */
    private static final int ROAR_CHANCE_DENOM = 800;

    public ChewbaccaEntity(EntityType<? extends ChewbaccaEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    public SwFaction getFaction() { return SwFaction.LIGHT; }

    /** LIGHT-side bolt color, matching {@code SwMob} blaster wielders. */
    public int getTracerColor() { return S2CBlasterTracerPacket.COLOR_LIGHT; }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        // Bowcaster (tamed only, LOOK-only) outranks the melee fallback, so a
        // tamed Chewbacca holds his ground and shoots while a wild one, whose
        // bowcaster never activates, closes to punch.
        this.goalSelector.addGoal(2, new BowcasterAttackGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0, 10.0F, 3.0F));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        // Proactive faction war, tamed only: hunt EMPIRE combatants using the
        // existing cross-faction decision (LIGHT targets its enemy, EMPIRE).
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(
            this, LivingEntity.class, 10, false, false,
            (target, level) -> this.isTame()
                && target instanceof SwCombatant sc
                && TargetPredicates.shouldTarget(
                    SwFaction.LIGHT, true, sc.getFaction(), false, 0, false)));
    }

    /**
     * Faction-aware version of the wolf's target filter: a tamed Chewbacca
     * fights the Empire and defends his owner, but never turns on the owner,
     * other light-side units, or another tamed pet.
     */
    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (target instanceof SwCombatant sc) return sc.getFaction() != SwFaction.LIGHT;
        if (target instanceof TamableAnimal tame) return !tame.isTame();
        return true;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.COOKED_PORKCHOP) || stack.is(Items.COOKED_BEEF);
    }

    // Chewbacca is a one-of-a-kind companion, not livestock — no breeding.
    @Override
    public boolean canFallInLove() { return false; }

    @Override
    @Nullable
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (this.isTame()) {
            // Feed to heal when hurt (does not re-roll taming).
            if (this.isFood(stack) && this.getHealth() < this.getMaxHealth()) {
                if (!this.level().isClientSide()) {
                    this.heal(4.0F);
                    stack.consume(1, player);
                }
                return InteractionResult.SUCCESS;
            }
            // Empty-hand interact by the owner toggles the sit order.
            if (this.isOwnedBy(player) && stack.isEmpty()) {
                if (!this.level().isClientSide()) {
                    this.setOrderedToSit(!this.isOrderedToSit());
                    this.jumping = false;
                    this.navigation.stop();
                    this.setTarget(null);
                }
                return InteractionResult.SUCCESS;
            }
            return super.mobInteract(player, hand);
        }

        // Wild: feeding cooked meat has a chance to tame him.
        if (!this.level().isClientSide() && this.isFood(stack)) {
            stack.consume(1, player);
            if (this.random.nextInt(TAME_CHANCE_DENOM) == 0
                    && !net.neoforged.neoforge.event.EventHooks.onAnimalTame(this, player)) {
                this.tame(player);
                this.navigation.stop();
                this.setTarget(null);
                this.setOrderedToSit(true);
                this.level().broadcastEntityEvent(this, (byte) 7);   // hearts
            } else {
                this.level().broadcastEntityEvent(this, (byte) 6);   // smoke
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.mobInteract(player, hand);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData);
        // His bowcaster (reusing the blaster-rifle visual) — cosmetic on a
        // wild wookiee, but the weapon his bowcaster goal fires once tamed.
        this.setItemSlot(EquipmentSlot.MAINHAND,
            new ItemStack(com.tweeks.starwars.Registration.BLASTER_RIFLE.get()));
        return result;
    }

    /**
     * A subtle, occasional roar (Ravager bellow, pitched well down). Runs in
     * the server AI step, so it never fires client-side or while paused.
     */
    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.random.nextInt(ROAR_CHANCE_DENOM) == 0) {
            this.playSound(SoundEvents.RAVAGER_ROAR, 0.5F, 0.55F);
        }
    }

    // A tamed companion never despawns.
    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return !this.isTame();
    }

    // Never drop the held bowcaster on death (skips equipment loot entirely).
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
    }

    // Wild Chewbacca drops his loot-table string; a tamed one leaves nothing.
    @Override
    protected void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKilled) {
        if (this.isTame()) return;
        super.dropFromLootTable(level, source, playerKilled);
    }
}
