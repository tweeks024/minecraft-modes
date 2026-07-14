package com.tweeks.starwars.entity;

import com.tweeks.starwars.faction.SwFaction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Ewok: a small furry forest native. LIGHT-faction {@link SwMob} — friendly
 * to good-standing players but hostile to the Empire: it hunts EMPIRE
 * {@link com.tweeks.starwars.faction.SwCombatant}s (stormtroopers, droids)
 * via the same cross-faction {@link com.tweeks.starwars.entity.ai.SwTargetGoal}
 * the Rebel Trooper uses, and retaliates when hurt (HurtByTargetGoal). Unlike
 * the blaster troopers it fights in melee with a wooden spear — modeled as
 * {@code usesBlaster() == false}, which flips {@link SwMob}'s default goal set
 * to the charge-speed MeleeAttackGoal, with damage coming from the
 * ATTACK_DAMAGE attribute below.
 *
 * <p>Personality touches: it panics and flees briefly when badly wounded
 * (&lt; 30% health) then rejoins the fight once the fright wears off (the
 * vanilla last-damage-source 40-tick timeout does the "briefly" for us), and
 * occasionally beats a subtle war-drum ({@link #customServerAiStep}).
 *
 * <p>CREATURE category with the generic ground spawn rule (registered in
 * {@code StarWarsMod}); group/village placement is left to the integrator.
 */
public class EwokEntity extends SwMob {

    public static final double MAX_HEALTH = 12.0;
    public static final double MOVEMENT_SPEED = 0.30;
    public static final double ATTACK_DAMAGE = 4.0;

    /** Below this fraction of max health the Ewok panics (flees) briefly. */
    private static final float LOW_HEALTH_FRACTION = 0.30f;
    /** Per-tick 1-in-N chance of a war-drum beat in {@link #customServerAiStep}. */
    private static final int DRUM_CHANCE_DENOM = 800;

    public EwokEntity(EntityType<? extends EwokEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    public SwFaction getFaction() { return SwFaction.LIGHT; }

    // Melee spear-fighter: no blaster, so SwMob's registerGoals wires the
    // charge-speed MeleeAttackGoal instead of the BlasterAttackGoal.
    @Override
    public boolean usesBlaster() { return false; }

    @Override
    public boolean usesRifleBlaster() { return false; }

    // The "spear" is flavor/melee-only — no held item entity (like the Tusken
    // Raider, damage comes from the ATTACK_DAMAGE attribute via the melee goal).
    @Override
    protected ItemStack getWeaponStack() { return ItemStack.EMPTY; }

    @Override
    protected void registerGoals() {
        // SwMob: FloatGoal, charge-speed MeleeAttackGoal (gated on !usesBlaster),
        // stroll/look/lookAround, HurtByTargetGoal + cross-faction SwTargetGoal.
        super.registerGoals();
        // Brief low-health panic: flee only while badly wounded AND freshly
        // hurt. The vanilla last-damage-source 40-tick timeout ends the panic,
        // so it rejoins the fight shortly after. Priority 1 preempts the melee
        // goal (priority 2) while active.
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.5) {
            @Override
            public boolean canUse() {
                return EwokEntity.this.getHealth()
                        < EwokEntity.this.getMaxHealth() * LOW_HEALTH_FRACTION
                    && super.canUse();
            }
        });
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        // Subtle war-drum/chatter: an occasional low wood-tom beat.
        if (this.random.nextInt(DRUM_CHANCE_DENOM) == 0) {
            this.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 0.6F, 0.7F);
        }
    }

    // ---------- sounds: small chittery forest critter ----------

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FOX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    public float getVoicePitch() {
        // Small and squeaky.
        return super.getVoicePitch() * 1.3F;
    }
}
