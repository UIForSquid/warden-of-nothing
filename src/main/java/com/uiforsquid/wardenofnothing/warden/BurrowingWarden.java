package com.uiforsquid.wardenofnothing.warden;

/**
 * Implemented on {@link net.minecraft.world.entity.monster.warden.Warden} by a mixin, so the rest of
 * the mod can ask a Warden to start a forced burrow (play the vanilla dig animation now, then be saved
 * as burrowed) without reaching into its private brain/anger state.
 */
public interface BurrowingWarden {

    /**
     * Clears the Warden's target, anger and dig cooldown so the vanilla brain immediately runs the DIG
     * activity. When the dig animation finishes, the Digging behaviour removes the Warden and the mod
     * saves its slot as burrowed (F3).
     */
    void wardenofnothing$forceBurrow();

    /**
     * Called each server tick: while a forced burrow hasn't started digging yet, keeps the brain on
     * DIG (re-clearing the dig cooldown, targets and anger a disturbance may have brought back).
     */
    void wardenofnothing$tickForcedBurrow();

    /**
     * Drops all of the Warden's anger and its attack/roar targets, so a returning City Warden ignores
     * everything on the way home (F8). The outside-target sonic boom is driven separately by the mod.
     */
    void wardenofnothing$clearAllAnger();
}
