package ru.lotuze.createmoto.station.client.workbench;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import ru.lotuze.createmoto.motorcycle.MotorcycleEntity;
import ru.lotuze.createmoto.motorcycle.client.MotorcycleModelRenderer;

public class MotorcycleViewport {

    private final MotorcycleModelRenderer modelRenderer = new MotorcycleModelRenderer();
    private final MotorcycleViewportCamera camera = new MotorcycleViewportCamera();
    private final MotorcycleViewportRenderer renderer = new MotorcycleViewportRenderer(modelRenderer);
    private final MotorcycleViewportPicker picker = new MotorcycleViewportPicker(modelRenderer);

    private boolean dragging;

    private double lastMouseX;
    private double lastMouseY;

    private int width;
    private int height;

    private int centerX;
    private int centerY;

    @Nullable
    private String focusedModule;

    @Nullable
    private MotorcycleEntity motorcycle;

    private float partialTick;

    public void render(
            GuiGraphics graphics,
            int entityId,
            int width,
            int height,
            float partialTick
    ) {

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        Entity entity = minecraft.level.getEntity(entityId);

        if (!(entity instanceof MotorcycleEntity motorcycle)) {
            return;
        }

        this.motorcycle = motorcycle;
        this.partialTick = partialTick;

        this.centerX = width / 2;
        this.centerY = height / 2 + 28;

        renderer.render(graphics, motorcycle, camera, centerX, centerY, partialTick, focusedModule);
    }

    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0) {
            String pickedModule = picker.pick(mouseX, mouseY, centerX, centerY, camera, motorcycle, partialTick, focusedModule);

            if (pickedModule == null) {
                this.focusedModule = null;
                return true;
            }

            if (pickedModule.equals(this.focusedModule)) {
                this.focusedModule = null;
                return true;
            }

            this.focusedModule = pickedModule;

            return true;
        }

        if (button == 1) {
            this.dragging = true;

            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;

            return true;
        }

        return false;
    }

    public boolean mouseReleased(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button != 1) {
            return false;
        }

        this.dragging = false;
        return true;
    }

    public boolean mouseDragged(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (!this.dragging || button != 1) {
            return false;
        }

        double deltaX = mouseX - this.lastMouseX;
        double deltaY = mouseY - this.lastMouseY;

        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;

        this.camera.rotate(
                (float) deltaX * 0.5F,
                (float) deltaY * 0.5F
        );

        return true;
    }

    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollDelta
    ) {
        this.camera.zoom(
                (float) scrollDelta * 0.1F
        );

        return true;
    }

    @Nullable
    public String getFocusedModule() {
        return focusedModule;
    }
}