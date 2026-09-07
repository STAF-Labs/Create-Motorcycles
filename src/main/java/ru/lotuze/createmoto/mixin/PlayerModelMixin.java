package ru.lotuze.createmoto.mixin;

import ru.lotuze.createmoto.MotorcycleEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.Minecraft;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin<T extends LivingEntity> extends HumanoidModel<T> {
    @Shadow
    @Final
    public ModelPart leftSleeve;
    @Shadow
    @Final
    public ModelPart rightSleeve;
    @Shadow
    @Final
    public ModelPart leftPants;
    @Shadow
    @Final
    public ModelPart rightPants;
    @Shadow
    @Final
    public ModelPart jacket;

    public PlayerModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void createMotorcycles$poseMotorcycleRider(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo callbackInfo) {
        if (!(entity.getVehicle() instanceof MotorcycleEntity)) {
            return;
        }

        boolean firstPersonLocalPlayer =
                entity == Minecraft.getInstance().player
                        && Minecraft.getInstance().options.getCameraType().isFirstPerson();

        this.body.xRot = createMotorcycles$radians(50.0F);
        this.body.yRot = 0.0F;
        this.body.zRot = 0.0F;
        this.body.y = 3.0F;
        this.body.z = -6.5F;

        this.head.xRot *= 0.65F;
        this.head.zRot = 0.0F;
        this.head.x = this.body.x;
        this.head.y = this.body.y - 1.0F;
        this.head.z = this.body.z;
        this.hat.copyFrom(this.head);

        if (firstPersonLocalPlayer) {
            this.rightArm.x = -5.0F;
            this.rightArm.y = 2.0F;
            this.rightArm.z = 0.0F;

            this.leftArm.x = 5.0F;
            this.leftArm.y = 2.0F;
            this.leftArm.z = 0.0F;
        } else {
            this.rightArm.xRot = createMotorcycles$radians(-82.0F);
            this.rightArm.yRot = createMotorcycles$radians(-8.0F);
            this.rightArm.zRot = createMotorcycles$radians(4.0F);

            this.rightArm.x = this.body.x - 5.0F;
            this.rightArm.y = this.body.y + 1.0F;
            this.rightArm.z = this.body.z;

            this.leftArm.xRot = createMotorcycles$radians(-82.0F);
            this.leftArm.yRot = createMotorcycles$radians(8.0F);
            this.leftArm.zRot = createMotorcycles$radians(-4.0F);

            this.leftArm.x = this.body.x + 5.0F;
            this.leftArm.y = this.body.y + 1.0F;
            this.leftArm.z = this.body.z;
        }

        this.rightLeg.xRot = createMotorcycles$radians(-58.0F);
        this.rightLeg.yRot = createMotorcycles$radians(8.0F);
        this.rightLeg.zRot = createMotorcycles$radians(3.0F);
        this.leftLeg.xRot = createMotorcycles$radians(-58.0F);
        this.leftLeg.yRot = createMotorcycles$radians(-8.0F);
        this.leftLeg.zRot = createMotorcycles$radians(-3.0F);

        this.rightSleeve.copyFrom(this.rightArm);
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightPants.copyFrom(this.rightLeg);
        this.leftPants.copyFrom(this.leftLeg);
        this.jacket.copyFrom(this.body);
    }

    @Unique
    private static float createMotorcycles$radians(float degrees) {
        return (float) Math.toRadians(degrees);
    }
}
