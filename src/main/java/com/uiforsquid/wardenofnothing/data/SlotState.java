package com.uiforsquid.wardenofnothing.data;

/**
 * The three states a {@link WardenSlot} can be in.
 *
 * <ul>
 *   <li>{@link #BURROWED} &mdash; alive, underground, waiting to be summoned. Counts as alive.
 *   <li>{@link #OUT} &mdash; alive, the Warden exists in the world.
 *   <li>{@link #DEAD} &mdash; the Warden was killed; the slot is empty until a new Warden fills it.
 * </ul>
 */
public enum SlotState {
    BURROWED,
    OUT,
    DEAD;

    /** Burrowed and out both count as alive (design doc glossary). */
    public boolean isAlive() {
        return this != DEAD;
    }
}
