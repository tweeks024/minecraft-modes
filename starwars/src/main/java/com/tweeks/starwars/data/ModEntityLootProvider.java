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
        // Third pool: 10% chance, one-of-four equal-weighted armor piece drop.
        this.add(ModEntities.STORMTROOPER.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.IRON_NUGGET).setWeight(60)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0f, 2.0f))))
                    .add(EmptyLootItem.emptyItem().setWeight(40)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .when(LootItemRandomChanceCondition.randomChance(0.25f))
                    .add(LootItem.lootTableItem(Registration.BLASTER_RIFLE.get())
                        .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.0f, 0.7f)))))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .when(LootItemRandomChanceCondition.randomChance(0.10f))
                    .add(LootItem.lootTableItem(Registration.STORMTROOPER_HELMET.get()))
                    .add(LootItem.lootTableItem(Registration.STORMTROOPER_CHESTPLATE.get()))
                    .add(LootItem.lootTableItem(Registration.STORMTROOPER_LEGGINGS.get()))
                    .add(LootItem.lootTableItem(Registration.STORMTROOPER_BOOTS.get()))));

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

        // Jedi Knight: 0-1 glowstone_dust @50%; 10% lightsaber drop (sabers
        // are the ruin's treasure — a 20% common-mob drop would be too generous).
        this.add(ModEntities.JEDI_KNIGHT.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.GLOWSTONE_DUST).setWeight(50)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0f, 1.0f))))
                    .add(EmptyLootItem.emptyItem().setWeight(50)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .when(LootItemRandomChanceCondition.randomChance(0.10f))
                    .add(LootItem.lootTableItem(Registration.LIGHTSABER.get()))));

        // Darth Vader: 2-4 obsidian @100% (a boss-tier material haul, no
        // nether_star). A 30% red-lightsaber drop was planned, but this MC
        // version has no component-setting loot function (grepped
        // "SetComponents|set_components" across wildwest/ and craftee/ with
        // no hits — see LootItemFunction/SetItemCountFunction/SetItemDamageFunction
        // siblings for what *is* available), and LightsaberItem.stackWithColor's
        // color lives in a data component with no setter loot function to pin it
        // to red — a plain LootItem drop would render blue (index 0) by default,
        // which is wrong for "Vader's saber". Falling back to obsidian-only per
        // the task brief until a component-setting loot function exists.
        this.add(ModEntities.DARTH_VADER.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.OBSIDIAN)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0f, 4.0f))))));

        // Luke Skywalker: 1-2 gold_ingot @60% (a hero-tier but not
        // boss-tier haul). A 30% green-lightsaber drop was planned, but —
        // same caveat as Darth Vader above — this MC version has no
        // component-setting loot function (grepped "SetComponents|set_components"
        // across wildwest/ and craftee/ with no hits), and LightsaberItem
        // .stackWithColor's color lives in a data component with no setter
        // loot function to pin it to green — a plain LootItem drop would
        // render blue (index 0) by default, which is wrong for "Luke's
        // saber". Falling back to gold-ingot-only per the task brief until a
        // component-setting loot function exists.
        this.add(ModEntities.LUKE_SKYWALKER.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(60)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 2.0f))))
                    .add(EmptyLootItem.emptyItem().setWeight(40))));

        // Obi-Wan Kenobi: 1-2 emerald @60% (a hero-tier but not boss-tier
        // haul, matching Luke). A blue-lightsaber drop was planned, but —
        // same caveat as Darth Vader and Luke above — this MC version has no
        // component-setting loot function (grepped "SetComponents|set_components"
        // across wildwest/ and craftee/ with no hits), and LightsaberItem
        // .stackWithColor's color lives in a data component with no setter
        // loot function to pin it to blue — a plain LootItem drop would
        // render blue (index 0) by default anyway, which is *coincidentally*
        // correct here, but the omission is kept consistent with the other
        // named heroes/villains until a component-setting loot function
        // exists. Falling back to emerald-only per the task brief.
        this.add(ModEntities.OBI_WAN.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.EMERALD).setWeight(60)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 2.0f))))
                    .add(EmptyLootItem.emptyItem().setWeight(40))));
    }

    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        Set<EntityType<?>> known = Set.of(
            ModEntities.STORMTROOPER.get(),
            ModEntities.BATTLE_DROID.get(),
            ModEntities.JEDI_KNIGHT.get(),
            ModEntities.DARTH_VADER.get(),
            ModEntities.LUKE_SKYWALKER.get(),
            ModEntities.OBI_WAN.get()
        );
        return BuiltInRegistries.ENTITY_TYPE.stream().filter(known::contains);
    }
}
