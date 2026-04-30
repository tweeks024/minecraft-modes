package com.tweeks.wildwest;

import com.tweeks.wildwest.item.BanditKnifeItem;
import com.tweeks.wildwest.item.BillyClubItem;
import com.tweeks.wildwest.item.PistolItem;
import com.tweeks.wildwest.item.RifleItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
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
                })
                .build());

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
