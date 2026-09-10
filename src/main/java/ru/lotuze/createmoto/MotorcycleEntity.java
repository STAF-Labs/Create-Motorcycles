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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

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

    private static final double COLLISION_HALF_WIDTH = 0.42D;
    private static final double COLLISION_HEIGHT = 1.20D;
    private static final double FRONT_COLLISION_OFFSET = 0.78D;
    private static final double REAR_COLLISION_OFFSET = 0.78D;
    private static final double COLLISION_EPSILON = 1.0E-4D;

    private static final double MAX_STEP_HEIGHT = 1.0D;
    private static final float VISUAL_PITCH_LERP = 0.25F;
    private static final double STEP_INCREMENT = 0.125D;
    private static final double GROUND_CHECK_DEPTH = 0.08D;
    private static final double GRAVITY = 0.08D;

    private static final double TERRAIN_PROBE_RADIUS = 0.08D;
    private static final double TERRAIN_PROBE_UP = 0.25D;
    private static final double TERRAIN_PROBE_DEPTH = 1.25D;
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
            this.setYRot(player.getYRot());
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
        finalMovement = resolveMotorcycleCollision(
                desiredMovement,
                newYaw
        );

        boolean fullyBlocked = desiredMovement.horizontalDistanceSqr() > COLLISION_EPSILON
                && finalMovement.horizontalDistanceSqr() <= COLLISION_EPSILON;

        if (fullyBlocked) {
            this.yawVelocity = 0.0D;
            this.setYRot(oldYaw);
        } else {
            this.setYRot(newYaw);
        }

        double verticalMovement;

        if (this.steppingThisTick) {
            verticalMovement = finalMovement.y;
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

    private AABB createCollisionBox(Vec3 center) {
        return new AABB(
                center.x - COLLISION_HALF_WIDTH,
                center.y,
                center.z - COLLISION_HALF_WIDTH,
                center.x + COLLISION_HALF_WIDTH,
                center.y + COLLISION_HEIGHT,
                center.z + COLLISION_HALF_WIDTH
        );
    }

    private boolean hasMotorcycleCollision(Vec3 position, float yaw) {
        Vec3 forward = forwardVector(yaw);

        Vec3 frontCenter = position.add(
                forward.scale(FRONT_COLLISION_OFFSET)
        );

        Vec3 rearCenter = position.subtract(
                forward.scale(REAR_COLLISION_OFFSET)
        );

        return hasBlockCollision(createCollisionBox(frontCenter))
                || hasBlockCollision(createCollisionBox(position))
                || hasBlockCollision(createCollisionBox(rearCenter));
    }

    private boolean hasBlockCollision(AABB box) {
        return this.level()
                .getBlockCollisions(this, box)
                .iterator()
                .hasNext();
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

    private Vec3 resolveMotorcycleCollision(Vec3 desiredMovement, float yaw) {
        if (desiredMovement.horizontalDistanceSqr() <= COLLISION_EPSILON) {
            return desiredMovement;
        }

        Vec3 currentPosition = this.position();

        Vec3 fullTarget = currentPosition.add(
                desiredMovement.x,
                0.0D,
                desiredMovement.z
        );

        if (!hasMotorcycleCollision(fullTarget, yaw)) {
            return desiredMovement;
        }

        Vec3 stepMovement = findStepMovement(
                desiredMovement,
                yaw
        );

        if (stepMovement != null) {
            return stepMovement;
        }

        Vec3 xMovement = new Vec3(
                desiredMovement.x,
                0.0D,
                0.0D
        );

        Vec3 zMovement = new Vec3(
                0.0D,
                0.0D,
                desiredMovement.z
        );

        boolean canMoveX = Math.abs(desiredMovement.x) > 1.0E-6D
                && !hasMotorcycleCollision(
                        currentPosition.add(xMovement),
                        yaw
                );

        boolean canMoveZ = Math.abs(desiredMovement.z) > 1.0E-6D
                && !hasMotorcycleCollision(
                        currentPosition.add(zMovement),
                        yaw
                );

        if (canMoveX && canMoveZ) {
            return Math.abs(desiredMovement.x) >= Math.abs(desiredMovement.z) ? xMovement : zMovement;
        }

        if (canMoveX) { return xMovement; }
        if (canMoveZ) { return zMovement; }

        return Vec3.ZERO;
    }

    private Vec3 findStepMovement(Vec3 desiredMovement, float yaw) {
        Vec3 currentPosition = this.position();

        if (!hasMotorcycleGroundSupport(currentPosition, yaw)) {
            return null;
        }

        for (
                double stepHeight = STEP_INCREMENT;
                stepHeight <= MAX_STEP_HEIGHT + 1.0E-6D;
                stepHeight += STEP_INCREMENT
        ) {
            Vec3 raisedPosition = currentPosition.add(
                    0.0D,
                    stepHeight,
                    0.0D
            );

            if (hasMotorcycleCollision(raisedPosition, yaw)) {
                continue;
            }

            Vec3 steppedTarget = currentPosition.add(
                    desiredMovement.x,
                    stepHeight,
                    desiredMovement.z
            );

            if (hasMotorcycleCollision(steppedTarget, yaw)) {
               continue;
            }

            this.steppingThisTick = true;

            return new Vec3(
                    desiredMovement.x,
                    stepHeight,
                    desiredMovement.z
            );
        }

        return null;
    }

    private boolean hasMotorcycleGroundSupport(Vec3 position, float yaw) {
        Vec3 forward = forwardVector(yaw);

        Vec3 frontCenter = position.add(
                forward.scale(FRONT_COLLISION_OFFSET)
        );

        Vec3 rearCenter = position.subtract(
                forward.scale(REAR_COLLISION_OFFSET)
        );

        return hasBoxGroundSupport(createCollisionBox(frontCenter))
                || hasBoxGroundSupport(createCollisionBox(position))
                || hasBoxGroundSupport(createCollisionBox(rearCenter));
    }

    private boolean hasBoxGroundSupport(AABB box) {
        AABB groundCheck = box.move(
                0.0D,
                -GROUND_CHECK_DEPTH,
                0.0D
        );

        return this.level()
                .getBlockCollisions(this, groundCheck)
                .iterator()
                .hasNext();
    }

    private double findGroundHeight(Vec3 point) {
        AABB probe = new AABB(
                point.x - TERRAIN_PROBE_RADIUS,
                point.y - TERRAIN_PROBE_DEPTH,
                point.z - TERRAIN_PROBE_RADIUS,
                point.x + TERRAIN_PROBE_RADIUS,
                point.y + TERRAIN_PROBE_UP,
                point.z + TERRAIN_PROBE_RADIUS
        );

        double highestGround = Double.NEGATIVE_INFINITY;

        for (VoxelShape shape : this.level().getBlockCollisions(this, probe)) {
            if (shape.isEmpty()) {
                continue;
            }

            AABB bounds = shape.bounds();

            if (bounds.maxY <= point.y + TERRAIN_PROBE_UP + 1.0E-6D) {
                highestGround = Math.max(
                        highestGround,
                        bounds.maxY
                );
            }
        }

        return highestGround;
    }

    private void updateVisualPitch() {
        this.visualPitchOld = this.visualPitch;

        Vec3 forward = forwardVector(this.getYRot());

        Vec3 frontProbe = this.position().add(
                forward.scale(HALF_WHEELBASE)
        );

        Vec3 rearProbe = this.position().subtract(
                forward.scale(HALF_WHEELBASE)
        );

        double frontGroundY = findGroundHeight(frontProbe);
        double rearGroundY = findGroundHeight(rearProbe);

        float targetPitch = 0.0F;

        if (Double.isFinite(frontGroundY)
                && Double.isFinite(rearGroundY)) {

            double heightDifference =
                    frontGroundY - rearGroundY;

            targetPitch = calculateStepPitch(
                    heightDifference
            );

            targetPitch = Mth.clamp(
                    targetPitch,
                    -MAX_TERRAIN_PITCH,
                    MAX_TERRAIN_PITCH
            );
        }

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

    private float calculateStepPitch(double stepHeight) {
        double wheelbase = HALF_WHEELBASE * 2.0D;

        return (float) Math.toDegrees(
                Math.atan(stepHeight / wheelbase)
        );
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
    }
}
