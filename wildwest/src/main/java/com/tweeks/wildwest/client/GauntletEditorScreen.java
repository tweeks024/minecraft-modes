package com.tweeks.wildwest.client;

import com.tweeks.wildwest.Registration;
import com.tweeks.wildwest.item.InfinityCommands;
import com.tweeks.wildwest.item.InfinityStone;
import com.tweeks.wildwest.item.ModDataComponents;
import com.tweeks.wildwest.network.C2SSetGauntletCommandsPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Editor screen — like a command block GUI but six rows, one per stone.
 *
 * <p>Each row: colored disc + stone name + EditBox prefilled with the
 * stone's current command (or empty). Done sends the full list to the
 * server via {@link C2SSetGauntletCommandsPacket}. Cancel/Esc discards.
 */
public class GauntletEditorScreen extends Screen {

    private static final int ROW_HEIGHT = 28;
    private static final int LABEL_WIDTH = 60;
    private static final int BOX_WIDTH = 260;
    private static final int DISC_PX = 12;

    private final boolean mainHand;
    private final List<EditBox> boxes = new ArrayList<>(InfinityCommands.SLOT_COUNT);

    public GauntletEditorScreen(boolean mainHand) {
        super(Component.translatable("screen.wildwest.infinity_gauntlet_editor"));
        this.mainHand = mainHand;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        // Preserve unsaved text across resize. init() is called both on
        // first show AND on window resize; without snapshotting, the user's
        // typed-but-not-yet-saved text would be reset to the stack's last
        // saved value every time they resized the window.
        List<String> previous = new ArrayList<>(InfinityCommands.SLOT_COUNT);
        boolean hadBoxes = !boxes.isEmpty();
        for (EditBox b : boxes) previous.add(b.getValue());
        boxes.clear();

        List<String> initial = hadBoxes
            ? InfinityCommands.normalize(previous)
            : readCommands();

        int totalH = ROW_HEIGHT * InfinityCommands.SLOT_COUNT + 50;
        int top = (this.height - totalH) / 2 + 20;
        int boxX = (this.width - BOX_WIDTH) / 2 + LABEL_WIDTH / 2;

        for (int i = 0; i < InfinityCommands.SLOT_COUNT; i++) {
            InfinityStone stone = InfinityStone.byIndex(i);
            EditBox box = new EditBox(this.font,
                boxX, top + i * ROW_HEIGHT,
                BOX_WIDTH, 18,
                Component.translatable("item.wildwest.infinity_gauntlet.stone." + stone.translationSuffix()));
            box.setMaxLength(C2SSetGauntletCommandsPacket.MAX_COMMAND_LENGTH);
            box.setValue(InfinityCommands.get(initial, i));
            this.addRenderableWidget(box);
            this.boxes.add(box);
        }

        int btnY = top + InfinityCommands.SLOT_COUNT * ROW_HEIGHT + 8;
        int btnW = 80;
        int doneX = this.width / 2 - btnW - 4;
        int cancelX = this.width / 2 + 4;

        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.done"), b -> save())
            .bounds(doneX, btnY, btnW, 20)
            .build());

        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.cancel"), b -> cancel())
            .bounds(cancelX, btnY, btnW, 20)
            .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xC0101010);

        // Title
        Component title = Component.translatable("screen.wildwest.infinity_gauntlet_editor");
        graphics.centeredText(this.font, title, this.width / 2,
            (this.height - ROW_HEIGHT * InfinityCommands.SLOT_COUNT - 50) / 2, 0xFFFFFFFF);

        int totalH = ROW_HEIGHT * InfinityCommands.SLOT_COUNT + 50;
        int top = (this.height - totalH) / 2 + 20;
        int discX = (this.width - BOX_WIDTH) / 2 - LABEL_WIDTH / 2;
        int labelX = discX + DISC_PX + 6;

        for (int i = 0; i < InfinityCommands.SLOT_COUNT; i++) {
            InfinityStone stone = InfinityStone.byIndex(i);
            int rowY = top + i * ROW_HEIGHT + 4;
            int color = 0xFF000000 | (stone.colorRgb() & 0x00FFFFFF);
            graphics.fill(discX - 1, rowY - 1, discX + DISC_PX + 1, rowY + DISC_PX + 1, 0xFF000000);
            graphics.fill(discX, rowY, discX + DISC_PX, rowY + DISC_PX, color);
            Component label = Component.translatable(
                "item.wildwest.infinity_gauntlet.stone." + stone.translationSuffix());
            graphics.text(this.font, label, labelX, rowY + 2, 0xFFFFFFFF);
        }

        // Renderables (EditBoxes + Buttons) drawn by super
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private List<String> readCommands() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return InfinityCommands.empty();
        ItemStack stack = player.getItemInHand(
            mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        if (!stack.is(Registration.INFINITY_GAUNTLET.get())) return InfinityCommands.empty();
        return InfinityCommands.normalize(
            stack.getOrDefault(ModDataComponents.COMMANDS.get(), InfinityCommands.empty()));
    }

    private void save() {
        List<String> values = new ArrayList<>(InfinityCommands.SLOT_COUNT);
        for (EditBox b : boxes) values.add(b.getValue());
        ClientPacketDistributor.sendToServer(new C2SSetGauntletCommandsPacket(mainHand, values));
        if (this.minecraft != null) this.minecraft.setScreen(null);
    }

    private void cancel() {
        if (this.minecraft != null) this.minecraft.setScreen(null);
    }
}
