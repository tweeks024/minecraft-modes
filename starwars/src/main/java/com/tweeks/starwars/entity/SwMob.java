package com.tweeks.starwars.entity;

import com.tweeks.starwars.entity.ai.BlasterAttackGoal;
import com.tweeks.starwars.entity.ai.SwTargetGoal;
import com.tweeks.starwars.faction.SwCombatant;
import com.tweeks.starwars.faction.SwFaction;
import com.tweeks.starwars.network.S2CBlasterTracerPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public abstract class SwMob extends PathfinderMob implements SwCombatant {

    protected SwMob(EntityType<? extends SwMob> type, Level level) {
        super(type, level);
    }

    /** True for blaster-wielders (trooper, droid, Boba). Saber wielders return false. */
    public abstract boolean usesBlaster();

    /** True if the blaster is a rifle (heavier shot, slower isn't modeled — damage only). */
    public abstract boolean usesRifleBlaster();

    /** The item shown in the main hand (blaster or saber). */
    protected abstract ItemStack getWeaponStack();

    public int getTracerColor() {
        return getFaction() == SwFaction.LIGHT
            ? S2CBlasterTracerPacket.COLOR_LIGHT
            : S2CBlasterTracerPacket.COLOR_EMPIRE;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new BlasterAttackGoal(this));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new SwTargetGoal(this));
    }

    @Override
    @org.jetbrains.annotations.Nullable
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.EntitySpawnReason reason,
            @org.jetbrains.annotations.Nullable net.minecraft.world.entity.SpawnGroupData spawnData) {
        net.minecraft.world.entity.SpawnGroupData result =
            super.finalizeSpawn(level, difficulty, reason, spawnData);
        this.setItemSlot(EquipmentSlot.MAINHAND, this.getWeaponStack());
        return result;
    }
}
