package com.tweeks.thief.data;

import com.tweeks.thief.Registration;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.stream.Stream;

public class ModEntityLootProvider extends EntityLootSubProvider {

    public ModEntityLootProvider(HolderLookup.Provider lookup) {
        super(FeatureFlags.REGISTRY.allFlags(), lookup);
    }

    @Override
    public void generate() {
        this.add(Registration.THIEF.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.EMERALD)
                        .setWeight(50)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0f))))
                    .add(net.minecraft.world.level.storage.loot.entries.EmptyLootItem.emptyItem().setWeight(50)))
                .withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.ARROW)
                        .setWeight(60)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0f, 2.0f))))
                    .add(net.minecraft.world.level.storage.loot.entries.EmptyLootItem.emptyItem().setWeight(40)))
                .withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Registration.BLACKJACK.get()).setWeight(25))
                    .add(net.minecraft.world.level.storage.loot.entries.EmptyLootItem.emptyItem().setWeight(75))));
    }

    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        return BuiltInRegistries.ENTITY_TYPE.stream()
            .filter(t -> t == Registration.THIEF.get());
    }
}
