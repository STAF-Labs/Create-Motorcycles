package ru.lotuze.createmoto.motorcycle;

public record MotorcycleInput(boolean forward, boolean backward, boolean left, boolean right) {
    public static final MotorcycleInput NONE = new MotorcycleInput(false, false, false, false);
}
