package ru.lotuze.createmoto.station.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import ru.lotuze.createmoto.CreateMotorcycles;
import ru.lotuze.createmoto.station.MotorcycleDetectionResult;
import ru.lotuze.createmoto.station.ServiceStationMenu;

public class ServiceStationOverviewScreen {

    private static final ResourceLocation LOGO =
            ResourceLocation.fromNamespaceAndPath(CreateMotorcycles.MODID, "textures/sprites/gui_logo.png");

    private static final ResourceLocation SMALL_BUTTON =
            ResourceLocation.fromNamespaceAndPath(CreateMotorcycles.MODID, "textures/sprites/small_button.png");

    private static final ResourceLocation WIDE_BUTTON =
            ResourceLocation.fromNamespaceAndPath(CreateMotorcycles.MODID, "textures/sprites/wide_button.png");

    private static final ResourceLocation EXIT_ICON =
            ResourceLocation.fromNamespaceAndPath(CreateMotorcycles.MODID, "textures/sprites/icons/exit.png");

    private static final ResourceLocation SPANNER_ICON =
            ResourceLocation.fromNamespaceAndPath(CreateMotorcycles.MODID, "textures/sprites/icons/station_spanner.png");

    private static final ResourceLocation MOTORCYCLE_FOUND_ICON =
            ResourceLocation.fromNamespaceAndPath(CreateMotorcycles.MODID, "textures/sprites/icons/station_moto_found.png");

    private static final ResourceLocation MOTORCYCLE_NOT_FOUND_ICON =
            ResourceLocation.fromNamespaceAndPath(CreateMotorcycles.MODID, "textures/sprites/icons/station_moto_not_found.png");

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

    private static final int MOTORCYCLE_ICON_WIDTH = 33;
    private static final int MOTORCYCLE_ICON_HEIGHT = 19;

    private static final int WIDE_BUTTON_WIDTH = 144;
    private static final int WIDE_BUTTON_HEIGHT = 20;

    private static final int LOGO_TEXTURE_WIDTH = 1024;
    private static final int LOGO_TEXTURE_HEIGHT = 1024;
    private static final int LOGO_RENDER_SIZE = 72;

    private final ServiceStationScreen parent;

    private int exitButtonX;
    private int exitButtonY;
    private int actionButtonX;
    private int actionButtonY;

    public ServiceStationOverviewScreen(ServiceStationScreen parent) {
        this.parent = parent;
    }

    public void init() {
        exitButtonX = width() - EDGE_MARGIN - SMALL_BUTTON_SIZE;
        exitButtonY = EDGE_MARGIN;

        actionButtonX = (width() - WIDE_BUTTON_WIDTH) / 2;
        actionButtonY = height() / 2 + 5;
    }

    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderHeader(graphics);
        renderBranding(graphics);
        renderMotorcycleStatus(graphics);
        renderActionButton(graphics);
        renderCredits(graphics);
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

    private void renderBranding(GuiGraphics graphics) {
        int centerX = width() / 2;
        int logoY = Math.max(55, height() / 7);
        int logoX = centerX - LOGO_RENDER_SIZE / 2;

        blit(graphics, LOGO, logoX, logoY, LOGO_RENDER_SIZE, LOGO_RENDER_SIZE, LOGO_TEXTURE_WIDTH, LOGO_TEXTURE_HEIGHT);

        graphics.drawCenteredString(font(), "Create Motorcycles", centerX, logoY + LOGO_RENDER_SIZE + 7, 0xFFFFFF);
    }

    private void renderMotorcycleStatus(GuiGraphics graphics) {
        boolean found = menu().getStatus() == MotorcycleDetectionResult.Status.FOUND;

        Component statusText = found
                ? Component.translatable("screen.create_motorcycles.service_station.motorcycle_found")
                : Component.translatable("screen.create_motorcycles.service_station.motorcycle_not_found");

        ResourceLocation icon = found ? MOTORCYCLE_FOUND_ICON : MOTORCYCLE_NOT_FOUND_ICON;

        int statusY = height() / 2 - 38;
        int textWidth = font().width(statusText);
        int totalWidth = MOTORCYCLE_ICON_WIDTH + 7 + textWidth;
        int startX = (width() - totalWidth) / 2;
        int iconY = statusY - (MOTORCYCLE_ICON_HEIGHT - font().lineHeight) / 2;

        blit(graphics, icon, startX, iconY, MOTORCYCLE_ICON_WIDTH, MOTORCYCLE_ICON_HEIGHT, MOTORCYCLE_ICON_WIDTH, MOTORCYCLE_ICON_HEIGHT);

        graphics.drawString(font(), statusText, startX + MOTORCYCLE_ICON_WIDTH + 7, statusY, found ? 0xFFFFFF : 0xAAAAAA, true);
    }

