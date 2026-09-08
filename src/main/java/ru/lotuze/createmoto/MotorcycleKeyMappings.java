package ru.lotuze.createmoto;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class MotorcycleKeyMappings {
    private static final String CATEGORY = "key.categories.create_motorcycles";
    public static final KeyMapping FORWARD = new KeyMapping(
            "key.create_motorcycles.motorcycle_forward",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_W,
            CATEGORY
    );
    public static final KeyMapping BACKWARD = new KeyMapping(
            "key.create_motorcycles.motorcycle_backward",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_S,
            CATEGORY
    );
    public static final KeyMapping LEFT = new KeyMapping(
            "key.create_motorcycles.motorcycle_left",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_A,
            CATEGORY
    );
    public static final KeyMapping RIGHT = new KeyMapping(
            "key.create_motorcycles.motorcycle_right",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_D,
            CATEGORY
    );
    private static boolean lastForward;
    private static boolean lastBackward;
    private static boolean lastLeft;
    private static boolean lastRight;

    private MotorcycleKeyMappings() {
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        boolean ridingMotorcycle = minecraft.player != null && minecraft.player.getVehicle() instanceof MotorcycleEntity;
        boolean forward = ridingMotorcycle && FORWARD.isDown();
        boolean backward = ridingMotorcycle && BACKWARD.isDown();
        boolean left = ridingMotorcycle && LEFT.isDown();
        boolean right = ridingMotorcycle && RIGHT.isDown();

        if (forward != lastForward || backward != lastBackward || left != lastLeft || right != lastRight) {
            if (minecraft.player != null && minecraft.player.getVehicle() instanceof MotorcycleEntity motorcycle) {

                motorcycle.setInput(forward, backward, left, right);
            }

            PacketDistributor.sendToServer(new MotorcycleInputPayload(forward, backward, left, right));

            lastForward = forward;
            lastBackward = backward;
            lastLeft = left;
            lastRight = right;
        }
    }
}
