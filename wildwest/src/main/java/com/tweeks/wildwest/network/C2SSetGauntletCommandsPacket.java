package com.tweeks.wildwest.network;

import com.mojang.logging.LogUtils;
import com.tweeks.wildwest.Registration;
import com.tweeks.wildwest.WildWestMod;
import com.tweeks.wildwest.item.InfinityCommands;
import com.tweeks.wildwest.item.ModDataComponents;
import java.util.List;
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

/**
 * Client → server: the player closed the {@code GauntletEditorScreen}
 * and is saving updated command strings for the held gauntlet.
 *
 * <p>Payload limits: 6 strings, each capped at 256 chars on the wire to
 * match the screen's EditBox max length. Out-of-spec payloads are
 * dropped (logged at debug) to bound resource use from misbehaving
 * clients.
 */
public record C2SSetGauntletCommandsPacket(boolean mainHand, List<String> commands)
        implements CustomPacketPayload {

    public static final int MAX_COMMAND_LENGTH = 256;

    public static final Type<C2SSetGauntletCommandsPacket> TYPE =
        new Type<>(Identifier.fromNamespaceAndPath(WildWestMod.MOD_ID, "set_gauntlet_commands"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SSetGauntletCommandsPacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.BOOL, C2SSetGauntletCommandsPacket::mainHand,
            ByteBufCodecs.stringUtf8(MAX_COMMAND_LENGTH).apply(ByteBufCodecs.list(InfinityCommands.SLOT_COUNT)),
            C2SSetGauntletCommandsPacket::commands,
            C2SSetGauntletCommandsPacket::new);

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(C2SSetGauntletCommandsPacket pkt, IPayloadContext ctx) {
        if (pkt.commands() == null || pkt.commands().size() > InfinityCommands.SLOT_COUNT) {
            LOGGER.debug("Dropping C2SSetGauntletCommandsPacket with bad command list size={}",
                pkt.commands() == null ? "null" : pkt.commands().size());
            return;
        }
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                LOGGER.warn("C2SSetGauntletCommandsPacket received without a ServerPlayer context");
                return;
            }
            InteractionHand hand = pkt.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(Registration.INFINITY_GAUNTLET.get())) {
                // Player swapped hands / dropped item between opening the
                // editor and clicking Done. Silent drop.
                return;
            }
            stack.set(ModDataComponents.COMMANDS.get(), InfinityCommands.normalize(pkt.commands()));
        });
    }
}
