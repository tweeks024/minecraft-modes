package com.tweeks.starwars.network;

import com.mojang.logging.LogUtils;
import com.tweeks.starwars.Registration;
import com.tweeks.starwars.StarWarsMod;
import com.tweeks.starwars.item.ForceCooldowns;
import com.tweeks.starwars.item.ForcePower;
import com.tweeks.starwars.item.ModDataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

public record C2SSelectPowerPacket(int powerIndex, boolean mainHand) implements CustomPacketPayload {

    public static final Type<C2SSelectPowerPacket> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(StarWarsMod.MOD_ID, "select_power"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSelectPowerPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, C2SSelectPowerPacket::powerIndex,
            ByteBufCodecs.BOOL,    C2SSelectPowerPacket::mainHand,
            C2SSelectPowerPacket::new);

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(C2SSelectPowerPacket pkt, IPayloadContext ctx) {
        int max = ForcePower.values().length - 1;
        if (pkt.powerIndex() < 0 || pkt.powerIndex() > max) {
            LOGGER.debug("Dropping C2SSelectPowerPacket with out-of-range powerIndex={}", pkt.powerIndex());
            return;
        }
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                LOGGER.warn("C2SSelectPowerPacket received without a ServerPlayer context");
                return;
            }
            InteractionHand hand = pkt.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(Registration.HOLOCRON.get())) {
                // Plausible race: player swapped hands or dropped item between
                // opening the picker and clicking. Silent drop is correct.
                return;
            }
            stack.set(ModDataComponents.ACTIVE_POWER.get(), pkt.powerIndex());

            // Re-sync the vanilla hotbar cooldown sweep to the newly-active
            // power. If the power is mid-cooldown, set the sweep to the
            // remaining ticks; otherwise clear the sweep entirely. The true
            // per-power cooldown gate still lives in the POWER_COOLDOWNS data
            // component (read in HolocronItem.use), so vanilla ItemCooldowns
            // is purely a visual mirror.
            java.util.List<Long> cds = stack.getOrDefault(
                ModDataComponents.POWER_COOLDOWNS.get(), ForceCooldowns.emptyCooldowns());
            long now = player.level().getGameTime();
            if (ForceCooldowns.isOnCooldown(cds, pkt.powerIndex(), now)) {
                int remaining = (int) Math.min(
                    ForceCooldowns.getExpiry(cds, pkt.powerIndex()) - now, Integer.MAX_VALUE);
                player.getCooldowns().addCooldown(stack, remaining);
            } else {
                player.getCooldowns().removeCooldown(
                    player.getCooldowns().getCooldownGroup(stack));
            }
        });
    }
}
