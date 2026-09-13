package ru.lotuze.createmoto.station.client.workbench;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import ru.lotuze.createmoto.motorcycle.MotorcycleEntity;
import ru.lotuze.createmoto.motorcycle.client.MotorcycleModelRenderer;

public class MotorcycleViewportPicker {

    private static final float RAY_DEPTH = 10000.0F;

    private final MotorcycleModelRenderer modelRenderer;

    public MotorcycleViewportPicker(MotorcycleModelRenderer modelRenderer) {
        this.modelRenderer = modelRenderer;
    }

    @Nullable
    public String pick(
            double mouseX,
            double mouseY,
            int centerX,
            int centerY,
            MotorcycleViewportCamera camera,
            MotorcycleEntity motorcycle,
            float partialTick,
            @Nullable String focusedModule
    ) {
        Matrix4f viewportTransform = createViewportTransform(centerX, centerY, camera);

        String pickedModule = null;
        float closestDepth = -Float.MAX_VALUE;

        for (MotorcycleModelRenderer.PickElement element : modelRenderer.getPickElements()) {

            Matrix4f elementTransform = modelRenderer.getPickTransform(element.index(), motorcycle, partialTick, focusedModule);
            Matrix4f fullTransform = new Matrix4f(viewportTransform).mul(elementTransform);
            Matrix4f inverseTransform = new Matrix4f(fullTransform).invert();

            Vector4f near = new Vector4f((float) mouseX, (float) mouseY, -RAY_DEPTH, 1.0F);
            Vector4f far = new Vector4f((float) mouseX, (float) mouseY, RAY_DEPTH, 1.0F);

            near.mul(inverseTransform);
            far.mul(inverseTransform);

            float directionX = far.x - near.x;
            float directionY = far.y - near.y;
            float directionZ = far.z - near.z;

            float distance = intersect(near.x, near.y, near.z, directionX, directionY, directionZ, element);

            if (distance < 0.0F) {
                continue;
            }

            Vector4f localHit = new Vector4f(near.x + directionX * distance, near.y + directionY * distance, near.z + directionZ * distance, 1.0F);

            localHit.mul(fullTransform);

            if (localHit.z > closestDepth) {
                closestDepth = localHit.z;
                pickedModule = element.module();
            }
        }

        return pickedModule;
    }

    private static Matrix4f createViewportTransform(
            int centerX,
            int centerY,
            MotorcycleViewportCamera camera
    ) {
        PoseStack poseStack = new PoseStack();
        poseStack.translate(centerX, centerY, MotorcycleViewportRenderer.GUI_MODEL_Z);

        float scale = MotorcycleViewportRenderer.GUI_MODEL_SCALE * camera.getZoom();

        poseStack.scale(scale, -scale, MotorcycleViewportRenderer.GUI_DEPTH_SCALE);
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getPitch()));

        poseStack.mulPose(Axis.YP.rotationDegrees(camera.getYaw()));

        return new Matrix4f(poseStack.last().pose());
    }

    private static float intersect(
            float originX,
            float originY,
            float originZ,
            float directionX,
            float directionY,
            float directionZ,
            MotorcycleModelRenderer.PickElement element
    ) {
        float tMin = -Float.MAX_VALUE;
        float tMax = Float.MAX_VALUE;

        float[] x = intersectAxis(originX, directionX, element.minX(), element.maxX());

        if (x == null) {
            return -1.0F;
        }

        tMin = Math.max(tMin, x[0]);
        tMax = Math.min(tMax, x[1]);

        float[] y = intersectAxis(originY, directionY, element.minY(), element.maxY());

        if (y == null) {
            return -1.0F;
        }

        tMin = Math.max(tMin, y[0]);
        tMax = Math.min(tMax, y[1]);

        float[] z = intersectAxis(originZ, directionZ, element.minZ(), element.maxZ());

        if (z == null) {
            return -1.0F;
        }

        tMin = Math.max(tMin, z[0]);
        tMax = Math.min(tMax, z[1]);

        if (tMax < tMin || tMax < 0.0F) {
            return -1.0F;
        }

        return tMin >= 0.0F ? tMin : tMax;
    }

    @Nullable
    private static float[] intersectAxis(
            float origin,
            float direction,
            float min,
            float max
    ) {
        if (Math.abs(direction) < 0.00001F) {
            if (origin < min || origin > max) {
                return null;
            }

            return new float[]{
                    -Float.MAX_VALUE,
                    Float.MAX_VALUE
            };
        }

        float t1 = (min - origin) / direction;
        float t2 = (max - origin) / direction;

        if (t1 > t2) {
            float temp = t1;
            t1 = t2;
            t2 = temp;
        }

        return new float[]{t1, t2};
    }
}