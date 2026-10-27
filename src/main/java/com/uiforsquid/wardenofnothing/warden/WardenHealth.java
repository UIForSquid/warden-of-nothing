package com.uiforsquid.wardenofnothing.warden;

import com.uiforsquid.wardenofnothing.config.Config;
import com.uiforsquid.wardenofnothing.data.WardenSlot;

/** The underground healing curve (F3). */
public final class WardenHealth {

    /** 20 ticks per second * 60 seconds = ticks per minute. */
    private static final double TICKS_PER_MINUTE = 1200.0;

    private WardenHealth() {
    }

    /**
     * Health a burrowed Warden returns at: its remembered health plus a share of the health it was
     * missing when it left, by the exponential curve, capped at max health.
     *
     * @param slot        the slot being summoned
     * @param maxHealth   the Warden's (configured) max health
     * @param nowGameTime the current world game time, in ticks
     */
    public static float computeSummonHealth(WardenSlot slot, float maxHealth, long nowGameTime) {
        float remembered = Math.min(slot.getRememberedHealth(), maxHealth);
        long burrowedAt = slot.getBurrowedAtGameTime();
        if (burrowedAt < 0) {
            // Never been out: sits at remembered (full) health.
            return remembered;
        }
        double minutes = (nowGameTime - burrowedAt) / TICKS_PER_MINUTE;
        if (minutes <= 0.0) {
            return remembered;
        }
        double share = healedShare(minutes, Config.FULL_HEAL_MINUTES.get(), Config.HEAL_CURVE_STEEPNESS.get());
        float missing = maxHealth - remembered;
        float healed = remembered + (float) (share * missing);
        return Math.min(healed, maxHealth);
    }

    /**
     * Share (0..1) of missing health restored after {@code minutes} underground.
     * share = (e^(k*t/T) - 1) / (e^k - 1), clamped to 1 at or beyond T; k = 0 is the straight line t/T.
     */
    public static double healedShare(double minutes, double fullHealMinutes, double steepness) {
        if (fullHealMinutes <= 0.0) {
            return 1.0;
        }
        double t = Math.min(minutes, fullHealMinutes);
        double ratio = t / fullHealMinutes;
        if (steepness == 0.0) {
            return ratio;
        }
        return (Math.exp(steepness * ratio) - 1.0) / (Math.exp(steepness) - 1.0);
    }
}
