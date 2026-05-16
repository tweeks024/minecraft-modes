package com.tweeks.wildwest;

import com.tweeks.wildwest.block.CannonBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS =
        DeferredRegister.createBlocks(WildWestMod.MOD_ID);

    public static final DeferredBlock<CannonBlock> CANNON = BLOCKS.register("cannon",
        () -> new CannonBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(3.5f, 6.0f)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
            .setId(ResourceKey.create(Registries.BLOCK,
                Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "cannon")))));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
