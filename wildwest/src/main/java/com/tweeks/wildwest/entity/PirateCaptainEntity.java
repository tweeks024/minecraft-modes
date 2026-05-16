package com.tweeks.wildwest.entity;

import com.tweeks.wildwest.api.Outlaw;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Flagship boss. Extends {@link WildWestMob} so pistol-vs-melee mode switching
 * comes for free. Adds a purple {@link ServerBossEvent} boss bar (mirrors
 * {@link HerobrineEntity}). Not a singleton — one per flagship is fine.
 */
public class PirateCaptainEntity extends WildWestMob implements Outlaw, Enemy {

    private final ServerBossEvent bossBar;
    // Persisted flag so we only auto-equip a captain that has never been
    // armed. Without it, a captain disarmed mid-fight (mod, /replaceitem,
    // looting tweak) would re-spawn its weapons every tick.
    private boolean equipmentInitialized = false;

    public PirateCaptainEntity(EntityType<? extends PirateCaptainEntity> type, Level level) {
        super(type, level);
        this.bossBar = new ServerBossEvent(
            this.getUUID(),
            Component.translatable("entity.wildwest.pirate_captain"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 120.0)
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.MOVEMENT_SPEED, 0.32)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
            .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected ItemStack getGunStack() {
        return new ItemStack(com.tweeks.wildwest.Registration.CAPTAIN_PISTOL.get());
    }

    @Override
    protected ItemStack getHandWeaponStack() {
        return buildEnchantedRapier();
    }

    private ItemStack buildEnchantedRapier() {
        ItemStack rapier = new ItemStack(com.tweeks.wildwest.Registration.RAPIER.get());
        // Mirrors HerobrineEntity.populateDefaultEquipmentSlots — the registry
        // lookup pattern that compiles cleanly in this build (Enchantments.*
        // are ResourceKeys, hoisted to Holders via the level's registry access).
        var registries = this.level().registryAccess();
        var enchRegistry = registries.lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> sharpness = enchRegistry.getOrThrow(Enchantments.SHARPNESS);
        Holder<Enchantment> fireAspect = enchRegistry.getOrThrow(Enchantments.FIRE_ASPECT);
        rapier.enchant(sharpness, 2);
        rapier.enchant(fireAspect, 1);
        return rapier;
    }

    @Override
    public boolean usesRifle() { return false; }

    @Override
    public boolean isLawman() { return false; }

    @Override
    public boolean isLeader() { return true; }

    @Override
    public void tick() {
        super.tick();
        // The weapon-mode tick in WildWestMob calls getHandWeaponStack() lazily,
        // so the enchanted rapier appears whenever the captain switches to MELEE.
        // Force-equip a freshly-spawned captain exactly once; the flag is
        // persisted, so reloaded captains keep their saved gear and disarmed
        // captains don't re-grow weapons every tick.
        if (!this.equipmentInitialized && !this.level().isClientSide()) {
            this.setItemSlot(EquipmentSlot.MAINHAND, this.getGunStack());
            this.setItemSlot(EquipmentSlot.OFFHAND, this.buildEnchantedRapier());
            this.setDropChance(EquipmentSlot.MAINHAND, 1.0f);
            this.setDropChance(EquipmentSlot.OFFHAND, 1.0f);
            this.equipmentInitialized = true;
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("EquipmentInitialized", this.equipmentInitialized);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.equipmentInitialized = input.getBooleanOr("EquipmentInitialized", false);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) return;
        this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        this.bossBar.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    public int getBaseExperienceReward(ServerLevel level) {
        return 50;
    }

    public ServerBossEvent getBossBar() {
        return this.bossBar;
    }
}
