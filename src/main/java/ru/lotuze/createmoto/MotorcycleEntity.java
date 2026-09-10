package ru.lotuze.createmoto;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class MotorcycleEntity extends Entity {
    private static final EntityDataAccessor<Float> DATA_STEERING_ANGLE = SynchedEntityData.defineId(MotorcycleEntity.class, EntityDataSerializers.FLOAT);

    private static final double RIDER_FORWARD_OFFSET = -0.52D;
    private static final double RIDER_RIGHT_OFFSET = 0.0D;
    private static final double RIDER_UP_OFFSET = 0.12D;
    private static final float THROTTLE_LERP = 0.18F;
    private static final float STEERING_LERP = 0.25F;
    private static final float MAX_STEERING_ANGLE = 40.0F;
    private static final double ENGINE_FORCE = 0.025D;
    private static final double REVERSE_FORCE = 0.010D;
    private static final double BRAKE_FORCE = 0.055D;
    private static final double ROLLING_RESISTANCE = 0.025D;
    private static final double AIR_DRAG = 0.030D;
    private static final double HALF_WHEELBASE = 0.85D;
    private static final double WHEEL_RADIUS = 5.02406D / 16.0D;

    private static final float VISUAL_PITCH_LERP = 0.25F;
    private static final double GRAVITY = 0.08D;

    private static final float MAX_TERRAIN_PITCH = 35.0F;

    private static final double YAW_LERP = 0.20D;
    private static final double MAX_FORWARD_SPEED = 0.42D;
    private static final double MAX_REVERSE_SPEED = 0.12D;
    private static final double MIN_TURN_SPEED = 0.06D;
    private static final double REVERSE_THRESHOLD = 0.035D;

    private float throttle;
    private float steeringInput;
    private float steeringAngleOld;
    private float steeringAngle;
    private float wheelRotationOld;
    private float wheelRotation;
    private double yawVelocity;
    private MotorcycleInput input = MotorcycleInput.NONE;
    private boolean steppingThisTick;
    private float visualPitchOld;
    private float visualPitch;
    private Vec3 previousVisualPosition;
    private int clientLerpSteps;
    private double clientLerpX;
    private double clientLerpY;
    private double clientLerpZ;
    private double clientLerpYRot;
    private double clientLerpXRot;

    public MotorcycleEntity(EntityType<? extends MotorcycleEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STEERING_ANGLE, 0.0F);
    }

    public float getSteeringAngle(float partialTick) {
        return Mth.lerp(
                partialTick,
                this.steeringAngleOld,
                this.steeringAngle
        );
    }

    public float getWheelRotation(float partialTick) {
        return Mth.lerp(
                partialTick,
                this.wheelRotationOld,
                this.wheelRotation
        );
    }

    public float getVisualPitch(float partialTick) {
        return Mth.lerp(
                partialTick,
                this.visualPitchOld,
                this.visualPitch
        );
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 positionBeforeMove = this.position();
        if (this.level().isClientSide) {
            this.tickClientVisualState();
            return;
        }

        this.tickServerPhysics(positionBeforeMove);
    }

    private void tickClientVisualState() {
        this.tickClientLerp();
        this.steeringAngleOld = this.steeringAngle;
        this.steeringAngle = this.entityData.get(DATA_STEERING_ANGLE);
        this.updateVisualPitch();
        if (this.previousVisualPosition != null) {
            this.updateWheelRotation(this.previousVisualPosition);
        }
        this.previousVisualPosition = this.position();
    }

    private void tickServerPhysics(Vec3 positionBeforeMove) {
        this.steppingThisTick = false;
        this.tickMotorcyclePhysics();

        if (!this.steppingThisTick) {
            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(movement.x, movement.y - GRAVITY, movement.z);
        }

        this.move(MoverType.SELF, this.getDeltaMovement());

        if (this.onGround() && this.getDeltaMovement().y < 0.0D) {
            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(movement.x, 0.0D, movement.z);
        }

        if (this.steppingThisTick) {
            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(movement.x, 0.0D, movement.z);
        }

        this.updateWheelRotation(positionBeforeMove);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (!this.level().isClientSide) {
            super.lerpTo(x, y, z, yRot, xRot, steps);
            return;
        }

        this.clientLerpX = x;
        this.clientLerpY = y;
        this.clientLerpZ = z;
        this.clientLerpYRot = yRot;
        this.clientLerpXRot = xRot;
        this.clientLerpSteps = Math.max(steps, 1);
    }

    private void tickClientLerp() {
        if (this.clientLerpSteps <= 0) {
            return;
        }

        this.lerpPositionAndRotationStep(
                this.clientLerpSteps,
                this.clientLerpX,
                this.clientLerpY,
                this.clientLerpZ,
                this.clientLerpYRot,
                this.clientLerpXRot
        );
        this.clientLerpSteps--;
    }

    public void setInput(MotorcycleInput input) {
        this.input = input;
    }

    private void clearInput() {
        this.input = MotorcycleInput.NONE;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return tryMount(player, hand);
    }

    @Override
    public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
        return tryMount(player, hand);
    }

    private InteractionResult tryMount(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (!this.level().isClientSide && !player.isPassenger() && this.getPassengers().isEmpty()) {
            player.setYRot(this.getYRot());
            player.startRiding(this);
        }

        player.swing(hand);
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override
    public boolean isControlledByLocalInstance() {
        return !this.level().isClientSide;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        Entity passenger = this.getFirstPassenger();
        return passenger instanceof LivingEntity livingEntity ? livingEntity : null;
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        this.clearInput();
    }

    @Override
    public void remove(RemovalReason reason) {
        this.clearInput();
        super.remove(reason);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        if (this.hasPassenger(passenger)) {
            float pitch = this.level().isClientSide ? this.getVisualPitch(1.0F) : 0.0F;
            Vec3 offset = riderOffset(this.getYRot(), pitch);

            moveFunction.accept(
                    passenger,
                    this.getX() + offset.x,
                    this.getY() + offset.y,
                    this.getZ() + offset.z
            );

            if (passenger instanceof LivingEntity livingEntity) {
                livingEntity.setYBodyRot(this.getYRot());
            }
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 offset = rotateOffset(new Vec3(0.0D, 0.0D, 1.05D), this.getYRot());
        return this.position().add(offset.x, 0.1D, offset.z);
    }

    private static Vec3 riderOffset(float yawDegrees, float pitchDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        Vec3 right = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);

        double pitchedForward = RIDER_FORWARD_OFFSET * Math.cos(pitch) - RIDER_UP_OFFSET * Math.sin(pitch);
        double pitchedUp = RIDER_FORWARD_OFFSET * Math.sin(pitch) + RIDER_UP_OFFSET * Math.cos(pitch);

        return forward.scale(pitchedForward)
                .add(right.scale(RIDER_RIGHT_OFFSET))
                .add(up.scale(pitchedUp));
    }

    private static Vec3 rotateOffset(Vec3 localOffset, float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double x = localOffset.z * cos - localOffset.x * sin;
        double z = localOffset.x * cos + localOffset.z * sin;
        return new Vec3(x, localOffset.y, z);
    }

    private void tickMotorcyclePhysics() {
        if (this.getControllingPassenger() == null) {
            this.clearInput();
        }

        this.updateSteering();

        Vec3 velocity = this.getDeltaMovement();
        Vec3 horizontalVelocity = new Vec3(velocity.x, 0.0D, velocity.z);
        Vec3 rearForward = forwardVector(this.getYRot());

        double rearForwardSpeed = horizontalVelocity.dot(rearForward);
        float effectiveSteering = this.getEffectiveSteeringAngle(Math.abs(rearForwardSpeed));

        float targetThrottle = 0.0F;
        boolean braking = false;

        if (this.input.forward() && !this.input.backward()) {
            targetThrottle = 1.0F;
        } else if (this.input.backward() && !this.input.forward()) {
            if (rearForwardSpeed > REVERSE_THRESHOLD) {
                braking = true;
            } else {
                targetThrottle = -1.0F;
            }
        }

        this.throttle = Mth.lerp(THROTTLE_LERP, this.throttle, targetThrottle);

        Vec3 driveForce = rearForward
                .scale(this.throttle > 0.0F ? this.throttle * ENGINE_FORCE : this.throttle * REVERSE_FORCE);

        Vec3 brakingForce = braking ? rearForward.scale(-Math.min(rearForwardSpeed, BRAKE_FORCE)) : Vec3.ZERO;

        Vec3 rollingResistance = rearForward
                .scale(-rearForwardSpeed * ROLLING_RESISTANCE);

        Vec3 airDrag = horizontalVelocity
                .scale(-horizontalVelocity.length() * AIR_DRAG);

        Vec3 movement = horizontalVelocity
                .add(driveForce)
                .add(brakingForce)
                .add(rollingResistance)
                .add(airDrag);

        double movementSpeed = movement.dot(rearForward);
        double wheelbase = HALF_WHEELBASE * 2.0D;

        Vec3 oldForward = forwardVector(this.getYRot());

        Vec3 oldFrontPosition = this.position()
                .add(oldForward.scale(HALF_WHEELBASE));

        Vec3 frontDirection = rotateY(
                oldForward,
                Math.toRadians(effectiveSteering)
        );

        Vec3 newFrontPosition = oldFrontPosition
                .add(frontDirection.scale(movementSpeed));

        double desiredYawVelocity = 0.0D;

        if (Math.abs(movementSpeed) >= MIN_TURN_SPEED) {

            desiredYawVelocity = movementSpeed / wheelbase
                    * Math.tan(Math.toRadians(effectiveSteering));
        }

        this.yawVelocity = Mth.lerp(
                YAW_LERP,
                this.yawVelocity,
                desiredYawVelocity
        );

        float oldYaw = this.getYRot();
        float newYaw = oldYaw + (float) Math.toDegrees(this.yawVelocity);

        Vec3 newForward = forwardVector(newYaw);

        Vec3 newCenterPosition;

        if (movementSpeed >= 0) {
            newCenterPosition = newFrontPosition
                    .subtract(newForward.scale(HALF_WHEELBASE));
        } else {
            Vec3 oldRearPosition = this.position()
                    .subtract(oldForward.scale(HALF_WHEELBASE));

            Vec3 newRearPosition = oldRearPosition
                    .add(oldForward.scale(movementSpeed));

            newCenterPosition = newRearPosition
                    .add(newForward.scale(HALF_WHEELBASE));
        }

        Vec3 finalMovement = newCenterPosition
                .subtract(this.position());

        finalMovement = clampForwardSpeed(finalMovement, newForward);

        if (finalMovement.horizontalDistanceSqr() < 1.0E-5D && Math.abs(this.throttle) < 0.02F) {
            finalMovement = Vec3.ZERO;
        }

        Vec3 desiredMovement = finalMovement;
        MotorcycleCollision.CollisionResult collisionResult = MotorcycleCollision.resolve(this, desiredMovement, newYaw);
        finalMovement = collisionResult.movement();
        this.steppingThisTick = collisionResult.stepped();

        boolean fullyBlocked = desiredMovement.horizontalDistanceSqr() > MotorcycleCollision.collisionEpsilon()
                && finalMovement.horizontalDistanceSqr() <= MotorcycleCollision.collisionEpsilon();

        if (fullyBlocked) {
            this.yawVelocity = 0.0D;
            this.setYRot(oldYaw);
        } else {
            this.setYRot(newYaw);
        }

        double verticalMovement;

        if (this.steppingThisTick) {
            verticalMovement = finalMovement.y;
            finalMovement = new Vec3(desiredMovement.x, verticalMovement, desiredMovement.z);
        } else {
            verticalMovement = velocity.y;
        }

        this.setDeltaMovement(
                finalMovement.x,
                verticalMovement,
                finalMovement.z
        );
    }

    private void updateSteering() {
        this.steeringAngleOld = this.steeringAngle;

        float targetSteering = 0.0F;

        if (this.input.left()) {
            targetSteering -= 1.0F;
        }

        if (this.input.right()) {
            targetSteering += 1.0F;
        }

        this.steeringInput = Mth.lerp(STEERING_LERP, this.steeringInput, targetSteering);
        this.steeringAngle = this.steeringInput * MAX_STEERING_ANGLE;
        this.entityData.set(DATA_STEERING_ANGLE, this.steeringAngle);
    }

    private float getEffectiveSteeringAngle(double speed) {
        double normalizedSpeed = Mth.clamp(speed / MAX_FORWARD_SPEED, 0.0D, 1.0D);
        float steeringScale = (float) Mth.lerp(normalizedSpeed, 1.0D, 0.30D);
        return this.steeringAngle * steeringScale;
    }

    private static Vec3 clampForwardSpeed(Vec3 movement, Vec3 forward) {
        double forwardSpeed = movement.dot(forward);
        if (forwardSpeed > MAX_FORWARD_SPEED) {
            return movement.add(forward.scale(MAX_FORWARD_SPEED - forwardSpeed));
        }
        if (forwardSpeed < -MAX_REVERSE_SPEED) {
            return movement.add(forward.scale(-MAX_REVERSE_SPEED - forwardSpeed));
        }
        return movement;
    }

    private static Vec3 rotateY(Vec3 vector, double angleRadians) {
        double sin = Math.sin(angleRadians);
        double cos = Math.cos(angleRadians);
        return new Vec3(vector.x * cos - vector.z * sin, 0.0D, vector.x * sin + vector.z * cos);
    }

    private static Vec3 forwardVector(float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        return new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
    }

    private void updateWheelRotation(Vec3 positionBeforeMove) {
        this.wheelRotationOld = this.wheelRotation;

        double dx = this.getX() - positionBeforeMove.x;
        double dz = this.getZ() - positionBeforeMove.z;

        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance < 1.0E-5D) {
            return;
        }

        Vec3 movement = new Vec3(dx, 0.0D, dz);
        Vec3 forward = forwardVector(this.getYRot());

        double direction = movement.dot(forward) >= 0.0D ? 1.0D : -1.0D;

        float rotationDelta = (float) Math.toDegrees(
                distance / WHEEL_RADIUS
        );

        this.wheelRotation += (float) (rotationDelta * direction);

        if (this.wheelRotation > 360.0F || this.wheelRotation < -360.0F) {
            this.wheelRotation %= 360.0F;
            this.wheelRotationOld %= 360.0F;
        }
    }

    private void updateVisualPitch() {
        this.visualPitchOld = this.visualPitch;
        float targetPitch = MotorcycleCollision.calculateTerrainPitch(
                this,
                this.position(),
                this.getYRot(),
                HALF_WHEELBASE,
                MAX_TERRAIN_PITCH
        );

        this.visualPitch = Mth.lerp(
                VISUAL_PITCH_LERP,
                this.visualPitch,
                targetPitch
        );

        if (Math.abs(this.visualPitch) < 0.01F
                && Math.abs(targetPitch) < 0.01F) {
            this.visualPitch = 0.0F;
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
    }
}
