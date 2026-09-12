package ru.lotuze.createmoto.station.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import ru.lotuze.createmoto.CreateMotorcycles;
import ru.lotuze.createmoto.station.ServiceStationMenu;

public class ServiceStationWorkbenchScreen {

    private static final ResourceLocation SMALL_BUTTON =
            ResourceLocation.fromNamespaceAndPath(CreateMotorcycles.MODID, "textures/sprites/small_button.png");

    private static final ResourceLocation WIDE_BUTTON =
            ResourceLocation.fromNamespaceAndPath(CreateMotorcycles.MODID, "textures/sprites/wide_button.png");

    private static final ResourceLocation EXIT_ICON =
            ResourceLocation.fromNamespaceAndPath(CreateMotorcycles.MODID, "textures/sprites/icons/exit.png");

    private static final ResourceLocation SPANNER_ICON =
            ResourceLocation.fromNamespaceAndPath(CreateMotorcycles.MODID, "textures/sprites/icons/station_spanner.png");

    private static final int EDGE_MARGIN = 18;

    private static final int SMALL_BUTTON_TEXTURE_SIZE = 33;
    private static final int SMALL_BUTTON_SIZE = 24;

    private static final int EXIT_TEXTURE_WIDTH = 29;
    private static final int EXIT_TEXTURE_HEIGHT = 29;
    private static final int EXIT_WIDTH = 18;
    private static final int EXIT_HEIGHT = 18;

    private static final int SPANNER_TEXTURE_WIDTH = 30;
    private static final int SPANNER_TEXTURE_HEIGHT = 30;
    private static final int SPANNER_WIDTH = 20;
    private static final int SPANNER_HEIGHT = 20;

    private static final int WIDE_BUTTON_WIDTH = 144;
    private static final int WIDE_BUTTON_HEIGHT = 20;

    private final ServiceStationScreen parent;

    private int exitButtonX;
    private int exitButtonY;

    private int finishButtonX;
    private int finishButtonY;

    public ServiceStationWorkbenchScreen(ServiceStationScreen parent) {
        this.parent = parent;
    }

