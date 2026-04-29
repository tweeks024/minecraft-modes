package com.tweeks.securityguard;

import com.tweeks.securityguard.entity.SecurityGuardEntity;
import com.tweeks.securityguard.item.BatonItem;
import com.tweeks.securityguard.item.GuardHelmetItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
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

    public static final DeferredItem<GuardHelmetItem> GUARD_HELMET = ITEMS.registerItem("guard_helmet",
        GuardHelmetItem::new,
        p -> p.stacksTo(64));

    private static final ItemAttributeModifiers BATON_ATTRIBUTES = ItemAttributeModifiers.builder()
        .add(Attributes.ATTACK_DAMAGE,
            new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 6.0, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND)
        .add(Attributes.ATTACK_SPEED,
            new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND)
        .build();

    public static final DeferredItem<BatonItem> BATON = ITEMS.registerItem("baton",
        BatonItem::new,
        p -> p.attributes(BATON_ATTRIBUTES).durability(250));

    public static final DeferredHolder<EntityType<?>, EntityType<SecurityGuardEntity>> SECURITY_GUARD =
        ENTITY_TYPES.register("guard", () -> EntityType.Builder.<SecurityGuardEntity>of(
                SecurityGuardEntity::new, MobCategory.MISC)
            .sized(0.6f, 1.95f)
            .clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(SecurityGuardMod.MOD_ID, "guard"))));

    public static final DeferredItem<SpawnEggItem> GUARD_SPAWN_EGG = ITEMS.registerItem("guard_spawn_egg",
        SpawnEggItem::new,
        p -> p.spawnEgg(SECURITY_GUARD.get()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SECURITY_GUARD_TAB =
        CREATIVE_TABS.register("main", () ->
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + SecurityGuardMod.MOD_ID))
                .icon(() -> GUARD_HELMET.get().getDefaultInstance())
                .displayItems((params, output) -> {
                    output.accept(GUARD_HELMET.get());
                    output.accept(BATON.get());
                    output.accept(GUARD_SPAWN_EGG.get());
                })
                .build());

    public static void register(IEventBus modEventBus) {
        com.tweeks.securityguard.sound.ModSounds.register(SOUND_EVENTS);
        // Entity types must register before items so SpawnEggItem can resolve SECURITY_GUARD.get().
        ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        SOUND_EVENTS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }
}
