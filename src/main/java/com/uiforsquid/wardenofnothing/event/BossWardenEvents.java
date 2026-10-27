package com.uiforsquid.wardenofnothing.event;

import com.uiforsquid.wardenofnothing.WardenOfNothing;
import com.uiforsquid.wardenofnothing.config.Config;
import com.uiforsquid.wardenofnothing.warden.BossBars;
import com.uiforsquid.wardenofnothing.warden.BossWarden;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * F1: boss stats on spawn, boss bar cleanup, and the sonic boom damage/knockback changes. The per-tick
 * boss bar update lives in {@link WardenTickEvents}.
 */
@EventBusSubscriber(modid = WardenOfNothing.MOD_ID)
public final class BossWardenEvents {

    private BossWardenEvents() {
    }

    /** Give boss stats to a Warden as it enters the world (city Wardens get them in the summoner too). */
    @SubscribeEvent
    public static void onWardenJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Warden warden) || !(event.getLevel() instanceof ServerLevel)) {
            return;
        }
        if (!BossWarden.isBoss(warden)) {
            return;
        }
        BossWarden.applyStats(warden);
        if (!event.loadedFromDisk()) {
            // A fresh boss spawns at full health. A Warden re-loaded from disk keeps its saved health.
            warden.setHealth(warden.getMaxHealth());
        }
    }

    /** Hide and forget a Warden's boss bar when it leaves the world (death, unload, dimension change). */
    @SubscribeEvent
    public static void onWardenLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Warden warden) {
            BossBars.remove(warden.getUUID());
        }
    }

    /**
     * F1 sonic boom changes. The vanilla boom deals a flat 10 and pushes by fixed amounts, both
     * hardcoded inside a lambda. We set the damage to {@code sonicBoomDamage} and add extra knockback
     * so the total is about {@code sonicBoomKnockbackMultiplier}x vanilla. Boss Wardens only; other
     * Wardens keep the vanilla boom. Applied victim-side so it is robust and difficulty-independent.
     */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (!source.is(DamageTypes.SONIC_BOOM) || !(source.getEntity() instanceof Warden warden)) {
            return;
        }
        if (!BossWarden.isBoss(warden)) {
            return;
        }
        event.setAmount((float) (double) Config.SONIC_BOOM_DAMAGE.get());

        double multiplier = Config.SONIC_BOOM_KNOCKBACK_MULTIPLIER.get();
        if (multiplier > 1.0) {
            LivingEntity victim = event.getEntity();
            double resistance = victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            double extra = multiplier - 1.0;
            // Vanilla boom push is 2.5 horizontal / 0.5 vertical, scaled by (1 - knockback resistance).
            double horizontal = 2.5 * (1.0 - resistance) * extra;
            double vertical = 0.5 * (1.0 - resistance) * extra;
            if (horizontal != 0.0 || vertical != 0.0) {
                Vec3 direction = victim.getEyePosition().subtract(warden.getEyePosition()).normalize();
                victim.push(direction.x() * horizontal, direction.y() * vertical, direction.z() * horizontal);
            }
        }
    }
}