    public void init() {
        exitButtonX = width() - EDGE_MARGIN - SMALL_BUTTON_SIZE;
        exitButtonY = EDGE_MARGIN;

        finishButtonX = (width() - WIDE_BUTTON_WIDTH) / 2;
        finishButtonY = height() - EDGE_MARGIN - WIDE_BUTTON_HEIGHT;
    }

    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderHeader(graphics);
        renderFinishButton(graphics);
        renderStationInfo(graphics);
    }

    private void renderHeader(GuiGraphics graphics) {
        int iconX = EDGE_MARGIN;
        int iconY = EDGE_MARGIN;

        blit(graphics, SPANNER_ICON, iconX, iconY, SPANNER_WIDTH, SPANNER_HEIGHT, SPANNER_TEXTURE_WIDTH, SPANNER_TEXTURE_HEIGHT);

        Component stationTitle = Component.translatable("block.create_motorcycles.service_station");

        graphics.drawString(font(), stationTitle, iconX + SPANNER_WIDTH + 7, iconY + (SPANNER_HEIGHT - font().lineHeight) / 2, 0xFFFFFF, true);

        blit(graphics, SMALL_BUTTON, exitButtonX, exitButtonY, SMALL_BUTTON_SIZE, SMALL_BUTTON_SIZE, SMALL_BUTTON_TEXTURE_SIZE, SMALL_BUTTON_TEXTURE_SIZE);

        int exitX = exitButtonX + (SMALL_BUTTON_SIZE - EXIT_WIDTH) / 2;
        int exitY = exitButtonY + (SMALL_BUTTON_SIZE - EXIT_HEIGHT) / 2;

        blit(graphics, EXIT_ICON, exitX, exitY, EXIT_WIDTH, EXIT_HEIGHT, EXIT_TEXTURE_WIDTH, EXIT_TEXTURE_HEIGHT);
    }

    private void renderFinishButton(GuiGraphics graphics) {
        blit(graphics, WIDE_BUTTON, finishButtonX, finishButtonY, WIDE_BUTTON_WIDTH, WIDE_BUTTON_HEIGHT, WIDE_BUTTON_WIDTH, WIDE_BUTTON_HEIGHT);

        Component text = Component.translatable("screen.create_motorcycles.service_station.finish_work");

        graphics.drawCenteredString(font(), text, width() / 2, finishButtonY + (WIDE_BUTTON_HEIGHT - font().lineHeight) / 2 + 1, 0xFFFFFF);
    }

    private void renderStationInfo(GuiGraphics graphics) {
        Component statusLabel = Component.translatable("screen.create_motorcycles.service_station.status");
        Component statusValue = Component.translatable("screen.create_motorcycles.service_station.status_working");

        Component motorcycleLabel = Component.translatable("screen.create_motorcycles.service_station.motorcycle");
        Component motorcycleValue = Component.translatable("screen.create_motorcycles.service_station.locked");

        Component modeLabel = Component.translatable("screen.create_motorcycles.service_station.mode");
        Component modeValue = Component.translatable("screen.create_motorcycles.service_station.maintenance");

        int maxWidth = Math.max(
                font().width(statusLabel) + font().width(statusValue) + 5,
                Math.max(
                        font().width(motorcycleLabel) + font().width(motorcycleValue) + 5,
                        font().width(modeLabel) + font().width(modeValue) + 5
                )
        );

        int x = width() - EDGE_MARGIN - maxWidth;
        int y = height() - EDGE_MARGIN - font().lineHeight * 3;

        drawInfoLine(graphics, statusLabel, statusValue, x, y, 0x55FF55);
        drawInfoLine(graphics, motorcycleLabel, motorcycleValue, x, y + font().lineHeight + 2, 0x55FF55);
        drawInfoLine(graphics, modeLabel, modeValue, x, y + (font().lineHeight + 2) * 2, 0xCCCCCC);
    }

    private void drawInfoLine(
            GuiGraphics graphics,
            Component label,
            Component value,
            int x,
            int y,
            int valueColor
    ) {
        graphics.drawString(font(), label, x, y, 0xAAAAAA, true);
        graphics.drawString(font(), value, x + font().width(label) + 5, y, valueColor, true);
    }

    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button != 0) {
            return false;
        }

        if (isInsideExitButton(mouseX, mouseY)) {
            parent.closeScreen();
            return true;
        }

        if (isInsideFinishButton(mouseX, mouseY)) {
            boolean handled =
                    parent.clickMenuButton(ServiceStationMenu.BUTTON_FINISH_WORK);

            if (handled) {
                parent.setPage(ServiceStationScreen.Page.OVERVIEW);
            }

            return handled;
        }

        return false;
    }

    private boolean isInsideExitButton(double mouseX, double mouseY) {
        return mouseX >= exitButtonX
                && mouseX < exitButtonX + SMALL_BUTTON_SIZE
                && mouseY >= exitButtonY
                && mouseY < exitButtonY + SMALL_BUTTON_SIZE;
    }

    private boolean isInsideFinishButton(
            double mouseX,
            double mouseY
    ) {
        return mouseX >= finishButtonX
                && mouseX < finishButtonX + WIDE_BUTTON_WIDTH
                && mouseY >= finishButtonY
                && mouseY < finishButtonY + WIDE_BUTTON_HEIGHT;
    }

    private ServiceStationMenu menu() {
        return parent.getStationMenu();
    }

    private Font font() {
        return parent.getScreenFont();
    }

    private int width() {
        return parent.getScreenWidth();
    }
    private int height() {
        return parent.getScreenHeight();
    }

    private static void blit(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            int renderWidth,
            int renderHeight,
            int textureWidth,
            int textureHeight
    ) {
        graphics.blit(texture, x, y, renderWidth, renderHeight, 0.0F, 0.0F, textureWidth, textureHeight, textureWidth, textureHeight);
    }
}