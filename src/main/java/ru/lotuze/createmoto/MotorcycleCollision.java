package ru.lotuze.createmoto;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class MotorcycleCollision {
    private static final double COLLISION_HALF_WIDTH = 0.42D;
    private static final double COLLISION_HEIGHT = 1.20D;
    private static final double FRONT_COLLISION_OFFSET = 0.78D;
    private static final double REAR_COLLISION_OFFSET = 0.78D;
    private static final double COLLISION_EPSILON = 1.0E-4D;

    private static final double MAX_STEP_HEIGHT = 1.0D;
    private static final double STEP_INCREMENT = 0.125D;
    private static final double GROUND_CHECK_DEPTH = 0.08D;

    private static final double TERRAIN_PROBE_RADIUS = 0.08D;
    private static final double TERRAIN_PROBE_UP = 0.25D;
    private static final double TERRAIN_PROBE_DEPTH = 1.25D;

    private MotorcycleCollision() {
    }

    public static double collisionEpsilon() {
        return COLLISION_EPSILON;
    }

    public static CollisionResult resolve(Entity entity, Vec3 desiredMovement, float yaw) {
        if (desiredMovement.horizontalDistanceSqr() <= COLLISION_EPSILON) {
            return new CollisionResult(desiredMovement, false);
        }

        Vec3 currentPosition = entity.position();
        Vec3 fullTarget = currentPosition.add(desiredMovement.x, 0.0D, desiredMovement.z);
        if (!hasMotorcycleCollision(entity, fullTarget, yaw)) {
            return new CollisionResult(desiredMovement, false);
        }

        Vec3 stepMovement = findStepMovement(entity, desiredMovement, yaw);
        if (stepMovement != null) {
            return new CollisionResult(stepMovement, true);
        }

        Vec3 xMovement = new Vec3(desiredMovement.x, 0.0D, 0.0D);
        Vec3 zMovement = new Vec3(0.0D, 0.0D, desiredMovement.z);

        boolean canMoveX = Math.abs(desiredMovement.x) > 1.0E-6D
                && !hasMotorcycleCollision(entity, currentPosition.add(xMovement), yaw);
        boolean canMoveZ = Math.abs(desiredMovement.z) > 1.0E-6D
                && !hasMotorcycleCollision(entity, currentPosition.add(zMovement), yaw);

        if (canMoveX && canMoveZ) {
            Vec3 movement = Math.abs(desiredMovement.x) >= Math.abs(desiredMovement.z) ? xMovement : zMovement;
            return new CollisionResult(movement, false);
        }

        if (canMoveX) {
            return new CollisionResult(xMovement, false);
        }
        if (canMoveZ) {
            return new CollisionResult(zMovement, false);
        }

        return new CollisionResult(Vec3.ZERO, false);
    }

    public static float calculateTerrainPitch(Entity entity, Vec3 position, float yaw, double halfWheelbase, float maxPitch) {
        Vec3 forward = forwardVector(yaw);
        Vec3 frontProbe = position.add(forward.scale(halfWheelbase));
        Vec3 rearProbe = position.subtract(forward.scale(halfWheelbase));

        double frontGroundY = findGroundHeight(entity, frontProbe);
        double rearGroundY = findGroundHeight(entity, rearProbe);
        if (!Double.isFinite(frontGroundY) || !Double.isFinite(rearGroundY)) {
            return 0.0F;
        }

        double heightDifference = frontGroundY - rearGroundY;
        double wheelbase = halfWheelbase * 2.0D;
        float pitch = (float) Math.toDegrees(Math.atan(heightDifference / wheelbase));
        return Mth.clamp(pitch, -maxPitch, maxPitch);
    }

    private static Vec3 findStepMovement(Entity entity, Vec3 desiredMovement, float yaw) {
        Vec3 currentPosition = entity.position();
        if (!hasMotorcycleGroundSupport(entity, currentPosition, yaw)) {
            return null;
        }

        for (double stepHeight = STEP_INCREMENT; stepHeight <= MAX_STEP_HEIGHT + 1.0E-6D; stepHeight += STEP_INCREMENT) {
            Vec3 raisedPosition = currentPosition.add(0.0D, stepHeight, 0.0D);
            if (hasMotorcycleCollision(entity, raisedPosition, yaw)) {
                continue;
            }

            Vec3 steppedTarget = currentPosition.add(desiredMovement.x, stepHeight, desiredMovement.z);
            if (hasMotorcycleCollision(entity, steppedTarget, yaw)) {
                continue;
            }

            return new Vec3(desiredMovement.x, stepHeight, desiredMovement.z);
        }

        return null;
    }

    private static boolean hasMotorcycleCollision(Entity entity, Vec3 position, float yaw) {
        Vec3 forward = forwardVector(yaw);
        Vec3 frontCenter = position.add(forward.scale(FRONT_COLLISION_OFFSET));
        Vec3 rearCenter = position.subtract(forward.scale(REAR_COLLISION_OFFSET));

        return hasBlockCollision(entity, createCollisionBox(frontCenter))
                || hasBlockCollision(entity, createCollisionBox(position))
                || hasBlockCollision(entity, createCollisionBox(rearCenter));
    }

    private static boolean hasBlockCollision(Entity entity, AABB box) {
        return entity.level()
                .getBlockCollisions(entity, box)
                .iterator()
                .hasNext();
    }

    private static boolean hasMotorcycleGroundSupport(Entity entity, Vec3 position, float yaw) {
        Vec3 forward = forwardVector(yaw);
        Vec3 frontCenter = position.add(forward.scale(FRONT_COLLISION_OFFSET));
        Vec3 rearCenter = position.subtract(forward.scale(REAR_COLLISION_OFFSET));

        return hasBoxGroundSupport(entity, createCollisionBox(frontCenter))
                || hasBoxGroundSupport(entity, createCollisionBox(position))
                || hasBoxGroundSupport(entity, createCollisionBox(rearCenter));
    }

    private static boolean hasBoxGroundSupport(Entity entity, AABB box) {
        AABB groundCheck = box.move(0.0D, -GROUND_CHECK_DEPTH, 0.0D);
        return entity.level()
                .getBlockCollisions(entity, groundCheck)
                .iterator()
                .hasNext();
    }

    private static double findGroundHeight(Entity entity, Vec3 point) {
        AABB probe = new AABB(
                point.x - TERRAIN_PROBE_RADIUS,
                point.y - TERRAIN_PROBE_DEPTH,
                point.z - TERRAIN_PROBE_RADIUS,
                point.x + TERRAIN_PROBE_RADIUS,
                point.y + TERRAIN_PROBE_UP,
                point.z + TERRAIN_PROBE_RADIUS
        );

        double highestGround = Double.NEGATIVE_INFINITY;
        for (VoxelShape shape : entity.level().getBlockCollisions(entity, probe)) {
            if (shape.isEmpty()) {
                continue;
            }

            AABB bounds = shape.bounds();
            if (bounds.maxY <= point.y + TERRAIN_PROBE_UP + 1.0E-6D) {
                highestGround = Math.max(highestGround, bounds.maxY);
            }
        }

        return highestGround;
    }

    private static AABB createCollisionBox(Vec3 center) {
        return new AABB(
                center.x - COLLISION_HALF_WIDTH,
                center.y,
                center.z - COLLISION_HALF_WIDTH,
                center.x + COLLISION_HALF_WIDTH,
                center.y + COLLISION_HEIGHT,
                center.z + COLLISION_HALF_WIDTH
        );
    }

    private static Vec3 forwardVector(float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        return new Vec3(-Math.sin(yaw), 0.0D, Math.cos(yaw));
    }

    public record CollisionResult(Vec3 movement, boolean stepped) {
    }
}
