package ru.lotuze.createmoto.station.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import ru.lotuze.createmoto.station.ServiceStationMenu;

public class ServiceStationScreen extends AbstractContainerScreen<ServiceStationMenu> {

    public enum Page {
        OVERVIEW,
        WORKBENCH
    }

    private final ServiceStationOverviewScreen overviewScreen;
    private final ServiceStationWorkbenchScreen workbenchScreen;

    private Page currentPage;

    public ServiceStationScreen(
            ServiceStationMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(menu, playerInventory, title);

        this.imageWidth = 0;
        this.imageHeight = 0;

        this.overviewScreen = new ServiceStationOverviewScreen(this);
        this.workbenchScreen = new ServiceStationWorkbenchScreen(this);

        this.currentPage = menu.isInitiallyLocked()
                ? Page.WORKBENCH
                : Page.OVERVIEW;
    }

    @Override
    protected void init() {
        super.init();

        overviewScreen.init();
        workbenchScreen.init();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Display have not GUI panel
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        super.render(graphics, mouseX, mouseY, partialTick);

        switch (currentPage) {
            case OVERVIEW -> overviewScreen.render(graphics, mouseX, mouseY, partialTick);
            case WORKBENCH -> workbenchScreen.render(graphics, mouseX, mouseY, partialTick);

        }
    }

    protected void containerTick() {
        super.containerTick();

        if (currentPage == Page.OVERVIEW && menu.isMotorcycleLocked()) {
            setPage(Page.WORKBENCH);
        }

        if (currentPage == Page.WORKBENCH
                && !menu.isMotorcycleLocked()) {
            setPage(Page.OVERVIEW);
        }
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        boolean handled = switch (currentPage) {
            case OVERVIEW -> overviewScreen.mouseClicked(mouseX, mouseY, button);
            case WORKBENCH -> workbenchScreen.mouseClicked(mouseX, mouseY, button);
        };

        if (handled) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void setPage(Page page) {
        this.currentPage = page;
    }
    public Page getPage() {
        return currentPage;
    }

    ServiceStationMenu getStationMenu() {
        return menu;
    }

    Font getScreenFont() {
        return font;
    }

    int getScreenWidth() {
        return width;
    }
    int getScreenHeight() {
        return height;
    }

    void closeScreen() {
        onClose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Without standard container labels
    }

    boolean clickMenuButton(int buttonId) {
        Minecraft minecraft = getMinecraft();

        if (minecraft == null || minecraft.gameMode == null) {
            return false;
        }

        minecraft.gameMode.handleInventoryButtonClick(
                menu.containerId,
                buttonId
        );

        return true;
    }
}