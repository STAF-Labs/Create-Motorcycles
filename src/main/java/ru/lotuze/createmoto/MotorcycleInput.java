package ru.lotuze.createmoto;

public record MotorcycleInput(boolean forward, boolean backward, boolean left, boolean right) {
    public static final MotorcycleInput NONE = new MotorcycleInput(false, false, false, false);
}
