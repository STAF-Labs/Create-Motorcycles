package ru.lotuze.createmoto.station;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record ServiceStationMenuData(
        MotorcycleDetectionResult.Status status,
        int count,
        int entityId,
        @Nullable UUID entityUuid,
        double x,
        double y,
        double z,
        boolean locked
) {

    public static ServiceStationMenuData from(
            MotorcycleDetectionResult detection,
            boolean locked
    ) {
        if (detection.status() != MotorcycleDetectionResult.Status.FOUND) {

            return new ServiceStationMenuData(
                    detection.status(),
                    detection.count(),
                    -1,
                    null,
                    0.0D,
                    0.0D,
                    0.0D,
                    locked
            );
        }

        var motorcycle = detection.motorcycle();

        if (motorcycle == null) {
            throw new IllegalStateException(
                    "FOUND detection without motorcycle"
            );
        }

        return new ServiceStationMenuData(
                MotorcycleDetectionResult.Status.FOUND,
                1,
                motorcycle.getId(),
                motorcycle.getUUID(),
                motorcycle.getX(),
                motorcycle.getY(),
                motorcycle.getZ(),
                locked
        );
    }
}