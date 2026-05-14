package com.tweeks.wildwest.entity;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

/**
 * Apex boss mob, peer of Herobrine. Singleton across the whole server (see
 * {@link Entity303SavedData}). Spawns at night in the overworld or any time
 * in The End; fires an enchanted bow at range, swaps to an iron sword in
 * melee, teleports every ~3 s. On damage occasionally spawns a 1-HP visual
 * decoy ({@code Entity303CloneEntity}) and warps behind the attacker.
 */
public class Entity303Entity extends Monster {

    private final ServerBossEvent bossBar;

    public Entity303Entity(EntityType<? extends Entity303Entity> type, Level level) {
        super(type, level);
        this.bossBar = new ServerBossEvent(
            Mth.createInsecureUUID(this.random),
            Component.translatable("entity.wildwest.entity_303"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);
        this.setPersistenceRequired();
        this.setCustomName(Component.translatable("entity.wildwest.entity_303"));
        this.setCustomNameVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 160.0)
            .add(Attributes.MOVEMENT_SPEED, 0.45)
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
            .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);

        var registries = this.level().registryAccess();
        var enchRegistry = registries.lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> power = enchRegistry.getOrThrow(Enchantments.POWER);
        Holder<Enchantment> flame = enchRegistry.getOrThrow(Enchantments.FLAME);
        Holder<Enchantment> sharpness = enchRegistry.getOrThrow(Enchantments.SHARPNESS);

        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(power, 5);
        bow.enchant(flame, 1);

        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.enchant(sharpness, 4);

        // Default stance: bow in mainhand, sword in offhand. Goals swap as
        // combat distance changes (see Entity303MeleeGoal / Entity303BowGoal).
        this.setItemSlot(EquipmentSlot.MAINHAND, bow);
        this.setItemSlot(EquipmentSlot.OFFHAND, sword);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.10f);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.10f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ELDER_GUARDIAN_AMBIENT_LAND;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4f;
    }

    @Override
    public int getBaseExperienceReward(ServerLevel level) {
        return 80;
    }

    public ServerBossEvent getBossBar() {
        return this.bossBar;
    }
}
