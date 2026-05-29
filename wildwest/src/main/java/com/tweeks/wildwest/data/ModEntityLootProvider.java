package com.tweeks.wildwest.data;

import com.tweeks.wildwest.ModEntities;
import com.tweeks.wildwest.Registration;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemDamageFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.Set;
import java.util.stream.Stream;

public class ModEntityLootProvider extends EntityLootSubProvider {

    public ModEntityLootProvider(HolderLookup.Provider lookup) {
        super(FeatureFlags.REGISTRY.allFlags(), lookup);
    }

    /**
     * Adds a 50% chance to drop the mob's gun with 30%+ remaining durability.
     * Mirrors the spec: "Plus the gun the mob was carrying (50% drop chance,
     * 30%+ remaining durability via LootItemRandomChanceCondition + SetItemDamageFunction)."
     */
    private static LootPool.Builder gunDropPool(Item gun) {
        return LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
            .when(LootItemRandomChanceCondition.randomChance(0.5f))
            .add(LootItem.lootTableItem(gun)
                .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.0f, 0.7f))));
    }

    @Override
    public void generate() {
        // Deputy: 0-1 billy_club @ 25%; 0-2 emerald @ 60%; 50% pistol drop
        this.add(ModEntities.DEPUTY.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Registration.BILLY_CLUB.get()).setWeight(25))
                    .add(EmptyLootItem.emptyItem().setWeight(75)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.EMERALD).setWeight(60)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0f, 2.0f))))
                    .add(EmptyLootItem.emptyItem().setWeight(40)))
                .withPool(gunDropPool(Registration.PISTOL.get())));

        // Sherrif: 0-1 billy_club @ 25%; 1-3 emerald @ 80%; 0-1 iron_ingot @ 30%
        this.add(ModEntities.SHERRIF.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Registration.BILLY_CLUB.get()).setWeight(25))
                    .add(EmptyLootItem.emptyItem().setWeight(75)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.EMERALD).setWeight(80)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 3.0f))))
                    .add(EmptyLootItem.emptyItem().setWeight(20)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(30))
                    .add(EmptyLootItem.emptyItem().setWeight(70)))
                .withPool(gunDropPool(Registration.RIFLE.get())));

        // Bandit: 0-1 bandit_knife @ 25%; 0-1 gold_ingot @ 30%; 0-2 string @ 50%; 50% pistol drop
        this.add(ModEntities.BANDIT.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Registration.BANDIT_KNIFE.get()).setWeight(25))
                    .add(EmptyLootItem.emptyItem().setWeight(75)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(30))
                    .add(EmptyLootItem.emptyItem().setWeight(70)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.STRING).setWeight(50)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0f, 2.0f))))
                    .add(EmptyLootItem.emptyItem().setWeight(50)))
                .withPool(gunDropPool(Registration.PISTOL.get())));

        // BanditLeader: 0-1 bandit_knife @ 30%; 1-3 gold_ingot @ 60%; 0-1 emerald @ 30%; 50% rifle drop
        this.add(ModEntities.BANDIT_LEADER.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Registration.BANDIT_KNIFE.get()).setWeight(30))
                    .add(EmptyLootItem.emptyItem().setWeight(70)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(60)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 3.0f))))
                    .add(EmptyLootItem.emptyItem().setWeight(40)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.EMERALD).setWeight(30))
                    .add(EmptyLootItem.emptyItem().setWeight(70)))
                .withPool(gunDropPool(Registration.RIFLE.get())));

        // Crab: no drops
        this.add(ModEntities.CRAB.get(), LootTable.lootTable());
    }

    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        Set<EntityType<?>> known = Set.of(
            ModEntities.DEPUTY.get(),
            ModEntities.SHERRIF.get(),
            ModEntities.BANDIT.get(),
            ModEntities.BANDIT_LEADER.get(),
            ModEntities.CRAB.get()
        );
        return BuiltInRegistries.ENTITY_TYPE.stream().filter(known::contains);
    }
}
