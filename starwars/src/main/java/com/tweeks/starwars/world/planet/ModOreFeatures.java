package com.tweeks.starwars.world.planet;

import java.util.List;

import com.tweeks.starwars.Registration;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

/**
 * Kyber ore worldgen: one coloured ore per world, salted through the deep
 * stone so mining a planet earns the blade colour it hides. Datagen only.
 */
public final class ModOreFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> BLUE_ORE = configured("blue_kyber_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> GREEN_ORE = configured("green_kyber_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PURPLE_ORE = configured("purple_kyber_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> RED_ORE = configured("red_kyber_ore");

    public static final ResourceKey<PlacedFeature> BLUE_ORE_PLACED = placed("blue_kyber_ore");
    public static final ResourceKey<PlacedFeature> GREEN_ORE_PLACED = placed("green_kyber_ore");
    public static final ResourceKey<PlacedFeature> PURPLE_ORE_PLACED = placed("purple_kyber_ore");
    public static final ResourceKey<PlacedFeature> RED_ORE_PLACED = placed("red_kyber_ore");

    private static final RuleTest STONE = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
    private static final RuleTest DEEPSLATE = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);

    private ModOreFeatures() {
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> configured(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.fromNamespaceAndPath("starwars", name));
    }

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath("starwars", name));
    }

    public static void bootstrapConfigured(BootstrapContext<ConfiguredFeature<?, ?>> ctx) {
        register(ctx, BLUE_ORE, Registration.BLUE_KYBER_ORE.get());
        register(ctx, GREEN_ORE, Registration.GREEN_KYBER_ORE.get());
        register(ctx, PURPLE_ORE, Registration.PURPLE_KYBER_ORE.get());
        register(ctx, RED_ORE, Registration.RED_KYBER_ORE.get());
    }

    private static void register(BootstrapContext<ConfiguredFeature<?, ?>> ctx,
                                 ResourceKey<ConfiguredFeature<?, ?>> key, Block ore) {
        ctx.register(key, new ConfiguredFeature<>(Feature.ORE, new OreConfiguration(List.of(
            OreConfiguration.target(STONE, ore.defaultBlockState()),
            OreConfiguration.target(DEEPSLATE, ore.defaultBlockState())
        ), 5)));
    }

    public static void bootstrapPlaced(BootstrapContext<PlacedFeature> ctx) {
        HolderGetter<ConfiguredFeature<?, ?>> configured = ctx.lookup(Registries.CONFIGURED_FEATURE);
        register(ctx, BLUE_ORE_PLACED, configured.getOrThrow(BLUE_ORE));
        register(ctx, GREEN_ORE_PLACED, configured.getOrThrow(GREEN_ORE));
        register(ctx, PURPLE_ORE_PLACED, configured.getOrThrow(PURPLE_ORE));
        register(ctx, RED_ORE_PLACED, configured.getOrThrow(RED_ORE));
    }

    private static void register(BootstrapContext<PlacedFeature> ctx, ResourceKey<PlacedFeature> key,
                                 net.minecraft.core.Holder<ConfiguredFeature<?, ?>> feature) {
        // Uncommon rich seams down in the deep stone.
        List<PlacementModifier> modifiers = List.of(
            CountPlacement.of(UniformInt.of(2, 5)),
            InSquarePlacement.spread(),
            HeightRangePlacement.triangle(VerticalAnchor.absolute(-56), VerticalAnchor.absolute(40)),
            BiomeFilter.biome());
        ctx.register(key, new PlacedFeature(feature, modifiers));
    }
}
