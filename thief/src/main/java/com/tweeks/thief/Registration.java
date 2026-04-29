package com.tweeks.thief;

import com.tweeks.thief.entity.ThiefEntity;
import com.tweeks.thief.item.BlackjackItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Registration {
    private Registration() {}

    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(ThiefMod.MOD_ID);

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, ThiefMod.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ThiefMod.MOD_ID);

    public static final DeferredItem<BlackjackItem> BLACKJACK = ITEMS.registerItem("blackjack",
        BlackjackItem::new,
        p -> p);

    public static final DeferredHolder<EntityType<?>, EntityType<ThiefEntity>> THIEF =
        ENTITY_TYPES.register("thief", () -> EntityType.Builder.<ThiefEntity>of(
                ThiefEntity::new, MobCategory.MONSTER)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(ThiefMod.MOD_ID, "thief"))));

    public static final DeferredItem<SpawnEggItem> THIEF_SPAWN_EGG = ITEMS.registerItem("thief_spawn_egg",
        SpawnEggItem::new,
        p -> p.spawnEgg(THIEF.get()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> THIEF_TAB =
        CREATIVE_TABS.register("main", () ->
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + ThiefMod.MOD_ID))
                .icon(() -> BLACKJACK.get().getDefaultInstance())
                .displayItems((params, output) -> {
                    output.accept(BLACKJACK.get());
                    output.accept(THIEF_SPAWN_EGG.get());
                })
                .build());

    public static void register(IEventBus modEventBus) {
        // Entity types must register before items so SpawnEggItem can resolve THIEF.get().
        ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
