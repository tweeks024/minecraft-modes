package com.tweeks.wildwest.network;

import com.mojang.logging.LogUtils;
import com.tweeks.wildwest.Registration;
import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.item.InfinityCooldowns;
import com.tweeks.wildwest.item.InfinityStone;
import com.tweeks.wildwest.item.ModDataComponents;
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

public record C2SSetActiveStonePacket(int stoneIndex, boolean mainHand) implements CustomPacketPayload {

    public static final Type<C2SSetActiveStonePacket> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "set_active_stone"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSetActiveStonePacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, C2SSetActiveStonePacket::stoneIndex,
            ByteBufCodecs.BOOL,    C2SSetActiveStonePacket::mainHand,
            C2SSetActiveStonePacket::new);

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(C2SSetActiveStonePacket pkt, IPayloadContext ctx) {
        int max = InfinityStone.values().length - 1;
        if (pkt.stoneIndex() < 0 || pkt.stoneIndex() > max) {
            LOGGER.debug("Dropping C2SSetActiveStonePacket with out-of-range stoneIndex={}", pkt.stoneIndex());
            return;
        }
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                LOGGER.warn("C2SSetActiveStonePacket received without a ServerPlayer context");
                return;
            }
            InteractionHand hand = pkt.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(Registration.INFINITY_GAUNTLET.get())) {
                // Plausible race: player swapped hands or dropped item between
                // opening the picker and clicking. Silent drop is correct.
                return;
            }
            stack.set(ModDataComponents.ACTIVE_STONE.get(), pkt.stoneIndex());

            // Re-sync the vanilla hotbar cooldown sweep to the newly-active
            // stone. If the stone is mid-cooldown, set the sweep to the
            // remaining ticks; otherwise clear the sweep entirely. The true
            // per-stone cooldown gate still lives in the COOLDOWNS data
            // component (read in InfinityGauntletItem.use), so vanilla
            // ItemCooldowns is purely a visual mirror.
            java.util.List<Long> cds = stack.getOrDefault(
                ModDataComponents.COOLDOWNS.get(), InfinityCooldowns.emptyCooldowns());
            long now = player.level().getGameTime();
            if (InfinityCooldowns.isOnCooldown(cds, pkt.stoneIndex(), now)) {
                int remaining = (int) Math.min(
                    InfinityCooldowns.getExpiry(cds, pkt.stoneIndex()) - now, Integer.MAX_VALUE);
                player.getCooldowns().addCooldown(stack, remaining);
            } else {
                player.getCooldowns().removeCooldown(
                    player.getCooldowns().getCooldownGroup(stack));
            }
        });
    }
}
