package com.tweeks.starwars.item;

import com.tweeks.starwars.ModSounds;
import com.tweeks.starwars.faction.AlignmentEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Kyber Holocron: casts the wielder's currently-selected
 * {@link ForcePower} (Task 25's radial picker sets it via
 * {@link ModDataComponents#ACTIVE_POWER}). Mirrors wildwest's
 * {@code InfinityGauntletItem.use} cooldown/CONSUME pattern.
 */
public class HolocronItem extends Item {

    public HolocronItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)) {
            // CONSUME on the client (arm swing + immediate feedback), matching
            // the InfinityGauntletItem reference.
            return InteractionResult.CONSUME;
        }
        ForcePower power = ForcePower.byIndex(
            stack.getOrDefault(ModDataComponents.ACTIVE_POWER.get(), 0));
        List<Long> cds = stack.getOrDefault(
            ModDataComponents.POWER_COOLDOWNS.get(), ForceCooldowns.emptyCooldowns());
        long now = level.getGameTime();
        if (ForceCooldowns.isOnCooldown(cds, power.ordinal(), now)) {
            return InteractionResult.FAIL;
        }
        if (!ForcePowers.cast(power, serverPlayer, serverLevel)) {
            return InteractionResult.FAIL;   // no valid target: no cooldown burned
        }
        stack.set(ModDataComponents.POWER_COOLDOWNS.get(),
            ForceCooldowns.applyCooldown(cds, power.ordinal(), now, power.cooldownTicks()));
        // Mirror into the vanilla hotbar sweep for visual feedback.
        player.getCooldowns().addCooldown(stack, power.cooldownTicks());
        AlignmentEvents.adjustScore(serverPlayer, power.alignmentDelta());
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            (power == ForcePower.LIGHTNING ? ModSounds.FORCE_LIGHTNING_SOUND : ModSounds.FORCE_CAST).get(),
            SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResult.CONSUME;
    }
}
