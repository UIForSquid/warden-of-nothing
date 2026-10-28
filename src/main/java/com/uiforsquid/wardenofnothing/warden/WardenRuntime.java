package com.uiforsquid.wardenofnothing.warden;

import com.uiforsquid.wardenofnothing.home.WardenHomeState;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.entity.monster.warden.Warden;

/**
 * Everything the mod tracks about one Warden while it is in the world, held by the Warden entity itself
 * (a field added by {@code WardenMixin}). Nothing here is saved: it is rebuilt after a reload, and it
 * goes away with the entity when it unloads, dies, burrows or changes dimension, so there is no
 * per-Warden map to forget or clear.
 */
public final class WardenRuntime {

    /** F8 home-range state. Replaced with a fresh one when the Warden is sent down to burrow. */
    private WardenHomeState home = new WardenHomeState();

    /** F1 boss bar, or null while the Warden has none. Managed by {@link BossBars}. */
    @Nullable
    ServerBossEvent bossBar;

    /** The config text {@link #bossBar}'s name was last built from, so the name is only rebuilt on change. */
    @Nullable
    String bossBarText;

    /** F3: true from a forced burrow until the dig animation starts. */
    private boolean forcingBurrow;

    private WardenRuntime() {
    }

    /** The runtime state of a Warden (every Warden has one). */
    public static WardenRuntime of(Warden warden) {
        return ((BurrowingWarden) warden).wardenofnothing$runtime();
    }

    public WardenHomeState home() {
        return home;
    }

    /** Starts home-range tracking over (the Warden has been sent to burrow). */
    public void resetHome() {
        home = new WardenHomeState();
    }

    public boolean isForcingBurrow() {
        return forcingBurrow;
    }

    public void setForcingBurrow(boolean forcingBurrow) {
        this.forcingBurrow = forcingBurrow;
    }

    /** For {@code WardenMixin}: a new, empty runtime state. */
    public static WardenRuntime create() {
        return new WardenRuntime();
    }
}
