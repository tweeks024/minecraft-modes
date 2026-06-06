package com.tweeks.wildwest.client;

import com.tweeks.wildwest.item.InfinityStone;
import com.tweeks.wildwest.network.C2SSetActiveStonePacket;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Radial picker for the Infinity Gauntlet's six stones. Press the keybind
 * to open, hover a wedge, click or close to select. The selected stone is
 * sent to the server via {@link C2SSetActiveStonePacket}.
 *
 * <p>MC 26.1.2 replaced the legacy {@code GuiGraphics} render pipeline
 * with {@link GuiGraphicsExtractor} — custom screens override
 * {@code extractRenderState(...)} and use {@code fill}/{@code text}
 * primitives on the extractor.
 */
public class RadialPickerScreen extends Screen {

    private static final double DEADZONE_PX = 18.0;
    private static final int OUTER_RADIUS_PX = 90;
    private static final int DISC_HALF_PX = 12;

    private final boolean mainHand;
    private int hoveredWedge = -1;
    private boolean selectionSent = false;

    public RadialPickerScreen(boolean mainHand) {
        super(Component.translatable("screen.wildwest.infinity_gauntlet_radial"));
        this.mainHand = mainHand;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int cx = this.width / 2;
        int cy = this.height / 2;
        this.hoveredWedge = RadialMath.wedgeFromMouse(mouseX, mouseY, cx, cy, DEADZONE_PX);

        graphics.fill(0, 0, this.width, this.height, 0x80000000);

        for (int i = 0; i < 6; i++) {
            InfinityStone stone = InfinityStone.byIndex(i);
            double angle = -Math.PI / 2 + i * (Math.PI / 3);
            int discCx = cx + (int) (Math.cos(angle) * OUTER_RADIUS_PX);
            int discCy = cy + (int) (Math.sin(angle) * OUTER_RADIUS_PX);

            int color = 0xFF000000 | (stone.colorRgb() & 0x00FFFFFF);
            int outline = (i == hoveredWedge) ? 0xFFFFFFFF : 0xFF202020;
            graphics.fill(discCx - DISC_HALF_PX - 2, discCy - DISC_HALF_PX - 2,
                          discCx + DISC_HALF_PX + 2, discCy + DISC_HALF_PX + 2, outline);
            graphics.fill(discCx - DISC_HALF_PX, discCy - DISC_HALF_PX,
                          discCx + DISC_HALF_PX, discCy + DISC_HALF_PX, color);

            Component label = Component.translatable(
                "item.wildwest.infinity_gauntlet.stone." + stone.translationSuffix());
            graphics.centeredText(this.font, label, discCx, discCy + DISC_HALF_PX + 4, 0xFFFFFFFF);
        }

        Component prompt = Component.translatable("screen.wildwest.infinity_gauntlet_radial.prompt");
        graphics.centeredText(this.font, prompt, cx, cy - 4, 0xFFCCCCCC);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isFocused) {
        int wedge = RadialMath.wedgeFromMouse(event.x(), event.y(),
            this.width / 2.0, this.height / 2.0, DEADZONE_PX);
        if (wedge >= 0) {
            select(wedge);
            return true;
        }
        return super.mouseClicked(event, isFocused);
    }

    @Override
    public void onClose() {
        if (!selectionSent && hoveredWedge >= 0) {
            select(hoveredWedge);
        } else {
            super.onClose();
        }
    }

    private void select(int wedge) {
        if (selectionSent) return;
        selectionSent = true;
        ClientPacketDistributor.sendToServer(new C2SSetActiveStonePacket(wedge, mainHand));
        if (this.minecraft != null) this.minecraft.setScreen(null);
    }
}
