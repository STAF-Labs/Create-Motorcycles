package ru.lotuze.createmoto.station.client.workbench;

import net.minecraft.util.Mth;

public class MotorcycleViewportCamera {

    private float yaw = 35.0F;
    private float pitch = 18.0F;
    private float zoom = 1.0F;

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public float getZoom() {
        return zoom;
    }

    public void rotate(float deltaYaw, float deltaPitch) {
        this.yaw += deltaYaw;
        this.pitch = Mth.clamp(
                this.pitch + deltaPitch,
                -30.0F,
                30.0F
        );
    }

    public void zoom(float delta) {
        this.zoom = Mth.clamp(
                this.zoom + delta,
                0.5F,
                2.0F
        );
    }

    private void reset() {
        this.yaw = 35.0F;
        this.pitch = 18.0F;
        this.zoom = 1.0F;
    }
}