    private void renderActionButton(GuiGraphics graphics) {
        int buttonX = (width() - WIDE_BUTTON_WIDTH) / 2;
        int buttonY = height() / 2 + 5;

        blit(graphics, WIDE_BUTTON, actionButtonX, actionButtonY, WIDE_BUTTON_WIDTH, WIDE_BUTTON_HEIGHT, WIDE_BUTTON_WIDTH, WIDE_BUTTON_HEIGHT);

        Component text = Component.translatable("screen.create_motorcycles.service_station.lock");

        int textColor = menu().getStatus() == MotorcycleDetectionResult.Status.FOUND ? 0xFFFFFF : 0x777777;

        graphics.drawCenteredString(font(), text, width() / 2, actionButtonY + (WIDE_BUTTON_HEIGHT - font().lineHeight) / 2 + 1, textColor);
    }

    private void renderCredits(GuiGraphics graphics) {
        int x = EDGE_MARGIN;
        int y = height() - EDGE_MARGIN - font().lineHeight * 3;

        graphics.drawString(font(), "STAF Labs", x, y, 0xAAAAAA, true);
        graphics.drawString(font(), "Create Motorcycles", x, y + font().lineHeight + 2, 0xAAAAAA, true);

        /*
         * The version can be retrieved from ModContainer later.
         */
    }

    private void renderStationInfo(GuiGraphics graphics) {
        boolean found = menu().getStatus() == MotorcycleDetectionResult.Status.FOUND;

        Component statusLabel = Component.translatable("screen.create_motorcycles.service_station.status");
        Component statusValue = Component.translatable(found
                ? "screen.create_motorcycles.service_station.status_ready"
                : "screen.create_motorcycles.service_station.status_waiting"
        );

        Component motorcycleLabel = Component.translatable("screen.create_motorcycles.service_station.motorcycle");
        Component motorcycleValue = Component.translatable(found
                ? "screen.create_motorcycles.service_station.detected"
                : "screen.create_motorcycles.service_station.not_detected"
        );

        Component modeLabel = Component.translatable("screen.create_motorcycles.service_station.mode");
        Component modeValue = Component.translatable(found
                ? "screen.create_motorcycles.service_station.awaiting_lock"
                : "screen.create_motorcycles.service_station.idle"
        );

        int maxWidth = Math.max(
                font().width(statusLabel) + font().width(statusValue) + 5,
                Math.max(
                        font().width(motorcycleLabel) + font().width(motorcycleValue) + 5,
                        font().width(modeLabel) + font().width(modeValue) + 5
                )
        );

        int x = width() - EDGE_MARGIN - maxWidth;
        int y = height() - EDGE_MARGIN - font().lineHeight * 3;

        drawInfoLine(graphics, statusLabel, statusValue, x, y, found ? 0x55FF55 : 0xAAAAAA);
        drawInfoLine(graphics, motorcycleLabel, motorcycleValue, x, y + font().lineHeight + 2, found ? 0x55FF55 : 0xAAAAAA);
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
        if (button != 0) { return false; }

        if (isInsideExitButton(mouseX, mouseY)) {
            parent.closeScreen();
            return true;
        }

        if (menu().getStatus() == MotorcycleDetectionResult.Status.FOUND
                && isInsideActionButton(mouseX, mouseY)) {
            parent.setPage(ServiceStationScreen.Page.WORKBENCH);
            return true;
        }

        return false;
    }

    private boolean isInsideExitButton(double mouseX, double mouseY) {
        return mouseX >= exitButtonX
                && mouseX < exitButtonX + SMALL_BUTTON_SIZE
                && mouseY >= exitButtonY
                && mouseY < exitButtonY + SMALL_BUTTON_SIZE;
    }

    private boolean isInsideActionButton(double mouseX, double mouseY) {
        return mouseX >= actionButtonX
                && mouseX < actionButtonX + WIDE_BUTTON_WIDTH
                && mouseY >= actionButtonY
                && mouseY < actionButtonY + WIDE_BUTTON_HEIGHT;
    }

    private ServiceStationMenu menu() { return parent.getStationMenu(); }

    private Font font() { return parent.getScreenFont(); }

    private int width() { return parent.getScreenWidth(); }
    private int height() { return parent.getScreenHeight(); }

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