package com.uiforsquid.wardenofnothing.home;

import com.uiforsquid.wardenofnothing.config.Config;
import com.uiforsquid.wardenofnothing.data.TrackedCity;
import com.uiforsquid.wardenofnothing.warden.CityWardens;
import com.uiforsquid.wardenofnothing.warden.WardenBurrow;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

/**
 * F8: keeps each City Warden within its home range. Driven from {@link #tick} every server tick (for
 * resolving a charging boom) and, for the heavier checks, every {@code homeCheckTicks}. Reproduces the
 * designed behaviour on top of vanilla AI, since the Warden brain has no event hooks.
 */
public final class HomeRangeManager {

    private static final Map<UUID, WardenHomeState> STATES = new ConcurrentHashMap<>();

    private HomeRangeManager() {
    }

    public static void forget(UUID wardenId) {
        STATES.remove(wardenId);
    }

    /** Drops all runtime state (server stopping), so nothing leaks into the next world. */
    public static void clear() {
        STATES.clear();
    }

    /** Called every tick for a City Warden. */
    public static void tick(Warden warden, ServerLevel level) {
        // Leave a digging/emerging Warden alone (it is already burrowing, or just climbed out).
        if (warden.hasPose(Pose.DIGGING) || warden.hasPose(Pose.EMERGING)) {
            return;
        }

        // Only a charging boom needs every tick; everything else waits for the next check tick.
        int checkTicks = Config.HOME_CHECK_TICKS.get();
        boolean checkTick = warden.tickCount % checkTicks == 0;
        WardenHomeState st = STATES.get(warden.getUUID());
        if (!checkTick && (st == null || st.boomTargetId == null)) {
            return;
        }

        TrackedCity city = CityWardens.getCity(level, warden);
        if (city == null) {
            return;
        }
        if (st == null) {
            st = new WardenHomeState();
            STATES.put(warden.getUUID(), st);
        }
        long now = level.getGameTime();

        // Resolve a charging boom (checked every tick so it fires on time).
        if (st.boomTargetId != null) {
            Entity target = level.getEntity(st.boomTargetId);
            if (now >= st.boomFireAt) {
                st.boomTargetId = null;
                if (target instanceof LivingEntity victim && victim.isAlive()
                        && warden.closerThan(victim, Config.SONIC_BOOM_RANGE_HORIZONTAL.get(), Config.SONIC_BOOM_RANGE_VERTICAL.get())
                        && warden.hasLineOfSight(victim)) {
                    fireBoom(level, warden, victim);
                }
                st.returning = true;
            } else if (target != null) {
                warden.getLookControl().setLookAt(target.position());
            }
        }

        if (!checkTick) {
            return;
        }

        BoundingBox bounds = city.getBounds();
        BoundingBox home = HomeRange.homeRangeBox(level, bounds);
        Vec3 pos = warden.position();
        boolean inBounds = bounds.isInside(BlockPos.containing(pos));
        boolean inHome = HomeRange.isInside(home, pos);

        // Hit-counter reset after staying inside the city bounds long enough.
        if (inBounds) {
            st.ticksInsideBounds += checkTicks;
            if (st.ticksInsideBounds >= Config.RETURN_HIT_RESET_SECONDS.get() * 20) {
                st.returnHits = 0;
            }
        } else {
            st.ticksInsideBounds = 0;
        }

        // Outside-target sonic boom rule: anger from an entity outside the home range.
        Optional<LivingEntity> angry = warden.getEntityAngryAt();
        if (angry.isPresent() && !HomeRange.isInside(home, angry.get().position())) {
            LivingEntity outside = angry.get();
            if (st.boomTargetId == null && now >= st.boomCooldownUntil
                    && warden.closerThan(outside, Config.SONIC_BOOM_RANGE_HORIZONTAL.get(), Config.SONIC_BOOM_RANGE_VERTICAL.get())
                    && warden.hasLineOfSight(outside)) {
                startBoomCharge(level, warden, st, outside, now);
            }
            // Never chase an outside target: give it up and return.
            warden.clearAnger(outside);
            warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            st.returning = true;
        }

        // A current target that walks out of the home range is given up; the Warden returns.
        LivingEntity target = warden.getTarget();
        if (target != null && !HomeRange.isInside(home, target.position())) {
            warden.clearAnger(target);
            warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            st.returning = true;
        }

        // Physically outside the home range: walk back if close, burrow if far or stuck.
        if (!inHome) {
            double outside = HomeRange.distanceOutside(home, pos);
            if (outside > Config.WALK_BACK_DISTANCE.get()) {
                WardenBurrow.forceBurrow(warden);
                STATES.remove(warden.getUUID());
                return;
            }
            st.returning = true;
            if (st.walkBackStartTime == 0L) {
                st.walkBackStartTime = now;
            } else if (now - st.walkBackStartTime > Config.WALK_BACK_TIMEOUT_SECONDS.get() * 20L) {
                WardenBurrow.forceBurrow(warden);
                STATES.remove(warden.getUUID());
                return;
            }
        } else {
            st.walkBackStartTime = 0L;
            if (inBounds) {
                st.returning = false;
            }
        }

        // While returning, ignore everything and walk to the nearest point inside the city bounds.
        if (st.returning && !inBounds) {
            CityWardens.clearAllAnger(warden);
            Vec3 destination = HomeRange.nearestInside(bounds, pos);
            warden.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(destination, 1.0F, 0));
        }
    }

    /** Called from the damage event: count a hit if the Warden is returning. */
    public static void onDamagedWhileReturning(Warden warden) {
        WardenHomeState st = STATES.get(warden.getUUID());
        if (st == null || !st.returning) {
            return;
        }
        st.returnHits++;
        if (st.returnHits >= Config.RETURN_HITS_BEFORE_BURROW.get()) {
            WardenBurrow.forceBurrow(warden);
            STATES.remove(warden.getUUID());
        }
    }

    public static boolean isReturning(Warden warden) {
        WardenHomeState st = STATES.get(warden.getUUID());
        return st != null && st.returning;
    }

    private static void startBoomCharge(ServerLevel level, Warden warden, WardenHomeState st, LivingEntity target, long now) {
        st.boomTargetId = target.getUUID();
        st.boomFireAt = now + Config.SONIC_BOOM_CHARGE_TIME.get();
        st.boomCooldownUntil = now + Config.SONIC_BOOM_COOLDOWN.get();
        warden.getLookControl().setLookAt(target.position());
        warden.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 3.0F, 1.0F);
        level.broadcastEntityEvent(warden, (byte) 62); // triggers the sonic boom animation client-side
    }

    private static void fireBoom(ServerLevel level, Warden warden, LivingEntity victim) {
        warden.playSound(SoundEvents.WARDEN_SONIC_BOOM, 3.0F, 1.0F);
        Vec3 from = warden.getEyePosition();
        Vec3 delta = victim.getEyePosition().subtract(from);
        Vec3 direction = delta.normalize();
        int steps = Mth.floor(delta.length()) + 7;
        for (int j = 1; j < steps; j++) {
            Vec3 at = from.add(direction.scale(j));
            level.sendParticles(ParticleTypes.SONIC_BOOM, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        // Damage (and the extra knockback) are rescaled for the boss by BossWardenEvents.onIncomingDamage;
        // the base push is vanilla's, applied as vanilla does only when the hit lands.
        if (victim.hurt(level.damageSources().sonicBoom(warden), 10.0F)) {
            double resistance = 1.0 - victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            victim.push(direction.x() * 2.5 * resistance, direction.y() * 0.5 * resistance, direction.z() * 2.5 * resistance);
        }
    }
}
