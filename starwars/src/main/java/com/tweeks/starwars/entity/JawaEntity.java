package com.tweeks.starwars.entity;

import com.tweeks.starwars.faction.SwCombatant;
import com.tweeks.starwars.faction.SwFaction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Jawa: skittish desert scavenger. Not a fighter — wanders, panics when
 * hurt, and barters: right-click with an iron ingot to trade it for one
 * random tech item off the {@link JawaBarter} table, on a 10 s per-jawa
 * cooldown. {@link SwCombatant} with NEUTRAL faction (a faction-war
 * bystander: war mobs ignore it, harming it is alignment-neutral) so
 * Tusken Raiders can hunt it via a plain class-targeted goal.
 */
public class JawaEntity extends PathfinderMob implements SwCombatant {

    public static final double MAX_HEALTH = 10.0;
    public static final double MOVEMENT_SPEED = 0.3;

    /** Game time of the last successful barter; persisted so reloads don't reset the cooldown. */
    private long lastBarterGameTime = JawaBarter.NEVER_BARTERED;

    public JawaEntity(EntityType<? extends JawaEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, MAX_HEALTH)
            .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    public SwFaction getFaction() { return SwFaction.NEUTRAL; }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    // ---------- barter ----------

    /** Jawas value credits over scrap — pay this many for a premium good. */
    private static final int CREDIT_PRICE = 3;

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(com.tweeks.starwars.Registration.CREDIT.get())) {
            return creditTrade(player, stack);
        }
        if (!stack.is(Items.IRON_INGOT)) {
            return super.mobInteract(player, hand);
        }
        if (this.level().isClientSide()) {
            // Server decides; optimistic swing on the client.
            return InteractionResult.SUCCESS;
        }
        long now = this.level().getGameTime();
        if (!JawaBarter.isReady(this.lastBarterGameTime, now)) {
            // Still haggling — jawa refuses (no ingot consumed).
            this.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.5F);
            return InteractionResult.CONSUME;
        }
        this.lastBarterGameTime = now;
        stack.consume(1, player);
        JawaBarter.Roll roll = JawaBarter.roll(new java.util.Random(this.random.nextLong()));
        this.spawnAtLocation((ServerLevel) this.level(),
            new ItemStack(itemFor(roll.item()), roll.count()));
        // Happy little "utini!" — villager yes, pitched up.
        this.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.6F);
        return InteractionResult.SUCCESS;
    }

    private static Item itemFor(JawaBarter.TechItem item) {
        return switch (item) {
            case REDSTONE -> Items.REDSTONE;
            case COPPER_INGOT -> Items.COPPER_INGOT;
            case QUARTZ -> Items.QUARTZ;
            case IRON_NUGGET -> Items.IRON_NUGGET;
            case GLOWSTONE_DUST -> Items.GLOWSTONE_DUST;
        };
    }

    /** Credits buy premium goods — better than the iron-scrap table. */
    private InteractionResult creditTrade(Player player, ItemStack stack) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (stack.getCount() < CREDIT_PRICE) {
            this.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.5F);
            return InteractionResult.CONSUME;
        }
        stack.consume(CREDIT_PRICE, player);
        int roll = this.random.nextInt(100);
        ItemStack reward = roll < 8 ? new ItemStack(Items.DIAMOND)
            : roll < 38 ? new ItemStack(Items.GOLD_BLOCK)
            : roll < 68 ? new ItemStack(Items.IRON_BLOCK)
            : new ItemStack(Items.REDSTONE_BLOCK, 2);
        this.spawnAtLocation((ServerLevel) this.level(), reward);
        this.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.6F);
        return InteractionResult.SUCCESS;
    }

    // ---------- persistence ----------

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.lastBarterGameTime = input.getLongOr("LastBarterTime", JawaBarter.NEVER_BARTERED);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putLong("LastBarterTime", this.lastBarterGameTime);
    }

    // ---------- sounds ----------

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        // Small hooded scavenger — chirpy.
        return super.getVoicePitch() * 1.5F;
    }
}
