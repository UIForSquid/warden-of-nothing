package com.uiforsquid.wardenofnothing.home;

import com.uiforsquid.wardenofnothing.config.Config;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

/**
 * F8 rules: what a City Warden does about its home range, decided from plain values: positions, the
 * game time, the Warden's own {@link WardenHomeState} (the rules' memory, which they update) and the
 * settings. No entity or level is touched here; {@link HomeRangeManager} gathers the {@link Situation}
 * from the world and carries out the {@link Verdict}. So every rule can be checked directly.
 *
 * <p>The rules, in order: a charging outside-target boom fires on time; the return-hit counter resets
 * after long enough inside the city; an angry target outside the home range is given up (and boomed,
 * if in reach and off cooldown); a target that leaves the home range is given up; a Warden outside the
 * home range walks back, unless it is too far out or has been walking back too long, when it burrows;
 * while returning it ignores everything and walks to the nearest point inside the city.
 */
public final class HomeRangeRules {

    /** The tunables the rules read, in ticks and blocks. */
    public record Settings(int checkTicks, int returnHitResetTicks, int returnHitsBeforeBurrow,
                           double walkBackDistance, long walkBackTimeoutTicks,
                           long boomChargeTicks, long boomCooldownTicks) {

        public static Settings fromConfig() {
            return new Settings(
                    Config.HOME_CHECK_TICKS.get(),
                    Config.RETURN_HIT_RESET_SECONDS.get() * 20,
                    Config.RETURN_HITS_BEFORE_BURROW.get(),
                    Config.WALK_BACK_DISTANCE.get(),
                    Config.WALK_BACK_TIMEOUT_SECONDS.get() * 20L,
                    Config.SONIC_BOOM_CHARGE_TIME.get(),
                    Config.SONIC_BOOM_COOLDOWN.get());
        }
    }

    /**
     * What the Warden faces at a check. {@code angryAtInReach} (range and line of sight) is asked only
     * if a boom could start, since line of sight is a raycast.
     */
    public record Situation(long now, Vec3 pos, BoundingBox cityBounds, BoundingBox homeRange,
                            @Nullable UUID angryAtId, @Nullable Vec3 angryAtPos, BooleanSupplier angryAtInReach,
                            @Nullable Vec3 targetPos) {
    }

    /**
     * What to do. {@code startBoomAt} is the outside target to start charging at (or null). A burrow
     * means the Warden digs now (and its home state starts over); otherwise {@code walkTo}, if set, is
     * where a returning Warden walks, ignoring everything on the way.
     */
    public record Verdict(@Nullable UUID startBoomAt, boolean giveUpAngryTarget, boolean giveUpTarget,
                          boolean burrow, @Nullable Vec3 walkTo) {
    }

    /** One tick of a charging outside-target boom. */
    public enum BoomStep {
        /** Not charging. */
        NONE,
        /** Still charging: keep looking at the target. */
        CHARGING,
        /** Charge done and the target is still in reach: fire. */
        FIRE,
        /** Charge done but the target is gone or out of reach: nothing fires. */
        FIZZLE
    }

    private HomeRangeRules() {
    }

    /**
     * Advances a charging boom (run every tick, so it fires on time). When the charge completes the
     * Warden is returning either way; {@code targetInReach} is asked only then.
     */
    public static BoomStep stepBoom(WardenHomeState st, long now, BooleanSupplier targetInReach) {
        if (st.boomTargetId == null) {
            return BoomStep.NONE;
        }
        if (now < st.boomFireAt) {
            return BoomStep.CHARGING;
        }
        st.boomTargetId = null;
        st.returning = true;
        return targetInReach.getAsBoolean() ? BoomStep.FIRE : BoomStep.FIZZLE;
    }

    /** The every-{@code checkTicks} decision. */
    public static Verdict check(WardenHomeState st, Situation s, Settings settings) {
        boolean inBounds = s.cityBounds().isInside(BlockPos.containing(s.pos()));
        boolean inHome = HomeRange.isInside(s.homeRange(), s.pos());

        // Hit-counter reset after staying inside the city bounds long enough.
        if (inBounds) {
            st.ticksInsideBounds += settings.checkTicks();
            if (st.ticksInsideBounds >= settings.returnHitResetTicks()) {
                st.returnHits = 0;
            }
        } else {
            st.ticksInsideBounds = 0;
        }

        // Outside-target sonic boom rule: anger from an entity outside the home range is never chased.
        UUID startBoomAt = null;
        boolean giveUpAngry = false;
        if (s.angryAtPos() != null && !HomeRange.isInside(s.homeRange(), s.angryAtPos())) {
            if (st.boomTargetId == null && s.now() >= st.boomCooldownUntil && s.angryAtInReach().getAsBoolean()) {
                startBoomAt = s.angryAtId();
                st.boomTargetId = startBoomAt;
                st.boomFireAt = s.now() + settings.boomChargeTicks();
                st.boomCooldownUntil = s.now() + settings.boomCooldownTicks();
            }
            giveUpAngry = true;
            st.returning = true;
        }

        // A current target that walks out of the home range is given up.
        boolean giveUpTarget = s.targetPos() != null && !HomeRange.isInside(s.homeRange(), s.targetPos());
        if (giveUpTarget) {
            st.returning = true;
        }

        // Physically outside the home range: walk back if close, burrow if far or stuck.
        if (!inHome) {
            if (HomeRange.distanceOutside(s.homeRange(), s.pos()) > settings.walkBackDistance()) {
                return new Verdict(startBoomAt, giveUpAngry, giveUpTarget, true, null);
            }
            st.returning = true;
            if (st.walkBackStartTime == 0L) {
                st.walkBackStartTime = s.now();
            } else if (s.now() - st.walkBackStartTime > settings.walkBackTimeoutTicks()) {
                return new Verdict(startBoomAt, giveUpAngry, giveUpTarget, true, null);
            }
        } else {
            st.walkBackStartTime = 0L;
            if (inBounds) {
                st.returning = false;
            }
        }

        Vec3 walkTo = st.returning && !inBounds ? HomeRange.nearestInside(s.cityBounds(), s.pos()) : null;
        return new Verdict(startBoomAt, giveUpAngry, giveUpTarget, false, walkTo);
    }

    /** A City Warden took damage. Counts it if returning; true once the hits mean it should burrow. */
    public static boolean hitWhileReturning(WardenHomeState st, int returnHitsBeforeBurrow) {
        if (!st.returning) {
            return false;
        }
        st.returnHits++;
        return st.returnHits >= returnHitsBeforeBurrow;
    }
}
