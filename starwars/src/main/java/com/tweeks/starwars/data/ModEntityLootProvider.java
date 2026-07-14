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
                        .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.3f, 1.0f)))))
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
                        .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.3f, 1.0f))))));

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
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0f, 4.0f)))))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .when(LootItemRandomChanceCondition.randomChance(0.25f))
                    .add(LootItem.lootTableItem(Registration.HOLOCRON.get()))));

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
                    .add(EmptyLootItem.emptyItem().setWeight(40)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .when(LootItemRandomChanceCondition.randomChance(0.25f))
                    .add(LootItem.lootTableItem(Registration.HOLOCRON.get()))));

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
                    .add(EmptyLootItem.emptyItem().setWeight(40)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .when(LootItemRandomChanceCondition.randomChance(0.25f))
                    .add(LootItem.lootTableItem(Registration.HOLOCRON.get()))));

        // Han Solo: 1-2 gold_ingot @60% + 25% holocron — hero-tier haul
        // matching Luke. Same no-component-setting-loot-function caveat as
        // the other named heroes: no signature-weapon drop.
        this.add(ModEntities.HAN_SOLO.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(60)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 2.0f))))
                    .add(EmptyLootItem.emptyItem().setWeight(40)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .when(LootItemRandomChanceCondition.randomChance(0.25f))
                    .add(LootItem.lootTableItem(Registration.HOLOCRON.get()))));

        // Princess Leia: 1-2 emerald @60% + 25% holocron — hero-tier haul
        // matching Obi-Wan. Same no-component-setting-loot-function caveat.
        this.add(ModEntities.PRINCESS_LEIA.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.EMERALD).setWeight(60)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 2.0f))))
                    .add(EmptyLootItem.emptyItem().setWeight(40)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .when(LootItemRandomChanceCondition.randomChance(0.25f))
                    .add(LootItem.lootTableItem(Registration.HOLOCRON.get()))));

        // Boba Fett: 2-4 gold_ingot @100% (bounty-hunter payday, no
        // weight/empty split — always drops) + 50% blaster rifle (30%+
        // durability) + 25% holocron.
        this.add(ModEntities.BOBA_FETT.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.GOLD_INGOT)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0f, 4.0f)))))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .when(LootItemRandomChanceCondition.randomChance(0.50f))
                    .add(LootItem.lootTableItem(Registration.BLASTER_RIFLE.get())
                        .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.3f, 1.0f)))))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .when(LootItemRandomChanceCondition.randomChance(0.25f))
                    .add(LootItem.lootTableItem(Registration.HOLOCRON.get()))));

        // Astromech: 1-2 iron_nugget @80% + 1-2 redstone @60% (spare parts
        // and power-cell scraps; no weapon/holocron drop — it's a utility
        // droid, not a combatant).
        this.add(ModEntities.ASTROMECH.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.IRON_NUGGET).setWeight(80)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 2.0f))))
                    .add(EmptyLootItem.emptyItem().setWeight(20)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.REDSTONE).setWeight(60)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 2.0f))))
                    .add(EmptyLootItem.emptyItem().setWeight(40))));

        // Jawa: no natural drops — the barter interaction IS the loot.
        this.add(ModEntities.JAWA.get(), LootTable.lootTable());

        // Tusken Raider: 1-2 string + 1-2 bone (wraps and gaffi-stick scraps).
        this.add(ModEntities.TUSKEN_RAIDER.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.STRING)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 2.0f)))))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.BONE)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 2.0f))))));

        // Bantha: 2-4 leather + 1-3 beef — the walking supply drop.
        this.add(ModEntities.BANTHA.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.LEATHER)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0f, 4.0f)))))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.BEEF)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 3.0f))))));

        // Rebel trooper: 1-2 redstone + 0-1 iron_nugget (field-kit scraps).
        this.add(ModEntities.REBEL_TROOPER.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.REDSTONE)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 2.0f)))))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.IRON_NUGGET)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0f, 1.0f))))));

        // Probe droid: 1-2 redstone + 1 iron_ingot + 0-1 glowstone_dust
        // (power cells, chassis plate, sensor dust).
        this.add(ModEntities.PROBE_DROID.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.REDSTONE)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 2.0f)))))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.IRON_INGOT)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0f)))))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.GLOWSTONE_DUST)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0f, 1.0f))))));

        // Wampa: 2-3 white_wool + 1-2 bone (shaggy pelt and gnawed remains).
        this.add(ModEntities.WAMPA.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.WHITE_WOOL)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0f, 3.0f)))))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.BONE)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 2.0f))))));

        // Tauntaun: 1-3 leather (and, famously, warmth).
        this.add(ModEntities.TAUNTAUN.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.LEATHER)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 3.0f))))));

        // Snowtrooper: not in the wave's loot brief, but per its "only
        // differences" contract it mirrors the stormtrooper table exactly
        // (nuggets + rifle chance + armor-piece chance).
        this.add(ModEntities.SNOWTROOPER.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.IRON_NUGGET).setWeight(60)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0f, 2.0f))))
                    .add(EmptyLootItem.emptyItem().setWeight(40)))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .when(LootItemRandomChanceCondition.randomChance(0.25f))
                    .add(LootItem.lootTableItem(Registration.BLASTER_RIFLE.get())
                        .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.3f, 1.0f)))))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .when(LootItemRandomChanceCondition.randomChance(0.10f))
                    .add(LootItem.lootTableItem(Registration.STORMTROOPER_HELMET.get()))
                    .add(LootItem.lootTableItem(Registration.STORMTROOPER_CHESTPLATE.get()))
                    .add(LootItem.lootTableItem(Registration.STORMTROOPER_LEGGINGS.get()))
                    .add(LootItem.lootTableItem(Registration.STORMTROOPER_BOOTS.get()))));

        // Dragonsnake: 1-2 slime_ball (swamp ooze).
        this.add(ModEntities.DRAGONSNAKE.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.SLIME_BALL)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 2.0f))))));

        // Bogwing: exactly 1 feather.
        this.add(ModEntities.BOGWING.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.FEATHER)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0f))))));

        // Yoda: no drops — masters leave no possessions behind.
        this.add(ModEntities.YODA.get(), LootTable.lootTable());

        // AT-AT: a boss-tier salvage haul — 4-8 iron_ingot + 1 iron_block +
        // 1-2 redstone, plus a rare (10%) chin-blaster-grade rifle drop.
        this.add(ModEntities.AT_AT.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.IRON_INGOT)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0f, 8.0f)))))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.IRON_BLOCK)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0f)))))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.REDSTONE)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 2.0f)))))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .when(LootItemRandomChanceCondition.randomChance(0.10f))
                    .add(LootItem.lootTableItem(Registration.BLASTER_RIFLE.get())
                        .apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.3f, 1.0f))))));

        // Band droid: 1-2 iron_nugget (spare servo parts) — a harmless musician.
        this.add(ModEntities.BAND_DROID.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.IRON_NUGGET)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 2.0f))))));

        // Chewbacca: 0-2 string (shed wookiee fur). A TAMED Chewbacca drops
        // nothing — the entity suppresses this table while tame so a killed
        // companion isn't looted (see ChewbaccaEntity#dropFromLootTable).
        this.add(ModEntities.CHEWBACCA.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.STRING)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0f, 2.0f))))));

        // Grogu: no drops — you would never.
        this.add(ModEntities.GROGU.get(), LootTable.lootTable());

        // Ewok: 0-2 sticks (spear hafts) + 0-1 leather (hide scraps).
        this.add(ModEntities.EWOK.get(),
            LootTable.lootTable()
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.STICK)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0f, 2.0f)))))
                .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0f))
                    .add(LootItem.lootTableItem(Items.LEATHER)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0f, 1.0f))))));
    }

    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        Set<EntityType<?>> known = Set.of(
            ModEntities.STORMTROOPER.get(),
            ModEntities.BATTLE_DROID.get(),
            ModEntities.JEDI_KNIGHT.get(),
            ModEntities.DARTH_VADER.get(),
            ModEntities.LUKE_SKYWALKER.get(),
            ModEntities.OBI_WAN.get(),
            ModEntities.BOBA_FETT.get(),
            ModEntities.ASTROMECH.get(),
            ModEntities.HAN_SOLO.get(),
            ModEntities.PRINCESS_LEIA.get(),
            ModEntities.JAWA.get(),
            ModEntities.TUSKEN_RAIDER.get(),
            ModEntities.BANTHA.get(),
            ModEntities.REBEL_TROOPER.get(),
            ModEntities.PROBE_DROID.get(),
            ModEntities.WAMPA.get(),
            ModEntities.TAUNTAUN.get(),
            ModEntities.SNOWTROOPER.get(),
            ModEntities.DRAGONSNAKE.get(),
            ModEntities.BOGWING.get(),
            ModEntities.YODA.get(),
            ModEntities.AT_AT.get(),
            ModEntities.BAND_DROID.get(),
            ModEntities.CHEWBACCA.get(),
            ModEntities.GROGU.get(),
            ModEntities.EWOK.get()
        );
        return BuiltInRegistries.ENTITY_TYPE.stream().filter(known::contains);
    }
}
