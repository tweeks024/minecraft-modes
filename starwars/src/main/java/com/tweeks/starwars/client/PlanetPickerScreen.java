package com.tweeks.starwars.client;

import com.tweeks.starwars.network.C2SSelectPlanetPacket;
import com.tweeks.starwars.world.planet.Planet;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Radial destination picker for a hyperspace gate, opened by the server
 * after the star compass validated the frame. Wedge order matches
 * {@link Planet}: Tatooine top, Andor right, Coruscant bottom, Home left.
 * Mirrors {@link ForcePickerScreen}'s 26.1.2 extract-render-state pattern.
 */
public class PlanetPickerScreen extends Screen {

    private static final double DEADZONE_PX = 18.0;
    private static final int OUTER_RADIUS_PX = 95;
    private static final int DISC_HALF_PX = 13;

    private static final int[] WEDGE_COLORS = {
        0xFFE8A33D, // TATOOINE — twin-sun amber
        0xFF57B86B, // ANDOR — highland green
        0xFF8E6BE8, // CORUSCANT — dusk violet
        0xFF6B8E4E, // DAGOBAH — swamp green
        0xFFA8D8F0, // HOTH — glacier blue
        0xFF6E7178, // DEATH_STAR — imperial gray
        0xFF4D9BE8, // HOME — overworld blue
    };

    private final BlockPos gateOrigin;
    private final boolean axisX;
    private int hoveredWedge = -1;
    private boolean selectionSent = false;

    public PlanetPickerScreen(BlockPos gateOrigin, boolean axisX) {
        super(Component.translatable("screen.starwars.planet_radial"));
        this.gateOrigin = gateOrigin;
        this.axisX = axisX;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        int cx = this.width / 2;
        int cy = this.height / 2;
        this.hoveredWedge = SwRadialMath.wedgeFromMouse(mouseX, mouseY, cx, cy, DEADZONE_PX, Planet.COUNT);

        graphics.fill(0, 0, this.width, this.height, 0x80000000);

        for (int i = 0; i < Planet.COUNT; i++) {
            Planet planet = Planet.byWedge(i);
            double angle = -Math.PI / 2 + i * (Math.PI * 2 / Planet.COUNT);
            int discCx = cx + (int) (Math.cos(angle) * OUTER_RADIUS_PX);
            int discCy = cy + (int) (Math.sin(angle) * OUTER_RADIUS_PX);

            int outline = (i == hoveredWedge) ? 0xFFFFFFFF : 0xFF202020;
            graphics.fill(discCx - DISC_HALF_PX - 2, discCy - DISC_HALF_PX - 2,
                          discCx + DISC_HALF_PX + 2, discCy + DISC_HALF_PX + 2, outline);
            graphics.fill(discCx - DISC_HALF_PX, discCy - DISC_HALF_PX,
                          discCx + DISC_HALF_PX, discCy + DISC_HALF_PX, WEDGE_COLORS[i]);

            Component label = Component.translatable(planet.translationKey());
            graphics.centeredText(this.font, label, discCx, discCy + DISC_HALF_PX + 4, 0xFFFFFFFF);
        }

        Component prompt = Component.translatable("screen.starwars.planet_radial.prompt");
        graphics.centeredText(this.font, prompt, cx, cy - 4, 0xFFCCCCCC);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isFocused) {
        int wedge = SwRadialMath.wedgeFromMouse(event.x(), event.y(),
            this.width / 2.0, this.height / 2.0, DEADZONE_PX, Planet.COUNT);
        if (wedge >= 0) {
            select(wedge);
            return true;
        }
        return super.mouseClicked(event, isFocused);
    }

    @Override
    public void onClose() {
        if (!selectionSent && hoveredWedge >= 0) {
            selectionSent = true;
            ClientPacketDistributor.sendToServer(new C2SSelectPlanetPacket(gateOrigin, axisX, hoveredWedge));
        }
        super.onClose();
    }

    private void select(int wedge) {
        if (selectionSent) {
            return;
        }
        selectionSent = true;
        ClientPacketDistributor.sendToServer(new C2SSelectPlanetPacket(gateOrigin, axisX, wedge));
        onClose();
    }
}
