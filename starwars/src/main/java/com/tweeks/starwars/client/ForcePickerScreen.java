package com.tweeks.starwars.client;

import com.mojang.logging.LogUtils;
import com.tweeks.starwars.Registration;
import com.tweeks.starwars.item.ForceCooldowns;
import com.tweeks.starwars.item.ForcePower;
import com.tweeks.starwars.item.ModDataComponents;
import com.tweeks.starwars.network.C2SSelectPowerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.slf4j.Logger;

/**
 * Radial picker for the Holocron's five Force powers. Press the keybind
 * to open, hover a wedge, click or close to select. The selected power is
 * sent to the server via {@link C2SSelectPowerPacket}.
 *
 * <p>MC 26.1.2 replaced the legacy {@code GuiGraphics} render pipeline
 * with {@link GuiGraphicsExtractor} — custom screens override
 * {@code extractRenderState(...)} and use {@code fill}/{@code text}
 * primitives on the extractor.
 */
public class ForcePickerScreen extends Screen {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final double DEADZONE_PX = 18.0;
    private static final int OUTER_RADIUS_PX = 90;
    private static final int DISC_HALF_PX = 12;
    private static final int WEDGE_COUNT = 5;

    private static final int[] WEDGE_COLORS = {
        0xFF8090A0, // PUSH
        0xFF60A090, // PULL
        0xFF70C070, // LEAP
        0xFFC0A040, // MIND_TRICK
        0xFF9040C0, // LIGHTNING
    };

    private final boolean mainHand;
    private int hoveredWedge = -1;
    private boolean selectionSent = false;

    public ForcePickerScreen(boolean mainHand) {
        super(Component.translatable("screen.starwars.force_radial"));
        this.mainHand = mainHand;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean isInGameUi() {
        // Lighter "in-game" rendering path: just the transparent dim instead
        // of the full menu background + blur. Keeps the world visible behind
        // the picker which is the expected look for a radial selector.
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int cx = this.width / 2;
        int cy = this.height / 2;
        this.hoveredWedge = SwRadialMath.wedgeFromMouse(mouseX, mouseY, cx, cy, DEADZONE_PX, WEDGE_COUNT);

        graphics.fill(0, 0, this.width, this.height, 0x80000000);

        java.util.List<Long> cds = readCooldowns();
        long now = currentGameTime();

        for (int i = 0; i < WEDGE_COUNT; i++) {
            ForcePower power = ForcePower.byIndex(i);
            double angle = -Math.PI / 2 + i * (Math.PI * 2 / WEDGE_COUNT);
            int discCx = cx + (int) (Math.cos(angle) * OUTER_RADIUS_PX);
            int discCy = cy + (int) (Math.sin(angle) * OUTER_RADIUS_PX);

            boolean onCooldown = ForceCooldowns.isOnCooldown(cds, i, now);
            int color = onCooldown
                ? dim(WEDGE_COLORS[i])
                : WEDGE_COLORS[i];
            int outline = (i == hoveredWedge) ? 0xFFFFFFFF : 0xFF202020;
            graphics.fill(discCx - DISC_HALF_PX - 2, discCy - DISC_HALF_PX - 2,
                          discCx + DISC_HALF_PX + 2, discCy + DISC_HALF_PX + 2, outline);
            graphics.fill(discCx - DISC_HALF_PX, discCy - DISC_HALF_PX,
                          discCx + DISC_HALF_PX, discCy + DISC_HALF_PX, color);

            // Overlay remaining seconds on cooldown discs
            if (onCooldown) {
                long remainingTicks = ForceCooldowns.getExpiry(cds, i) - now;
                String remaining = formatRemaining(remainingTicks);
                graphics.centeredText(this.font, Component.literal(remaining),
                    discCx, discCy - 3, 0xFFFFFFFF);
            }

            Component label = Component.translatable(power.translationKey());
            int labelColor = onCooldown ? 0xFF888888 : 0xFFFFFFFF;
            graphics.centeredText(this.font, label, discCx, discCy + DISC_HALF_PX + 4, labelColor);
        }

        Component prompt = Component.translatable("screen.starwars.force_radial.prompt");
        graphics.centeredText(this.font, prompt, cx, cy - 4, 0xFFCCCCCC);
    }

    /** Read the held holocron's cooldowns, or all-zero if not found. */
    private java.util.List<Long> readCooldowns() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return ForceCooldowns.emptyCooldowns();
        ItemStack stack = player.getItemInHand(
            mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        if (!stack.is(Registration.HOLOCRON.get())) return ForceCooldowns.emptyCooldowns();
        return stack.getOrDefault(ModDataComponents.POWER_COOLDOWNS.get(), ForceCooldowns.emptyCooldowns());
    }

    private long currentGameTime() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0L : mc.level.getGameTime();
    }

    /** Dim the wedge color to ~30% intensity with full alpha. */
    private static int dim(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return 0xFF000000 | ((r / 3) << 16) | ((g / 3) << 8) | (b / 3);
    }

    /** Format ticks as "X.Ys" rounded to one decimal, or "Xs" if >=10s. */
    private static String formatRemaining(long ticks) {
        double seconds = ticks / 20.0;
        if (seconds >= 10.0) return ((int) Math.ceil(seconds)) + "s";
        return String.format(java.util.Locale.ROOT, "%.1fs", seconds);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isFocused) {
        int wedge = SwRadialMath.wedgeFromMouse(event.x(), event.y(),
            this.width / 2.0, this.height / 2.0, DEADZONE_PX, WEDGE_COUNT);
        if (wedge >= 0) {
            select(wedge);
            return true;
        }
        return super.mouseClicked(event, isFocused);
    }

    @Override
    public void onClose() {
        if (!selectionSent && hoveredWedge >= 0) {
            // Fire the selection packet but fall through to super.onClose()
            // so the parent screen-close cleanup (focus stack, narrator)
            // still runs. select() no longer touches the screen state.
            selectionSent = true;
            ClientPacketDistributor.sendToServer(new C2SSelectPowerPacket(hoveredWedge, mainHand));
        }
        super.onClose();
    }

    private void select(int wedge) {
        if (selectionSent) return;
        selectionSent = true;
        ClientPacketDistributor.sendToServer(new C2SSelectPowerPacket(wedge, mainHand));
        // Route through onClose so parent cleanup runs uniformly.
        onClose();
    }
}
