package ru.lotuze.createmoto.motorcycle.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import ru.lotuze.createmoto.motorcycle.MotorcycleEntity;

public class MotorcycleRenderer extends EntityRenderer<MotorcycleEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");

    private final MotorcycleModelRenderer modelRenderer = new MotorcycleModelRenderer();

    public MotorcycleRenderer(EntityRendererProvider.Context context) {
        super(context);

        this.shadowRadius = 0.7F;
    }

    @Override
    public void render(
            MotorcycleEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F - entityYaw));

        float visualPitch = entity.getVisualPitch(partialTick);

        poseStack.mulPose(Axis.ZP.rotationDegrees(-visualPitch));

        modelRenderer.render(entity, partialTick, poseStack, buffer, packedLight);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MotorcycleEntity entity) {

        return TEXTURE;
    }
}