package com.tweeks.starwars.item;

import com.tweeks.starwars.StarWarsMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Custom crafting recipe serializers. */
public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, StarWarsMod.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<KyberSaberRecipe>> KYBER_SABER =
        SERIALIZERS.register("kyber_saber", () -> KyberSaberRecipe.SERIALIZER);

    private ModRecipeSerializers() {
    }

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }
}
