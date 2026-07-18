package com.tweeks.starwars.item;

import com.tweeks.starwars.faction.Alignment;
import com.tweeks.starwars.faction.AlignmentEvents;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * A kyber crystal, colour carried in {@link ModDataComponents#KYBER_COLOR}.
 * Combine one with a saber hilt in a crafting grid (see {@link KyberSaberRecipe})
 * to build a lightsaber of the same colour. A dark-aligned player can "bleed"
 * any crystal to red (Sith) by holding it and using it — the light-side
 * refuses.
 */
public class KyberCrystalItem extends Item {

    /** Alignment at or below which a player can bleed a crystal to red. */
    public static final int DARK_BLEED_THRESHOLD = -Alignment.HOSTILE_THRESHOLD;

    public KyberCrystalItem(Properties properties) {
        super(properties);
    }

    public static SaberColor colorOf(ItemStack stack) {
        return SaberColor.byIndex(stack.getOrDefault(ModDataComponents.KYBER_COLOR.get(), 0));
    }

    public static ItemStack withColor(SaberColor color) {
        ItemStack stack = new ItemStack(com.tweeks.starwars.Registration.KYBER_CRYSTAL.get());
        stack.set(ModDataComponents.KYBER_COLOR.get(), color.ordinal());
        return stack;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (colorOf(stack) == SaberColor.RED) {
            return InteractionResult.PASS; // already bled
        }
        if (level.isClientSide() || !(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        int score = AlignmentEvents.getScore(player);
        if (score > DARK_BLEED_THRESHOLD) {
            serverPlayer.sendSystemMessage(Component.translatable("starwars.kyber.resists"), true);
            return InteractionResult.CONSUME;
        }
        // Bleed a single crystal Sith red, leaving the rest of the stack pure.
        ItemStack bled = stack.split(1);
        bled.set(ModDataComponents.KYBER_COLOR.get(), SaberColor.RED.ordinal());
        if (stack.isEmpty()) {
            player.setItemInHand(hand, bled);
        } else if (!player.getInventory().add(bled)) {
            player.drop(bled, false);
        }
        ((ServerLevel) level).sendParticles(ParticleTypes.CRIMSON_SPORE,
            player.getX(), player.getEyeY(), player.getZ(), 20, 0.3, 0.3, 0.3, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK,
            SoundSource.PLAYERS, 0.8F, 0.6F);
        serverPlayer.sendSystemMessage(Component.translatable("starwars.kyber.bled"), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        SaberColor color = colorOf(stack);
        tooltip.accept(Component.translatable("starwars.kyber.color." + color.suffix())
            .withColor(color.argb() & 0xFFFFFF));
        if (color != SaberColor.RED) {
            tooltip.accept(Component.translatable("starwars.kyber.hint_bleed").withColor(0xFF8080));
        }
    }
}
