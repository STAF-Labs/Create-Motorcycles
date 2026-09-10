package ru.lotuze.createmoto.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.lotuze.createmoto.MotorcycleEntity;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void createMotorcycles$alignRiderWithMotorcyclePitch(
            LivingEntity entity,
            PoseStack poseStack,
            float bob,
            float yBodyRot,
            float partialTick,
            float scale,
            CallbackInfo callbackInfo
    ) {
        if (entity.getVehicle() instanceof MotorcycleEntity motorcycle) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(-motorcycle.getVisualPitch(partialTick)));
        }
    }
}
