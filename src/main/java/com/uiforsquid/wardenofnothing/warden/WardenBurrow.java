package com.uiforsquid.wardenofnothing.warden;

import com.uiforsquid.wardenofnothing.city.CityShriekers;
import com.uiforsquid.wardenofnothing.city.CityTracker;
import com.uiforsquid.wardenofnothing.config.Config;
import com.uiforsquid.wardenofnothing.data.TrackedCity;
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
            // Unassigned Warden: it may still claim a city that currently has no Warden (see below).
            return claimForDefeatedCity(warden, level);
        }
        TrackedCity city = CityTracker.get(level).getCity(ref.cityId());
        // A no-op if this Warden no longer holds its slot (already burrowed, dead, or taken over).
        return city != null && city.burrow(ref.slotIndex(), warden.getUUID(), warden.getHealth(), level.getGameTime());
    }

    /**
     * An Unassigned Warden burrowing inside a tracked city that has no living Warden (defeated) is
     * claimed as that city's Warden: it fills the lowest dead slot (or a new one) as BURROWED with its
     * current health, so the city is no longer defeated and its shriekers switch back on. This lets an op
     * revive a city just by adding a Warden (egg or command) to it and letting it burrow. The Warden
     * entity itself is despawning as part of the burrow, so only the slot is recorded.
     *
     * @return true if the Warden was claimed into a slot
     */
    private static boolean claimForDefeatedCity(Warden warden, ServerLevel level) {
        if (!Config.CLAIM_BURROWING_WARDENS.get()) {
            return false;
        }
        TrackedCity city = CityTracker.findCity(level, warden.blockPosition());
        if (city == null || !city.isDefeated()) {
            return false; // no city here, or it already has a living Warden
        }
        city.claim(null, warden.getHealth(), level.getGameTime());
        CityShriekers.refreshLoaded(level, city); // the city now has a Warden: shriekers switch back on
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
