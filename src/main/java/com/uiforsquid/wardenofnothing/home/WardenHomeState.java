package com.uiforsquid.wardenofnothing.home;

import java.util.UUID;
import javax.annotation.Nullable;

/** Runtime-only F8 state for one City Warden (not saved; rebuilt on reload). */
public final class WardenHomeState {

    /** True while the Warden is heading back to its city, ignoring everything but the outside boom. */
    public boolean returning;

    /** Hits taken while returning; at {@code returnHitsBeforeBurrow} the Warden digs. */
    public int returnHits;

    /** Ticks spent continuously inside the city bounds, for the hit-counter reset. */
    public int ticksInsideBounds;

    /** Game time the current walk-back attempt (while physically outside home range) started; 0 = none. */
    public long walkBackStartTime;

    /** Game time until which another outside-target boom may not start. */
    public long boomCooldownUntil;

    /** The outside target currently being charged at, or null if not charging. */
    @Nullable
    public UUID boomTargetId;

    /** Game time the charging boom fires. */
    public long boomFireAt;
}
