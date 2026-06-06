package com.tweeks.wildwest.network;

import com.tweeks.wildwest.Registration;
import com.tweeks.wildwest.WildWestMod;
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

public record C2SSetActiveStonePacket(int stoneIndex, boolean mainHand) implements CustomPacketPayload {

    public static final Type<C2SSetActiveStonePacket> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "set_active_stone"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSetActiveStonePacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT, C2SSetActiveStonePacket::stoneIndex,
            ByteBufCodecs.BOOL,    C2SSetActiveStonePacket::mainHand,
            C2SSetActiveStonePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(C2SSetActiveStonePacket pkt, IPayloadContext ctx) {
        if (pkt.stoneIndex() < 0 || pkt.stoneIndex() > 5) return;
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            InteractionHand hand = pkt.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(Registration.INFINITY_GAUNTLET.get())) return;
            stack.set(ModDataComponents.ACTIVE_STONE.get(), pkt.stoneIndex());
        });
    }
}
