package com.tweeks.wildwest;

import com.tweeks.wildwest.ModBlocks;
import com.tweeks.wildwest.item.AgentSpawnEggItem;
import com.tweeks.wildwest.item.BanditKnifeItem;
import com.tweeks.wildwest.item.BillyClubItem;
import com.tweeks.wildwest.item.CaptainPistolItem;
import com.tweeks.wildwest.item.CursedTomeItem;
import com.tweeks.wildwest.item.FlintlockPistolItem;
import com.tweeks.wildwest.item.HerobrineSpawnEggItem;
import com.tweeks.wildwest.item.MeteorStaffItem;
import com.tweeks.wildwest.item.ReaperScytheItem;
import com.tweeks.wildwest.item.NullSpawnEggItem;
import com.tweeks.wildwest.item.PistolItem;
import com.tweeks.wildwest.item.RapierItem;
import com.tweeks.wildwest.item.RifleItem;
import com.tweeks.wildwest.item.TaintedVialItem;
import com.tweeks.wildwest.item.VoidMarkItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Registration {
    private Registration() {}

    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(WildWestMod.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WildWestMod.MOD_ID);

    public static final DeferredItem<PistolItem> PISTOL = ITEMS.registerItem(
        "pistol", PistolItem::new, p -> p);

    public static final DeferredItem<RifleItem> RIFLE = ITEMS.registerItem(
        "rifle", RifleItem::new, p -> p);

    public static final DeferredItem<BillyClubItem> BILLY_CLUB = ITEMS.registerItem(
        "billy_club", BillyClubItem::new, p -> p);

    public static final DeferredItem<BanditKnifeItem> BANDIT_KNIFE = ITEMS.registerItem(
        "bandit_knife", BanditKnifeItem::new, p -> p);

    public static final DeferredItem<SpawnEggItem> DEPUTY_SPAWN_EGG = ITEMS.registerItem(
        "deputy_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.DEPUTY.get()));

    public static final DeferredItem<SpawnEggItem> SHERRIF_SPAWN_EGG = ITEMS.registerItem(
        "sherrif_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.SHERRIF.get()));

    public static final DeferredItem<SpawnEggItem> BANDIT_SPAWN_EGG = ITEMS.registerItem(
        "bandit_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.BANDIT.get()));

    public static final DeferredItem<SpawnEggItem> BANDIT_LEADER_SPAWN_EGG = ITEMS.registerItem(
        "bandit_leader_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.BANDIT_LEADER.get()));

    public static final DeferredItem<SpawnEggItem> WALKER_SPAWN_EGG = ITEMS.registerItem(
        "walker_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.WALKER.get()));

    public static final DeferredItem<SpawnEggItem> STEVE_STACKER_SPAWN_EGG = ITEMS.registerItem(
        "steve_stacker_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.STEVE_STACKER.get()));

    public static final DeferredItem<HerobrineSpawnEggItem> HEROBRINE_SPAWN_EGG = ITEMS.registerItem(
        "herobrine_spawn_egg",
        HerobrineSpawnEggItem::new,
        p -> p.spawnEgg(ModEntities.HEROBRINE.get()));

    public static final DeferredItem<AgentSpawnEggItem> AGENT_SPAWN_EGG = ITEMS.registerItem(
        "the_agent_spawn_egg",
        AgentSpawnEggItem::new,
        p -> p.spawnEgg(ModEntities.AGENT.get()));

    public static final DeferredItem<NullSpawnEggItem> NULL_SPAWN_EGG = ITEMS.registerItem(
        "null_spawn_egg",
        NullSpawnEggItem::new,
        p -> p.spawnEgg(ModEntities.NULL.get()));

    public static final DeferredItem<CursedTomeItem> CURSED_TOME = ITEMS.registerItem(
        "cursed_tome",
        CursedTomeItem::new,
        p -> p.stacksTo(1).durability(CursedTomeItem.MAX_USES).rarity(Rarity.EPIC));

    public static final DeferredItem<VoidMarkItem> VOID_MARK = ITEMS.registerItem(
        "void_mark",
        VoidMarkItem::new,
        p -> p.stacksTo(16).rarity(Rarity.EPIC));

    public static final DeferredItem<MeteorStaffItem> METEOR_STAFF = ITEMS.registerItem(
        "meteor_staff",
        MeteorStaffItem::new,
        p -> p.stacksTo(1).rarity(Rarity.EPIC));

    public static final DeferredItem<ReaperScytheItem> REAPER_SCYTHE = ITEMS.registerItem(
        "reaper_scythe", ReaperScytheItem::new, p -> p);

    public static final DeferredItem<TaintedVialItem> TAINTED_VIAL = ITEMS.registerItem(
        "tainted_vial", TaintedVialItem::new, p -> p);

    public static final DeferredItem<RapierItem> RAPIER = ITEMS.registerItem(
        "rapier", RapierItem::new, p -> p);

    public static final DeferredItem<FlintlockPistolItem> FLINTLOCK_PISTOL = ITEMS.registerItem(
        "flintlock_pistol", FlintlockPistolItem::new, p -> p);

    public static final DeferredItem<CaptainPistolItem> CAPTAIN_PISTOL = ITEMS.registerItem(
        "captain_pistol", CaptainPistolItem::new, p -> p.rarity(Rarity.RARE));

    public static final DeferredItem<BlockItem> CANNON = ITEMS.registerItem(
        "cannon",
        properties -> new BlockItem(ModBlocks.CANNON.get(), properties),
        p -> p);

    public static final DeferredItem<SpawnEggItem> PIRATE_SPAWN_EGG = ITEMS.registerItem(
        "pirate_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.PIRATE.get()));

    public static final DeferredItem<SpawnEggItem> SKELETON_PIRATE_SPAWN_EGG = ITEMS.registerItem(
        "skeleton_pirate_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.SKELETON_PIRATE.get()));

    public static final DeferredItem<SpawnEggItem> PIRATE_CAPTAIN_SPAWN_EGG = ITEMS.registerItem(
        "pirate_captain_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.PIRATE_CAPTAIN.get()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WILDWEST_TAB =
        CREATIVE_TABS.register("main", () ->
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + WildWestMod.MOD_ID))
                .icon(() -> PISTOL.get().getDefaultInstance())
                .displayItems((params, output) -> {
                    output.accept(PISTOL.get());
                    output.accept(RIFLE.get());
                    output.accept(BILLY_CLUB.get());
                    output.accept(BANDIT_KNIFE.get());
                    output.accept(DEPUTY_SPAWN_EGG.get());
                    output.accept(SHERRIF_SPAWN_EGG.get());
                    output.accept(BANDIT_SPAWN_EGG.get());
                    output.accept(BANDIT_LEADER_SPAWN_EGG.get());
                    output.accept(WALKER_SPAWN_EGG.get());
                    output.accept(STEVE_STACKER_SPAWN_EGG.get());
                    output.accept(HEROBRINE_SPAWN_EGG.get());
                    output.accept(AGENT_SPAWN_EGG.get());
                    output.accept(NULL_SPAWN_EGG.get());
                    output.accept(VOID_MARK.get());
                    output.accept(CURSED_TOME.get());
                    output.accept(METEOR_STAFF.get());
                    output.accept(REAPER_SCYTHE.get());
                    output.accept(TAINTED_VIAL.get());
                    output.accept(RAPIER.get());
                    output.accept(FLINTLOCK_PISTOL.get());
                    output.accept(CAPTAIN_PISTOL.get());
                    output.accept(CANNON.get());
                    output.accept(PIRATE_SPAWN_EGG.get());
                    output.accept(SKELETON_PIRATE_SPAWN_EGG.get());
                    output.accept(PIRATE_CAPTAIN_SPAWN_EGG.get());
                })
                .build());

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
