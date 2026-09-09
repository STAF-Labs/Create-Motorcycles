package ru.lotuze.createmoto;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MotorcycleRenderer extends EntityRenderer<MotorcycleEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");
    private static final float UNIT = 1.0F / 16.0F;
    private static final float[] MODEL_CENTER = {17.8F, 0.0F, 8.0F};
    private static final ModuleSpec[] MODULES = {
            new ModuleSpec("moto_front_lamp", new float[]{-0.6F, 0.0F, -1.2F}, new float[]{2.22125F, -11.385F, -1.2F}),
            new ModuleSpec("moto_front_assembly", new float[]{-0.6F, 0.0F, -1.2F}, new float[]{2.135F, -4.5425F, -1.2F}),
            new ModuleSpec("moto_foot_controls", new float[]{17.225F, 4.37F, 8.0F}, new float[]{7.655F, 0.69F, 8.0F}),
            new ModuleSpec("moto_fuel_tank", new float[]{16.42F, 12.65F, 8.0F}, new float[]{8.115F, 2.4725F, 8.0F}),
            new ModuleSpec("moto_engine", new float[]{16.42F, 7.475F, 8.0F}, new float[]{7.45375F, 2.8175F, 8.0F}),
            new ModuleSpec("moto_frame", new float[]{15.5F, 8.05F, 8.0F}, new float[]{4.80875F, 4.8875F, 8.0F}),
            new ModuleSpec("moto_front_wheel", new float[]{4.0F, 5.02406F, 8.60015F}, new float[]{8.0F, 5.02406F, 8.60015F}),
            new ModuleSpec("seat", new float[]{26.77F, 12.19F, 8.0F}, new float[]{8.02875F, 1.2075F, 8.0F}),
            new ModuleSpec("rear_wheel", new float[]{31.6F, 5.02406F, 8.60015F}, new float[]{8.0F, 5.02406F, 8.60015F}),
            new ModuleSpec("rear_assembly", new float[]{8.6F, 9.2F, 8.0F}, new float[]{-10.2275F, 5.1175F, 8.0F})
    };
    private static final float STEERING_PIVOT_X = -11.27F;
    private static final float STEERING_PIVOT_Y = 14.28875F;
    private static final float STEERING_PIVOT_Z = 0.0F;

    private static final float FRONT_WHEEL_PIVOT_X = -13.8F;
    private static final float FRONT_WHEEL_PIVOT_Y = 5.02406F;
    private static final float FRONT_WHEEL_PIVOT_Z = 0.60015F;

    private static final float REAR_WHEEL_PIVOT_X = 13.8F;
    private static final float REAR_WHEEL_PIVOT_Y = 5.02406F;
    private static final float REAR_WHEEL_PIVOT_Z = 0.60015F;

    private List<ModelElement> elements;

    public MotorcycleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.7F;
    }

    @Override
    public void render(MotorcycleEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F - entityYaw));
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        float steeringAngle = entity.getSteeringAngle(partialTick);
        float wheelRotation = entity.getWheelRotation(partialTick);

        for (ModelElement element : getElements()) {
            renderElement(element, poseStack, consumer, packedLight, steeringAngle, wheelRotation);
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MotorcycleEntity entity) {
        return TEXTURE;
    }

    private List<ModelElement> getElements() {
        if (this.elements == null) {
            this.elements = loadElements();
        }
        return this.elements;
    }

    private static List<ModelElement> loadElements() {
        List<ModelElement> loaded = new ArrayList<>();
        for (ModuleSpec module : MODULES) {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(CreateMotorcycles.MODID, "models/entity/motorcycle/modules/" + module.name + ".json");
            Minecraft.getInstance().getResourceManager().getResource(location).ifPresent(resource -> {
                try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

                    for (var jsonElement : root.getAsJsonArray("elements")) {
                        loaded.add(
                                ModelElement.fromJson(
                                        jsonElement.getAsJsonObject(),
                                        module.offset(),
                                        module.name()
                                )
                        );
                    }
                } catch (Exception exception) {
                    CreateMotorcycles.LOGGER.warn("Failed to load motorcycle model module {}", location, exception);
                }
            });
        }
        return loaded;
    }

    private static void renderElement(ModelElement element, PoseStack poseStack, VertexConsumer consumer, int packedLight, float steeringAngle, float wheelRotation) {
        poseStack.pushPose();

        if (isSteerable(element)) {
            float pivotX = toWorld(STEERING_PIVOT_X);
            float pivotY = toWorld(STEERING_PIVOT_Y);
            float pivotZ = toWorld(STEERING_PIVOT_Z);

            poseStack.translate(pivotX, pivotY, pivotZ);
            poseStack.mulPose(Axis.YP.rotationDegrees(-steeringAngle));
            poseStack.translate(-pivotX, -pivotY, -pivotZ);
        }

        if ("moto_front_wheel".equals(element.module())) {
            rotateWheel(
                    poseStack,
                    FRONT_WHEEL_PIVOT_X,
                    FRONT_WHEEL_PIVOT_Y,
                    FRONT_WHEEL_PIVOT_Z,
                    wheelRotation
            );
        } else if ("rear_wheel".equals((element.module()))) {
            rotateWheel(
                    poseStack,
                    REAR_WHEEL_PIVOT_X,
                    REAR_WHEEL_PIVOT_Y,
                    REAR_WHEEL_PIVOT_Z,
                    wheelRotation
            );
        }

        if (element.rotation != null) {
            poseStack.translate(toWorld(element.rotation.origin[0]), toWorld(element.rotation.origin[1]), toWorld(element.rotation.origin[2]));

            switch (element.rotation.axis) {
                case "x" -> poseStack.mulPose(Axis.XP.rotationDegrees(element.rotation.angle));
                case "y" -> poseStack.mulPose(Axis.YP.rotationDegrees(element.rotation.angle));
                case "z" -> poseStack.mulPose(Axis.ZP.rotationDegrees(element.rotation.angle));
                default -> {}
            }

            poseStack.translate(-toWorld(element.rotation.origin[0]), -toWorld(element.rotation.origin[1]), -toWorld(element.rotation.origin[2]));
        }

        float x1 = toWorld(element.from[0]);
        float y1 = toWorld(element.from[1]);
        float z1 = toWorld(element.from[2]);
        float x2 = toWorld(element.to[0]);
        float y2 = toWorld(element.to[1]);
        float z2 = toWorld(element.to[2]);
        cube(poseStack.last(), consumer, x1, y1, z1, x2, y2, z2, colorFor(element.color), packedLight);
        poseStack.popPose();
    }

    private static float toWorld(float value) {
        return value * UNIT;
    }

    private static int colorFor(int index) {
        return switch (index) {
            case 1 -> 0xFF9A4B31;
            case 2 -> 0xFFB78943;
            case 3 -> 0xFF545B61;
            case 4 -> 0xFF2D3135;
            case 5 -> 0xFF3A2420;
            case 6 -> 0xFF24282B;
            case 7 -> 0xFF7C2020;
            case 8 -> 0xFF777E82;
            case 9 -> 0xFFB7BFC3;
            default -> 0xFFCED4D7;
        };
    }

    private static void cube(PoseStack.Pose pose, VertexConsumer consumer, float x1, float y1, float z1, float x2, float y2, float z2, int color, int light) {
        quad(pose, consumer, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, 0, 0, 1, color, light);
        quad(pose, consumer, x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1, 0, 0, -1, color, light);
        quad(pose, consumer, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, -1, 0, 0, color, light);
        quad(pose, consumer, x2, y1, z2, x2, y1, z1, x2, y2, z1, x2, y2, z2, 1, 0, 0, color, light);
        quad(pose, consumer, x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1, 0, 1, 0, color, light);
        quad(pose, consumer, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, 0, -1, 0, color, light);
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer consumer,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             float nx, float ny, float nz, int color, int light) {
        vertex(pose, consumer, x1, y1, z1, 0, 0, nx, ny, nz, color, light);
        vertex(pose, consumer, x2, y2, z2, 1, 0, nx, ny, nz, color, light);
        vertex(pose, consumer, x3, y3, z3, 1, 1, nx, ny, nz, color, light);
        vertex(pose, consumer, x4, y4, z4, 0, 1, nx, ny, nz, color, light);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float x, float y, float z,
                               float u, float v, float nx, float ny, float nz, int color, int light) {
        Matrix4f matrix = pose.pose();
        consumer.addVertex(matrix, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    private static boolean isSteerable(ModelElement element) {
        return switch (element.module()) {
            case "moto_front_assembly",
                 "moto_front_wheel",
                 "moto_front_lamp" -> true;
            default -> false;
        };
    }

    private record ModuleSpec(String name, float[] referenceOrigin, float[] moduleOrigin) {
        float[] offset() {
            return new float[]{
                    referenceOrigin[0] - moduleOrigin[0] - MODEL_CENTER[0],
                    referenceOrigin[1] - moduleOrigin[1] - MODEL_CENTER[1],
                    referenceOrigin[2] - moduleOrigin[2] - MODEL_CENTER[2]
            };
        }
    }

    private record ModelRotation(String axis, float angle, float[] origin) {
        static ModelRotation fromJson(JsonObject object, float[] offset) {
            JsonArray origin = object.getAsJsonArray("origin");
            return new ModelRotation(
                    object.get("axis").getAsString(),
                    object.get("angle").getAsFloat(),
                    new float[]{origin.get(0).getAsFloat() + offset[0], origin.get(1).getAsFloat() + offset[1], origin.get(2).getAsFloat() + offset[2]}
            );
        }
    }

    private record ModelElement(String module, float[] from, float[] to, ModelRotation rotation, int color) {
        static ModelElement fromJson(JsonObject object, float[] offset, String module) {
            JsonArray from = object.getAsJsonArray("from");
            JsonArray to = object.getAsJsonArray("to");
            ModelRotation rotation = object.has("rotation") ? ModelRotation.fromJson(object.getAsJsonObject("rotation"), offset) : null;
            int color = object.has("color") ? object.get("color").getAsInt() : 0;

            return new ModelElement(
                    module,
                    new float[]{
                            from.get(0).getAsFloat() + offset[0],
                            from.get(1).getAsFloat() + offset[1],
                            from.get(2).getAsFloat() + offset[2]
                    },
                    new float[]{
                            to.get(0).getAsFloat() + offset[0],
                            to.get(1).getAsFloat() + offset[1],
                            to.get(2).getAsFloat() + offset[2]
                    },
                    rotation,
                    color
            );
        }
    }

    private static void rotateWheel(
            PoseStack poseStack,
            float pivotX,
            float pivotY,
            float pivotZ,
            float rotation
    ) {
        float x = toWorld(pivotX);
        float y = toWorld(pivotY);
        float z = toWorld(pivotZ);

        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
        poseStack.translate(-x, -y, -z);
    }
}
