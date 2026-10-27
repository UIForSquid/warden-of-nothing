package com.uiforsquid.wardenofnothing.heart;

import net.minecraft.util.StringRepresentable;

/**
 * The three stages of the Heart of the Warden (F5). The placeholder look reuses the sniffer egg's
 * crack models, reversed: Dead = most cracked (hatch 2), Faint = slightly cracked (hatch 1),
 * Alive = no cracks (hatch 0).
 */
public enum HeartStage implements StringRepresentable {
    DEAD("dead"),
    FAINT("faint"),
    ALIVE("alive");

    private final String name;

    HeartStage(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public HeartStage next() {
        return this == DEAD ? FAINT : ALIVE;
    }

    public boolean isMaxed() {
        return this == ALIVE;
    }
}
