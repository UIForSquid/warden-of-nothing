package com.uiforsquid.wardenofnothing.warden;

import com.uiforsquid.wardenofnothing.city.CityTracker;
import com.uiforsquid.wardenofnothing.data.CityData;
import com.uiforsquid.wardenofnothing.data.SlotState;
import com.uiforsquid.wardenofnothing.data.TrackedCity;
import com.uiforsquid.wardenofnothing.data.WardenSlot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.warden.Warden;

/** F3: saves a City Warden's health and time into its slot when it burrows, and starts forced burrows. */
public final class WardenBurrow {

    private WardenBurrow() {
    }

    /**
     * Records a City Warden as burrowed: its current health and the game time are saved to its slot,
     * the slot becomes BURROWED, and the out-Warden link is cleared. Idempotent and a no-op for
     * Unassigned Wardens (which simply despawn).
     *
     * @return true if a City Warden slot was saved (so the caller knows this wasn't a plain despawn)
     */
    public static boolean onBurrowed(Warden warden, ServerLevel level) {
        CityWardenRef ref = CityWardens.getRef(warden);
        if (ref == null) {
            return false; // Unassigned Warden: nothing remembered.
        }
        CityData data = CityTracker.get(level);
        TrackedCity city = data.getCity(ref.cityId());
        WardenSlot slot = city == null ? null : city.outSlotOf(ref.slotIndex(), warden.getUUID());
        if (slot == null) {
            return false; // already handled (burrowed, dead, or taken over by another Warden)
        }
        slot.setRememberedHealth(warden.getHealth());
        slot.setBurrowedAtGameTime(level.getGameTime());
        slot.setState(SlotState.BURROWED);
        slot.setOutWardenId(null);
        data.setDirty();
        return true;
    }

    /**
     * Starts a forced burrow: the Warden plays the vanilla dig animation where it stands; when the
     * animation ends it is saved as burrowed by {@link #onBurrowed}. Cases 1-3 of the forced burrow.
     */
    public static void forceBurrow(Warden warden) {
        if (warden instanceof BurrowingWarden burrowing) {
            burrowing.wardenofnothing$forceBurrow();
        }
    }
}
