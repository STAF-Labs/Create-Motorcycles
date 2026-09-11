package ru.lotuze.createmoto.station;

import org.jetbrains.annotations.Nullable;
import ru.lotuze.createmoto.motorcycle.MotorcycleEntity;

public record MotorcycleDetectionResult(
        Status status,
        @Nullable MotorcycleEntity motorcycle,
        int count
) {
   public enum Status {
       NOT_FOUND,
       FOUND,
   }

   public static MotorcycleDetectionResult notFound() {
       return new MotorcycleDetectionResult(
               Status.NOT_FOUND,
               null,
               0
       );
   }

   public static MotorcycleDetectionResult found(
           MotorcycleEntity motorcycle
   ) {
       return new MotorcycleDetectionResult(
               Status.FOUND,
               motorcycle,
               1
       );
   }
}
