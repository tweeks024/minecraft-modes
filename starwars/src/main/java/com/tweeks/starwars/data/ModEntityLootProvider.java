package com.tweeks.starwars.data;

import com.tweeks.starwars.ModEntities;
import com.tweeks.starwars.Registration;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
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

    @Override
    public void generate() {
        // Stormtrooper: 0-2 iron_nugget @60%; 25% blaster rifle drop (30%+ durability).
        this.add(ModEntities.STORMTROOPER.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.IRON_NUGGET).setWeight(60)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0f, 2.0f))))
                    .add(EmptyLootItem.emptyItem().setWeight(40)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .when(LootItemRandomChanceCondition.randomChance(0.25f))
                    .add(LootItem.lootTableItem(Registration.BLASTER_RIFLE.get())
                        .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.0f, 0.7f))))));

        // Battle droid: 1-3 iron_nugget @80%; 15% blaster pistol drop.
        this.add(ModEntities.BATTLE_DROID.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.IRON_NUGGET).setWeight(80)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 3.0f))))
                    .add(EmptyLootItem.emptyItem().setWeight(20)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .when(LootItemRandomChanceCondition.randomChance(0.15f))
                    .add(LootItem.lootTableItem(Registration.BLASTER_PISTOL.get())
                        .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.0f, 0.7f))))));
    }

    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        Set<EntityType<?>> known = Set.of(
            ModEntities.STORMTROOPER.get(),
            ModEntities.BATTLE_DROID.get()
        );
        return BuiltInRegistries.ENTITY_TYPE.stream().filter(known::contains);
    }
}
