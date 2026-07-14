package com.tweeks.starwars.client;

import java.util.List;

import com.tweeks.starwars.network.S2CGalaxyMapPacket;
import com.tweeks.starwars.world.planet.Planet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * The galaxy map: a starfield with one disc per world (charted worlds lit,
 * uncharted dimmed), gate tallies, and the nearest gates in the world you
 * are standing in. Pure display — data arrives via {@link S2CGalaxyMapPacket}.
 */
public class GalaxyMapScreen extends Screen {

    /** Same hues as the planet picker wedges, ordinal-aligned. */
    private static final int[] PLANET_COLORS = {
        0xFFE8A33D, 0xFF57B86B, 0xFF8E6BE8, 0xFF6B8E4E, 0xFFA8D8F0, 0xFF4D9BE8,
    };

    private final S2CGalaxyMapPacket data;

    public GalaxyMapScreen(S2CGalaxyMapPacket data) {
        super(Component.translatable("screen.starwars.galaxy_map"));
        this.data = data;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.fill(0, 0, this.width, this.height, 0xE0060810);
        paintStars(graphics);

        graphics.centeredText(this.font, this.title, this.width / 2, 14, 0xFFE8D9A8);

        Planet[] planets = Planet.values();
        int count = planets.length;
        int spacing = Math.min(110, (this.width - 40) / count);
        int rowX = this.width / 2 - (spacing * (count - 1)) / 2;
        int discY = 58;

        for (int i = 0; i < count; i++) {
            Planet planet = planets[i];
            boolean visited = (data.visitedMask() & (1 << i)) != 0;
            int cx = rowX + i * spacing;
            int color = PLANET_COLORS[i % PLANET_COLORS.length];
            int body = visited ? color : dim(color);
            int half = 9;

            graphics.fill(cx - half - 1, discY - half - 1, cx + half + 1, discY + half + 1,
                visited ? 0xFFDDDDDD : 0xFF303030);
            graphics.fill(cx - half, discY - half, cx + half, discY + half, body);

            graphics.centeredText(this.font, Component.translatable(planet.translationKey()),
                cx, discY + half + 6, visited ? 0xFFFFFFFF : 0xFF808080);
            Component status = visited
                ? Component.translatable("screen.starwars.galaxy_map.charted")
                : Component.translatable("screen.starwars.galaxy_map.uncharted");
            graphics.centeredText(this.font, status, cx, discY + half + 17,
                visited ? 0xFF9BE89B : 0xFF707070);
            int gates = i < data.gateCounts().size() ? data.gateCounts().get(i) : 0;
            graphics.centeredText(this.font,
                Component.translatable("screen.starwars.galaxy_map.gates", gates),
                cx, discY + half + 28, 0xFFAAB8CC);
        }

        int listY = discY + 60;
        graphics.centeredText(this.font,
            Component.translatable("screen.starwars.galaxy_map.nearby"),
            this.width / 2, listY, 0xFFE8D9A8);
        listY += 13;

        List<S2CGalaxyMapPacket.GateInfo> gates = data.nearbyGates();
        if (gates.isEmpty()) {
            graphics.centeredText(this.font,
                Component.translatable("screen.starwars.galaxy_map.no_gates"),
                this.width / 2, listY, 0xFF808080);
        } else {
            BlockPos feet = playerPos();
            for (S2CGalaxyMapPacket.GateInfo gate : gates) {
                Planet destination = Planet.byWedge(gate.destinationOrdinal());
                String destinationName = destination == null ? "?" : destination.id();
                int distance = (int) Math.sqrt(gate.pos().distSqr(feet));
                Component line = Component.translatable("screen.starwars.galaxy_map.gate_line",
                    Component.translatable("starwars.planet." + destinationName),
                    gate.pos().getX(), gate.pos().getY(), gate.pos().getZ(), distance);
                graphics.centeredText(this.font, line, this.width / 2, listY, 0xFFCCCCCC);
                listY += 11;
            }
        }
    }

    private BlockPos playerPos() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? BlockPos.ZERO : mc.player.blockPosition();
    }

    /** Deterministic little starfield so the chart feels like space. */
    private void paintStars(GuiGraphicsExtractor graphics) {
        long seed = 0x57A9F1E1DL;
        for (int i = 0; i < 90; i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int x = (int) Long.remainderUnsigned(seed >>> 16, Math.max(1, this.width));
            int y = (int) Long.remainderUnsigned(seed >>> 40, Math.max(1, this.height));
            int shade = (i % 3 == 0) ? 0xFFB8C4D8 : 0xFF5A6478;
            graphics.fill(x, y, x + 1, y + 1, shade);
        }
    }

    private static int dim(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return 0xFF000000 | ((r / 3) << 16) | ((g / 3) << 8) | (b / 3);
    }
}
