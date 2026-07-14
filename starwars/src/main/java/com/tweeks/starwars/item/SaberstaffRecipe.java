package com.tweeks.starwars.item;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Forge a saberstaff: two {@link SaberHiltItem hilts} joined end to end around
 * two Sith-red kyber crystals. A special recipe because it must inspect the
 * crystals' colour component, which a plain shaped recipe cannot.
 */
public class SaberstaffRecipe extends CustomRecipe {
    public static final MapCodec<SaberstaffRecipe> CODEC = MapCodec.unit(SaberstaffRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, SaberstaffRecipe> STREAM_CODEC =
        StreamCodec.unit(new SaberstaffRecipe());
    public static final RecipeSerializer<SaberstaffRecipe> SERIALIZER =
        new RecipeSerializer<>(CODEC, STREAM_CODEC);

    public SaberstaffRecipe() {
    }

    private static boolean matchesGrid(CraftingInput input) {
        int hilts = 0;
        int redCrystals = 0;
        int otherCrystals = 0;
        int others = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof SaberHiltItem) {
                hilts++;
            } else if (stack.getItem() instanceof KyberCrystalItem) {
                if (KyberCrystalItem.colorOf(stack) == SaberColor.RED) {
                    redCrystals++;
                } else {
                    otherCrystals++;
                }
            } else {
                others++;
            }
        }
        return SaberstaffAssembly.valid(hilts, redCrystals, otherCrystals, others);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return matchesGrid(input);
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return matchesGrid(input)
            ? new ItemStack(com.tweeks.starwars.Registration.SABERSTAFF.get())
            : ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}
