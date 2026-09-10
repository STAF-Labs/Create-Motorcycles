package ru.lotuze.createmoto.station;

import net.minecraft.util.StringRepresentable;

public enum ServiceStationPart implements StringRepresentable {
    FRONT("front"),
    MASTER("master"),
    BACK("back");

    private final String name;

    ServiceStationPart(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
