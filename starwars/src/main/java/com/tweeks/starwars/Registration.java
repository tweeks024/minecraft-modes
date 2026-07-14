package com.tweeks.starwars;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Registration {
    private Registration() {}

    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(StarWarsMod.MOD_ID);

    public static final DeferredRegister.Blocks BLOCKS =
        DeferredRegister.createBlocks(StarWarsMod.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, StarWarsMod.MOD_ID);

    // Technical block — no BlockItem; the film only exists inside gate frames.
    public static final net.neoforged.neoforge.registries.DeferredBlock<com.tweeks.starwars.world.gate.HyperspacePortalBlock>
        HYPERSPACE_PORTAL = BLOCKS.registerBlock("hyperspace_portal",
            com.tweeks.starwars.world.gate.HyperspacePortalBlock::new,
            () -> net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                .noCollision()
                .strength(-1.0F)
                .sound(net.minecraft.world.level.block.SoundType.GLASS)
                .lightLevel(state -> 11)
                .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)
                .noLootTable());

    public static final DeferredItem<com.tweeks.starwars.item.StarCompassItem> STAR_COMPASS =
        ITEMS.registerItem("star_compass", com.tweeks.starwars.item.StarCompassItem::new, p -> p);

    // The cantina band's set list, on one well-worn record.
    public static final DeferredItem<Item> CANTINA_RECORD = ITEMS.registerItem("cantina_record",
        Item::new,
        p -> p.stacksTo(1)
              .rarity(net.minecraft.world.item.Rarity.RARE)
              .jukeboxPlayable(com.tweeks.starwars.item.ModJukeboxSongs.CANTINA_BAND));

    public static final DeferredItem<com.tweeks.starwars.item.GalaxyMapItem> GALAXY_MAP =
        ITEMS.registerItem("galaxy_map", com.tweeks.starwars.item.GalaxyMapItem::new, p -> p);

    // Kyber ores: deepslate-hosted, iron-pickaxe tier, one colour per planet.
    private static java.util.function.Supplier<net.minecraft.world.level.block.state.BlockBehaviour.Properties> kyberOreProps() {
        return () -> net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
            .strength(4.5F, 3.0F)
            .requiresCorrectToolForDrops()
            .sound(net.minecraft.world.level.block.SoundType.DEEPSLATE)
            .lightLevel(state -> 4);
    }

    public static final net.neoforged.neoforge.registries.DeferredBlock<net.minecraft.world.level.block.Block>
        BLUE_KYBER_ORE = BLOCKS.registerBlock("blue_kyber_ore", net.minecraft.world.level.block.Block::new, kyberOreProps());
    public static final net.neoforged.neoforge.registries.DeferredBlock<net.minecraft.world.level.block.Block>
        GREEN_KYBER_ORE = BLOCKS.registerBlock("green_kyber_ore", net.minecraft.world.level.block.Block::new, kyberOreProps());
    public static final net.neoforged.neoforge.registries.DeferredBlock<net.minecraft.world.level.block.Block>
        PURPLE_KYBER_ORE = BLOCKS.registerBlock("purple_kyber_ore", net.minecraft.world.level.block.Block::new, kyberOreProps());

    public static final DeferredItem<net.minecraft.world.item.BlockItem> BLUE_KYBER_ORE_ITEM =
        ITEMS.registerSimpleBlockItem(BLUE_KYBER_ORE);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> GREEN_KYBER_ORE_ITEM =
        ITEMS.registerSimpleBlockItem(GREEN_KYBER_ORE);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> PURPLE_KYBER_ORE_ITEM =
        ITEMS.registerSimpleBlockItem(PURPLE_KYBER_ORE);

    public static final DeferredItem<com.tweeks.starwars.item.KyberCrystalItem> KYBER_CRYSTAL =
        ITEMS.registerItem("kyber_crystal", com.tweeks.starwars.item.KyberCrystalItem::new, p -> p);

    public static final DeferredItem<com.tweeks.starwars.item.SaberHiltItem> SABER_HILT =
        ITEMS.registerItem("saber_hilt", com.tweeks.starwars.item.SaberHiltItem::new, p -> p.stacksTo(1));

    public static final DeferredItem<com.tweeks.starwars.item.BlasterPistolItem> BLASTER_PISTOL =
        ITEMS.registerItem("blaster_pistol", com.tweeks.starwars.item.BlasterPistolItem::new, p -> p);

    public static final DeferredItem<com.tweeks.starwars.item.BlasterRifleItem> BLASTER_RIFLE =
        ITEMS.registerItem("blaster_rifle", com.tweeks.starwars.item.BlasterRifleItem::new, p -> p);

    public static final DeferredItem<com.tweeks.starwars.item.LightsaberItem> LIGHTSABER =
        ITEMS.registerItem("lightsaber", com.tweeks.starwars.item.LightsaberItem::new, p -> p);

    public static final DeferredItem<com.tweeks.starwars.item.HolocronItem> HOLOCRON =
        ITEMS.registerItem("holocron", com.tweeks.starwars.item.HolocronItem::new, p -> p);

    public static final DeferredItem<SpawnEggItem> STORMTROOPER_SPAWN_EGG = ITEMS.registerItem(
        "stormtrooper_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.STORMTROOPER.get()));

    public static final DeferredItem<SpawnEggItem> BATTLE_DROID_SPAWN_EGG = ITEMS.registerItem(
        "battle_droid_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.BATTLE_DROID.get()));

    public static final DeferredItem<SpawnEggItem> JEDI_KNIGHT_SPAWN_EGG = ITEMS.registerItem(
        "jedi_knight_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.JEDI_KNIGHT.get()));

    public static final DeferredItem<SpawnEggItem> DARTH_VADER_SPAWN_EGG = ITEMS.registerItem(
        "darth_vader_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.DARTH_VADER.get()));

    public static final DeferredItem<SpawnEggItem> LUKE_SKYWALKER_SPAWN_EGG = ITEMS.registerItem(
        "luke_skywalker_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.LUKE_SKYWALKER.get()));

    public static final DeferredItem<SpawnEggItem> OBI_WAN_SPAWN_EGG = ITEMS.registerItem(
        "obi_wan_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.OBI_WAN.get()));

    public static final DeferredItem<SpawnEggItem> ASTROMECH_SPAWN_EGG = ITEMS.registerItem(
        "astromech_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.ASTROMECH.get()));

    public static final DeferredItem<SpawnEggItem> BOBA_FETT_SPAWN_EGG = ITEMS.registerItem(
        "boba_fett_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.BOBA_FETT.get()));

    public static final DeferredItem<SpawnEggItem> HAN_SOLO_SPAWN_EGG = ITEMS.registerItem(
        "han_solo_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.HAN_SOLO.get()));

    public static final DeferredItem<SpawnEggItem> PRINCESS_LEIA_SPAWN_EGG = ITEMS.registerItem(
        "princess_leia_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.PRINCESS_LEIA.get()));

    public static final DeferredItem<SpawnEggItem> JAWA_SPAWN_EGG = ITEMS.registerItem(
        "jawa_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.JAWA.get()));

    public static final DeferredItem<SpawnEggItem> TUSKEN_RAIDER_SPAWN_EGG = ITEMS.registerItem(
        "tusken_raider_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.TUSKEN_RAIDER.get()));

    public static final DeferredItem<SpawnEggItem> BANTHA_SPAWN_EGG = ITEMS.registerItem(
        "bantha_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.BANTHA.get()));

    public static final DeferredItem<SpawnEggItem> REBEL_TROOPER_SPAWN_EGG = ITEMS.registerItem(
        "rebel_trooper_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.REBEL_TROOPER.get()));

    public static final DeferredItem<SpawnEggItem> PROBE_DROID_SPAWN_EGG = ITEMS.registerItem(
        "probe_droid_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.PROBE_DROID.get()));

    public static final DeferredItem<SpawnEggItem> WAMPA_SPAWN_EGG = ITEMS.registerItem(
        "wampa_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.WAMPA.get()));

    public static final DeferredItem<SpawnEggItem> TAUNTAUN_SPAWN_EGG = ITEMS.registerItem(
        "tauntaun_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.TAUNTAUN.get()));

    public static final DeferredItem<SpawnEggItem> SNOWTROOPER_SPAWN_EGG = ITEMS.registerItem(
        "snowtrooper_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.SNOWTROOPER.get()));

    public static final DeferredItem<SpawnEggItem> DRAGONSNAKE_SPAWN_EGG = ITEMS.registerItem(
        "dragonsnake_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.DRAGONSNAKE.get()));

    public static final DeferredItem<SpawnEggItem> BOGWING_SPAWN_EGG = ITEMS.registerItem(
        "bogwing_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.BOGWING.get()));

    public static final DeferredItem<SpawnEggItem> YODA_SPAWN_EGG = ITEMS.registerItem(
        "yoda_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.YODA.get()));

    public static final DeferredItem<SpawnEggItem> AT_AT_SPAWN_EGG = ITEMS.registerItem(
        "at_at_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.AT_AT.get()));

    public static final DeferredItem<SpawnEggItem> BAND_DROID_SPAWN_EGG = ITEMS.registerItem(
        "band_droid_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.BAND_DROID.get()));

    public static final DeferredItem<SpawnEggItem> CHEWBACCA_SPAWN_EGG = ITEMS.registerItem(
        "chewbacca_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.CHEWBACCA.get()));

    public static final DeferredItem<SpawnEggItem> GROGU_SPAWN_EGG = ITEMS.registerItem(
        "grogu_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.GROGU.get()));

    public static final DeferredItem<Item> STORMTROOPER_HELMET = ITEMS.registerItem("stormtrooper_helmet",
        Item::new,
        p -> p.humanoidArmor(com.tweeks.starwars.item.StormtrooperArmorMaterials.STORMTROOPER,
                net.minecraft.world.item.equipment.ArmorType.HELMET)
              .stacksTo(1));

    public static final DeferredItem<Item> STORMTROOPER_CHESTPLATE = ITEMS.registerItem("stormtrooper_chestplate",
        Item::new,
        p -> p.humanoidArmor(com.tweeks.starwars.item.StormtrooperArmorMaterials.STORMTROOPER,
                net.minecraft.world.item.equipment.ArmorType.CHESTPLATE)
              .stacksTo(1));

    public static final DeferredItem<Item> STORMTROOPER_LEGGINGS = ITEMS.registerItem("stormtrooper_leggings",
        Item::new,
        p -> p.humanoidArmor(com.tweeks.starwars.item.StormtrooperArmorMaterials.STORMTROOPER,
                net.minecraft.world.item.equipment.ArmorType.LEGGINGS)
              .stacksTo(1));

    public static final DeferredItem<Item> STORMTROOPER_BOOTS = ITEMS.registerItem("stormtrooper_boots",
        Item::new,
        p -> p.humanoidArmor(com.tweeks.starwars.item.StormtrooperArmorMaterials.STORMTROOPER,
                net.minecraft.world.item.equipment.ArmorType.BOOTS)
              .stacksTo(1));

    public static final DeferredItem<com.tweeks.starwars.item.LandspeederItem> LANDSPEEDER =
        ITEMS.registerItem("landspeeder", com.tweeks.starwars.item.LandspeederItem::new, p -> p);

    public static final DeferredItem<com.tweeks.starwars.item.VehicleDeployItem> SPEEDER_BIKE =
        ITEMS.registerItem("speeder_bike",
            p -> new com.tweeks.starwars.item.VehicleDeployItem(p, ModEntities.SPEEDER_BIKE), p -> p);

    public static final DeferredItem<com.tweeks.starwars.item.VehicleDeployItem> XWING =
        ITEMS.registerItem("xwing",
            p -> new com.tweeks.starwars.item.VehicleDeployItem(p, ModEntities.XWING), p -> p);

    public static final DeferredItem<com.tweeks.starwars.item.VehicleDeployItem> TIE_FIGHTER =
        ITEMS.registerItem("tie_fighter",
            p -> new com.tweeks.starwars.item.VehicleDeployItem(p, ModEntities.TIE_FIGHTER), p -> p);

    public static final DeferredItem<Item> HAN_SOLO_HELMET = ITEMS.registerItem("han_solo_helmet",
        Item::new,
        p -> p.humanoidArmor(com.tweeks.starwars.item.HanSoloArmorMaterials.HAN_SOLO,
                net.minecraft.world.item.equipment.ArmorType.HELMET)
              .stacksTo(1)
              .fireResistant());

    public static final DeferredItem<Item> HAN_SOLO_CHESTPLATE = ITEMS.registerItem("han_solo_chestplate",
        Item::new,
        p -> p.humanoidArmor(com.tweeks.starwars.item.HanSoloArmorMaterials.HAN_SOLO,
                net.minecraft.world.item.equipment.ArmorType.CHESTPLATE)
              .stacksTo(1)
              .fireResistant());

    public static final DeferredItem<Item> HAN_SOLO_LEGGINGS = ITEMS.registerItem("han_solo_leggings",
        Item::new,
        p -> p.humanoidArmor(com.tweeks.starwars.item.HanSoloArmorMaterials.HAN_SOLO,
                net.minecraft.world.item.equipment.ArmorType.LEGGINGS)
              .stacksTo(1)
              .fireResistant());

    public static final DeferredItem<Item> HAN_SOLO_BOOTS = ITEMS.registerItem("han_solo_boots",
        Item::new,
        p -> p.humanoidArmor(com.tweeks.starwars.item.HanSoloArmorMaterials.HAN_SOLO,
                net.minecraft.world.item.equipment.ArmorType.BOOTS)
              .stacksTo(1)
              .fireResistant());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> STARWARS_TAB =
        CREATIVE_TABS.register("main", () ->
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + StarWarsMod.MOD_ID))
                .icon(() -> BLASTER_PISTOL.get().getDefaultInstance())
                .displayItems((params, output) -> {
                    output.accept(BLASTER_PISTOL.get());
                    output.accept(BLASTER_RIFLE.get());
                    for (com.tweeks.starwars.item.SaberColor color : com.tweeks.starwars.item.SaberColor.values()) {
                        output.accept(com.tweeks.starwars.item.LightsaberItem.stackWithColor(color));
                    }
                    output.accept(STORMTROOPER_SPAWN_EGG.get());
                    output.accept(BATTLE_DROID_SPAWN_EGG.get());
                    output.accept(JEDI_KNIGHT_SPAWN_EGG.get());
                    output.accept(DARTH_VADER_SPAWN_EGG.get());
                    output.accept(LUKE_SKYWALKER_SPAWN_EGG.get());
                    output.accept(OBI_WAN_SPAWN_EGG.get());
                    output.accept(ASTROMECH_SPAWN_EGG.get());
                    output.accept(BOBA_FETT_SPAWN_EGG.get());
                    output.accept(HAN_SOLO_SPAWN_EGG.get());
                    output.accept(PRINCESS_LEIA_SPAWN_EGG.get());
                    output.accept(STORMTROOPER_HELMET.get());
                    output.accept(STORMTROOPER_CHESTPLATE.get());
                    output.accept(STORMTROOPER_LEGGINGS.get());
                    output.accept(STORMTROOPER_BOOTS.get());
                    output.accept(HOLOCRON.get());
                    output.accept(LANDSPEEDER.get());
                    output.accept(HAN_SOLO_HELMET.get());
                    output.accept(HAN_SOLO_CHESTPLATE.get());
                    output.accept(HAN_SOLO_LEGGINGS.get());
                    output.accept(HAN_SOLO_BOOTS.get());
                    output.accept(STAR_COMPASS.get());
                    output.accept(CANTINA_RECORD.get());
                    output.accept(GALAXY_MAP.get());
                    output.accept(SPEEDER_BIKE.get());
                    output.accept(XWING.get());
                    output.accept(TIE_FIGHTER.get());
                    output.accept(BLUE_KYBER_ORE.get());
                    output.accept(GREEN_KYBER_ORE.get());
                    output.accept(PURPLE_KYBER_ORE.get());
                    output.accept(SABER_HILT.get());
                    for (com.tweeks.starwars.item.SaberColor color : com.tweeks.starwars.item.SaberColor.values()) {
                        output.accept(com.tweeks.starwars.item.KyberCrystalItem.withColor(color));
                    }
                    output.accept(JAWA_SPAWN_EGG.get());
                    output.accept(TUSKEN_RAIDER_SPAWN_EGG.get());
                    output.accept(BANTHA_SPAWN_EGG.get());
                    output.accept(REBEL_TROOPER_SPAWN_EGG.get());
                    output.accept(PROBE_DROID_SPAWN_EGG.get());
                    output.accept(WAMPA_SPAWN_EGG.get());
                    output.accept(TAUNTAUN_SPAWN_EGG.get());
                    output.accept(SNOWTROOPER_SPAWN_EGG.get());
                    output.accept(DRAGONSNAKE_SPAWN_EGG.get());
                    output.accept(BOGWING_SPAWN_EGG.get());
                    output.accept(YODA_SPAWN_EGG.get());
                    output.accept(AT_AT_SPAWN_EGG.get());
                    output.accept(BAND_DROID_SPAWN_EGG.get());
                    output.accept(CHEWBACCA_SPAWN_EGG.get());
                    output.accept(GROGU_SPAWN_EGG.get());
                    // Later tasks append their items here.
                })
                .build());

    public static void register(IEventBus modEventBus) {
        // Blocks first so BlockItems (if any ever exist) can resolve them.
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
