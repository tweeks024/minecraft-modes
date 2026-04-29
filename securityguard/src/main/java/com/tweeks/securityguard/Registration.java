package com.tweeks.securityguard;

import com.tweeks.securityguard.entity.SecurityGuardEntity;
import com.tweeks.securityguard.item.GuardHelmetItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Registration {
    private Registration() {}

    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems(SecurityGuardMod.MOD_ID);

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, SecurityGuardMod.MOD_ID);

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(Registries.SOUND_EVENT, SecurityGuardMod.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SecurityGuardMod.MOD_ID);

    public static final DeferredItem<GuardHelmetItem> GUARD_HELMET = ITEMS.register("guard_helmet",
        () -> new GuardHelmetItem(new Item.Properties().stacksTo(64)));

    public static final DeferredHolder<EntityType<?>, EntityType<SecurityGuardEntity>> SECURITY_GUARD =
        ENTITY_TYPES.register("guard", () -> EntityType.Builder.<SecurityGuardEntity>of(
                SecurityGuardEntity::new, MobCategory.MISC)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(SecurityGuardMod.MOD_ID, "guard"))));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SECURITY_GUARD_TAB =
        CREATIVE_TABS.register("main", () ->
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + SecurityGuardMod.MOD_ID))
                .icon(() -> GUARD_HELMET.get().getDefaultInstance())
                .displayItems((params, output) -> {
                    output.accept(GUARD_HELMET.get());
                    // entity spawn egg added in Task 7
                })
                .build());

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
