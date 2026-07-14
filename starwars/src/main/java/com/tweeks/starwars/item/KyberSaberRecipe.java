package com.tweeks.starwars.item;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Build-your-own lightsaber: put a {@link SaberHiltItem hilt} and a
 * {@link KyberCrystalItem kyber crystal} anywhere in the crafting grid and
 * get a lightsaber whose blade matches the crystal — including a Sith-red
 * blade from a bled crystal. A special (shapeless, component-aware) recipe
 * because the output's blade colour is copied from an input's data component,
 * which a plain shaped recipe cannot express.
 */
public class KyberSaberRecipe extends CustomRecipe {
    public static final MapCodec<KyberSaberRecipe> CODEC = MapCodec.unit(KyberSaberRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, KyberSaberRecipe> STREAM_CODEC =
        StreamCodec.unit(new KyberSaberRecipe());
    public static final RecipeSerializer<KyberSaberRecipe> SERIALIZER =
        new RecipeSerializer<>(CODEC, STREAM_CODEC);

    /** Scans a grid into counts, then applies {@link SaberAssembly#evaluate}. */
    public static SaberAssembly.Result assemble(Iterable<ItemStack> grid) {
        int hilts = 0;
        int crystals = 0;
        int others = 0;
        SaberColor color = SaberColor.BLUE;
        for (ItemStack stack : grid) {
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof SaberHiltItem) {
                hilts++;
            } else if (stack.getItem() instanceof KyberCrystalItem) {
                crystals++;
                color = KyberCrystalItem.colorOf(stack);
            } else {
                others++;
            }
        }
        return SaberAssembly.evaluate(hilts, crystals, others, color);
    }

    public KyberSaberRecipe() {
    }

    private static Iterable<ItemStack> stacksOf(CraftingInput input) {
        java.util.List<ItemStack> stacks = new java.util.ArrayList<>(input.size());
        for (int i = 0; i < input.size(); i++) {
            stacks.add(input.getItem(i));
        }
        return stacks;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return assemble(stacksOf(input)).valid();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        SaberAssembly.Result result = assemble(stacksOf(input));
        if (!result.valid()) {
            return ItemStack.EMPTY;
        }
        return LightsaberItem.stackWithColor(result.color());
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}
