package com.uiforsquid.wardenofnothing.home;

import com.uiforsquid.wardenofnothing.config.Config;
import com.uiforsquid.wardenofnothing.data.TrackedCity;
import com.uiforsquid.wardenofnothing.warden.CityWardens;
import com.uiforsquid.wardenofnothing.warden.WardenBurrow;
import com.uiforsquid.wardenofnothing.warden.WardenRuntime;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
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
 * F8: keeps each City Warden within its home range. The decisions are {@link HomeRangeRules}; this is
 * the adapter between them and the world: each tick it advances a charging boom, and every
 * {@code homeCheckTicks} it reads the Warden's situation, asks the rules, and carries out the verdict on
 * the Warden's brain (the Warden brain has no event hooks, so behaviour is steered from outside). The
 * rules' memory is the Warden's own {@link WardenHomeState}, held in its {@link WardenRuntime}.
 */
public final class HomeRangeManager {

    private HomeRangeManager() {
    }

    /** Called every tick for a City Warden. */
    public static void tick(Warden warden, ServerLevel level) {
        // Leave a digging/emerging Warden alone (it is already burrowing, or just climbed out).
        if (warden.hasPose(Pose.DIGGING) || warden.hasPose(Pose.EMERGING)) {
            return;
        }
        WardenRuntime runtime = WardenRuntime.of(warden);
        WardenHomeState st = runtime.home();
        long now = level.getGameTime();

        if (st.boomTargetId != null) {
            Entity boomTarget = level.getEntity(st.boomTargetId);
            switch (HomeRangeRules.stepBoom(st, now, () -> boomTarget instanceof LivingEntity victim && inBoomReach(warden, victim))) {
                case FIRE -> fireBoom(level, warden, (LivingEntity) boomTarget);
                case CHARGING -> {
                    if (boomTarget != null) {
                        warden.getLookControl().setLookAt(boomTarget.position());
                    }
                }
                default -> {
                }
            }
        }

        if (warden.tickCount % Config.HOME_CHECK_TICKS.get() != 0) {
            return;
        }
        TrackedCity city = CityWardens.getCity(level, warden);
        if (city == null) {
            return;
        }

        BoundingBox bounds = city.getBounds();
        LivingEntity angryAt = warden.getEntityAngryAt().orElse(null);
        LivingEntity target = warden.getTarget();
        HomeRangeRules.Verdict verdict = HomeRangeRules.check(st, new HomeRangeRules.Situation(
                now, warden.position(), bounds, HomeRange.homeRangeBox(level, bounds),
                angryAt == null ? null : angryAt.getUUID(),
                angryAt == null ? null : angryAt.position(),
                () -> angryAt != null && inBoomReach(warden, angryAt),
                target == null ? null : target.position()), HomeRangeRules.Settings.fromConfig());

        if (verdict.startBoomAt() != null && angryAt != null) {
            startBoomCharge(level, warden, angryAt);
        }
        if (verdict.giveUpAngryTarget() && angryAt != null) {
            giveUp(warden, angryAt);
        }
        if (verdict.giveUpTarget() && target != null) {
            giveUp(warden, target);
        }
        if (verdict.burrow()) {
            WardenBurrow.forceBurrow(warden);
            runtime.resetHome();
            return;
        }
        if (verdict.walkTo() != null) {
            CityWardens.clearAllAnger(warden);
            warden.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(verdict.walkTo(), 1.0F, 0));
        }
    }

    /** Called from the damage event: count a hit if the Warden is returning, and burrow at the limit. */
    public static void onDamagedWhileReturning(Warden warden) {
        WardenRuntime runtime = WardenRuntime.of(warden);
        if (HomeRangeRules.hitWhileReturning(runtime.home(), Config.RETURN_HITS_BEFORE_BURROW.get())) {
            WardenBurrow.forceBurrow(warden);
            runtime.resetHome();
        }
    }

    private static boolean inBoomReach(Warden warden, LivingEntity victim) {
        return victim.isAlive()
                && warden.closerThan(victim, Config.SONIC_BOOM_RANGE_HORIZONTAL.get(), Config.SONIC_BOOM_RANGE_VERTICAL.get())
                && warden.hasLineOfSight(victim);
    }

    private static void giveUp(Warden warden, LivingEntity entity) {
        warden.clearAnger(entity);
        warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
    }

    private static void startBoomCharge(ServerLevel level, Warden warden, LivingEntity target) {
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
