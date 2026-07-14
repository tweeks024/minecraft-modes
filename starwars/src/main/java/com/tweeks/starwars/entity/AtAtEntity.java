package com.tweeks.starwars.entity;

import com.tweeks.starwars.entity.ai.AtAtChinBlasterGoal;
import com.tweeks.starwars.entity.ai.AtAtStompGoal;
import com.tweeks.starwars.entity.ai.SwTargetGoal;
import com.tweeks.starwars.faction.SwFaction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * AT-AT (All Terrain Armored Transport): an Imperial siege walker and boss.
 * An {@link SwMob} so it plugs into the faction war ({@link SwTargetGoal}
 * sees it as EMPIRE, so rebel troopers and Yoda auto-engage it back), armed
 * with a chin blaster ({@link AtAtChinBlasterGoal}) that rains fire from its
 * high head and a slow foot {@link AtAtStompGoal} for anything beneath it.
 *
 * <p>300 HP, immovable ({@code KNOCKBACK_RESISTANCE 1.0}), plods at speed
 * {@code 0.15}, and steps over 2-block obstacles. No natural-spawn wiring
 * lives here (the spawn cost list is owned elsewhere); the spawn PLACEMENT
 * rule in {@code StarWarsMod} uses the generic mob rule, not the monster
 * darkness rule — a daytime siege walker must be allowed to appear in the
 * light.
 */
public class AtAtEntity extends SwMob implements Enemy {

    public static final double MAX_HEALTH = 300.0;
    public static final double MOVEMENT_SPEED = 0.15;
    public static final double ATTACK_DAMAGE = 12.0;
    public static final double KNOCKBACK_RESISTANCE = 1.0;
    public static final double STEP_HEIGHT = 2.0;
    public static final double FOLLOW_RANGE = 32.0;

    public AtAtEntity(EntityType<? extends AtAtEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
            .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE)
            .add(Attributes.STEP_HEIGHT, STEP_HEIGHT)
            .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE);
    }

    @Override
    public SwFaction getFaction() { return SwFaction.EMPIRE; }

    @Override
    public boolean usesBlaster() { return true; }

    @Override
    public boolean usesRifleBlaster() { return false; }

    @Override
    protected ItemStack getWeaponStack() {
        // The chin blaster is built into the head — nothing held.
        return ItemStack.EMPTY;
    }

    @Override
    protected void registerGoals() {
        // Deliberately NOT super.registerGoals(): the SwMob default set is for
        // ground troopers; the walker fields its own chin blaster + stomp.
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new AtAtStompGoal(this, 1.0));
        this.goalSelector.addGoal(2, new AtAtChinBlasterGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.5));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Cross-faction targeting: engages LIGHT combatants via the faction seam.
        this.targetSelector.addGoal(2, new SwTargetGoal(this));
        // Siege boss: also bears down on any player it spots (like the probe droid).
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ---------- sounds ----------

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.IRON_GOLEM_STEP;   // heavy servos
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.5F;   // deep and slow
    }
}
