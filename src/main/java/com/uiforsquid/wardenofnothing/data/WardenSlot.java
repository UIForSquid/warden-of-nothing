package com.uiforsquid.wardenofnothing.data;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;

/**
 * One numbered place in a city's list of Wardens (Warden_1, Warden_2, ...). Each slot holds one
 * Warden's state and its own remembered health.
 *
 * <p>Slot numbers are internal; players only ever see Wardens. The slot's index in the city's list
 * is its number minus one.
 */
public final class WardenSlot {

    private SlotState state;

    /** Health remembered from when this Warden last burrowed, before the healing curve is applied. */
    private float rememberedHealth;

    /** World game time (ticks) when this Warden burrowed, or -1 if it has never been out. */
    private long burrowedAtGameTime;

    /** The live Warden entity's UUID while this slot is OUT, otherwise null. */
    @Nullable
    private UUID outWardenId;

    public WardenSlot(SlotState state, float rememberedHealth, long burrowedAtGameTime, @Nullable UUID outWardenId) {
        this.state = state;
        this.rememberedHealth = rememberedHealth;
        this.burrowedAtGameTime = burrowedAtGameTime;
        this.outWardenId = outWardenId;
    }

    /** A brand-new slot: burrowed, full health, never been out. */
    public static WardenSlot newBurrowed(float maxHealth) {
        return new WardenSlot(SlotState.BURROWED, maxHealth, -1L, null);
    }

    public SlotState getState() {
        return state;
    }

    public void setState(SlotState state) {
        this.state = state;
    }

    public float getRememberedHealth() {
        return rememberedHealth;
    }

    public void setRememberedHealth(float rememberedHealth) {
        this.rememberedHealth = rememberedHealth;
    }

    /** -1 if this Warden has never been out (so it sits at full/remembered health with no curve). */
    public long getBurrowedAtGameTime() {
        return burrowedAtGameTime;
    }

    public void setBurrowedAtGameTime(long burrowedAtGameTime) {
        this.burrowedAtGameTime = burrowedAtGameTime;
    }

    @Nullable
    public UUID getOutWardenId() {
        return outWardenId;
    }

    public void setOutWardenId(@Nullable UUID outWardenId) {
        this.outWardenId = outWardenId;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("state", state.name());
        tag.putFloat("rememberedHealth", rememberedHealth);
        tag.putLong("burrowedAt", burrowedAtGameTime);
        if (outWardenId != null) {
            tag.putUUID("outWarden", outWardenId);
        }
        return tag;
    }

    public static WardenSlot load(CompoundTag tag) {
        SlotState state;
        try {
            state = SlotState.valueOf(tag.getString("state"));
        } catch (IllegalArgumentException e) {
            state = SlotState.BURROWED;
        }
        float health = tag.getFloat("rememberedHealth");
        long burrowedAt = tag.contains("burrowedAt") ? tag.getLong("burrowedAt") : -1L;
        UUID outWarden = tag.hasUUID("outWarden") ? tag.getUUID("outWarden") : null;
        return new WardenSlot(state, health, burrowedAt, outWarden);
    }
}
