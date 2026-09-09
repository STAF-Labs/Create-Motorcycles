package ru.lotuze.createmoto;

import net.minecraft.nbt.CompoundTag;
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
    private static final double RIDER_FORWARD_OFFSET = -0.52D;
    private static final double RIDER_RIGHT_OFFSET = 0.0D;
    private static final double RIDER_UP_OFFSET = 0.12D;
    private static final float THROTTLE_LERP = 0.18F;
    private static final float STEERING_LERP = 0.25F;
    private static final float MAX_STEERING_ANGLE = 20.0F;
    private static final double ENGINE_FORCE = 0.025D;
    private static final double REVERSE_FORCE = 0.010D;
    private static final double BRAKE_FORCE = 0.055D;
    private static final double FRONT_GRIP = 0.28D;
    private static final double REAR_GRIP = 0.72D;
    private static final double ROLLING_RESISTANCE = 0.025D;
    private static final double AIR_DRAG = 0.030D;
    private static final double HALF_WHEELBASE = 0.85D;
    private static final double YAW_LERP = 0.20D;
    private static final double MAX_FORWARD_SPEED = 0.42D;
    private static final double MAX_REVERSE_SPEED = 0.12D;
    private static final double MIN_TURN_SPEED = 0.06D;
    private static final double REVERSE_THRESHOLD = 0.035D;
    private float throttle;
    private float steeringInput;
    private float steeringAngle;
    private double yawVelocity;
    private boolean forwardInput;
    private boolean backwardInput;
    private boolean leftInput;
    private boolean rightInput;


    public MotorcycleEntity(EntityType<? extends MotorcycleEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        this.tickMotorcyclePhysics();
        this.applyGravity();
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    public void setInput(boolean forward, boolean backward, boolean left, boolean right) {
        this.forwardInput = forward;
        this.backwardInput = backward;
        this.leftInput = left;
        this.rightInput = right;
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

    private String debugSide() {
        return this.level().isClientSide ? "CLIENT" : "SERVER";
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
    public LivingEntity getControllingPassenger() {
        Entity passenger = this.getFirstPassenger();
        return passenger instanceof LivingEntity livingEntity ? livingEntity : null;
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        if (this.hasPassenger(passenger)) {
            Vec3 offset = riderOffset(this.getYRot());
            moveFunction.accept(passenger, this.getX() + offset.x, this.getY() + offset.y, this.getZ() + offset.z);
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 offset = rotateOffset(new Vec3(0.0D, 0.0D, 1.05D), this.getYRot());
        return this.position().add(offset.x, 0.1D, offset.z);
    }

    private static Vec3 riderOffset(float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
        Vec3 right = new Vec3(Math.cos(yaw), 0.0D, Math.sin(yaw));
        return forward.scale(RIDER_FORWARD_OFFSET)
                .add(right.scale(RIDER_RIGHT_OFFSET))
                .add(0.0D, RIDER_UP_OFFSET, 0.0D);
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
            this.forwardInput = false;
            this.backwardInput = false;
            this.leftInput = false;
            this.rightInput = false;
        }

        this.updateSteering();

        Vec3 velocity = this.getDeltaMovement();
        Vec3 horizontalVelocity = new Vec3(velocity.x, 0.0D, velocity.z);
        Vec3 rearForward = forwardVector(this.getYRot());

        double rearForwardSpeed = horizontalVelocity.dot(rearForward);
        float effectiveSteering = this.getEffectiveSteeringAngle(Math.abs(rearForwardSpeed));

        float targetThrottle = 0.0F;
        boolean braking = false;

        if (this.forwardInput && !this.backwardInput) {
            targetThrottle = 1.0F;
        } else if (this.backwardInput && !this.forwardInput) {
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

        float newYaw = this.getYRot() + (float) Math.toDegrees(this.yawVelocity);

        Vec3 newForward = forwardVector(newYaw);

        Vec3 newCenterPosition = newFrontPosition
                .subtract(newForward.scale(HALF_WHEELBASE));

        Vec3 finalMovement = newCenterPosition
                .subtract(this.position());

        this.setYRot(newYaw);

        finalMovement = clampForwardSpeed(finalMovement, newForward);

        if (finalMovement.horizontalDistanceSqr() < 1.0E-5D
                && Math.abs(this.throttle) < 0.02F) {
            finalMovement = Vec3.ZERO;
        }

        this.setDeltaMovement(
                finalMovement.x,
                velocity.y,
                finalMovement.z
        );
    }

    private void updateSteering() {
        float targetSteering = 0.0F;
        if (this.leftInput) {
            targetSteering -= 1.0F;
        }
        if (this.rightInput) {
            targetSteering += 1.0F;
        }
        this.steeringInput = Mth.lerp(STEERING_LERP, this.steeringInput, targetSteering);
        this.steeringAngle = this.steeringInput * MAX_STEERING_ANGLE;
    }

    private float getEffectiveSteeringAngle(double speed) {
        double normalizedSpeed = Mth.clamp(speed / MAX_FORWARD_SPEED, 0.0D, 1.0D);
        float steeringScale = (float) Mth.lerp(normalizedSpeed, 1.0D, 0.30D);
        return this.steeringAngle * steeringScale;
    }

    private void updateYawFromBicycleModel(double forwardSpeed, float effectiveSteering) {
        if (Math.abs(forwardSpeed) < MIN_TURN_SPEED) {
            this.yawVelocity = 0.0D;
            return;
        }

        double wheelbase = HALF_WHEELBASE * 2.0D;

        double steeringRadians = Math.toRadians(effectiveSteering);

        double beta = Math.atan(0.5D * Math.tan(steeringRadians));

        double desiredYawVelocity = forwardSpeed / wheelbase * Math.cos(beta) * Math.tan(steeringRadians);
        this.yawVelocity = Mth.lerp(YAW_LERP, this.yawVelocity, desiredYawVelocity);
        this.setYRot(this.getYRot() + (float) Math.toDegrees(this.yawVelocity));
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

    private static Vec3 rightVector(Vec3 forward) {
        return new Vec3(forward.z, 0.0D, -forward.x);
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

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
    }
}
