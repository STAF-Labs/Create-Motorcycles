package ru.lotuze.createmoto.station.client.workbench;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import org.lwjgl.opengl.GL11;
import ru.lotuze.createmoto.motorcycle.MotorcycleEntity;
import ru.lotuze.createmoto.motorcycle.client.MotorcycleModelRenderer;

import javax.annotation.Nullable;

public class MotorcycleViewportRenderer {

    static final float GUI_MODEL_SCALE = 200.0F;
    static final float GUI_DEPTH_SCALE = 20.0F;
    static final float GUI_MODEL_Z = 100.0F;

    private final MotorcycleModelRenderer modelRenderer;

    public MotorcycleViewportRenderer(MotorcycleModelRenderer modelRenderer) {
        this.modelRenderer = modelRenderer;
    }

    public void render(
            GuiGraphics graphics,
            MotorcycleEntity motorcycle,
            MotorcycleViewportCamera camera,
            int centerX,
            int centerY,
            float partialTick,
            @Nullable String focusedModule
    ) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(centerX, centerY, GUI_MODEL_Z);

        float scale = GUI_MODEL_SCALE * camera.getZoom();

        poseStack.scale(scale, -scale, GUI_DEPTH_SCALE);
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getPitch()));
        poseStack.mulPose(Axis.YP.rotationDegrees(camera.getYaw()));

        RenderSystem.enableDepthTest();

        MultiBufferSource.BufferSource buffer = Minecraft.getInstance()
                .renderBuffers()
                .bufferSource();

        modelRenderer.render(motorcycle, partialTick, poseStack, buffer, LightTexture.FULL_BRIGHT, focusedModule);

        buffer.endBatch();

        RenderSystem.disableDepthTest();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

        poseStack.popPose();
    }
}
