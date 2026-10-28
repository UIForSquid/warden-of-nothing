package com.uiforsquid.wardenofnothing.event;

import com.uiforsquid.wardenofnothing.WardenOfNothing;
import com.uiforsquid.wardenofnothing.config.Config;
import com.uiforsquid.wardenofnothing.home.HomeRangeManager;
import com.uiforsquid.wardenofnothing.warden.BossBars;
import com.uiforsquid.wardenofnothing.warden.BossWarden;
import com.uiforsquid.wardenofnothing.warden.BurrowingWarden;
import com.uiforsquid.wardenofnothing.warden.CityWardens;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * The single per-tick hook for server Wardens. {@link EntityTickEvent} fires for every entity every
 * tick, so all Warden tick work shares one listener (one dispatch and one type check per entity).
 */
@EventBusSubscriber(modid = WardenOfNothing.MOD_ID)
public final class WardenTickEvents {

    private WardenTickEvents() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Warden warden) || !(warden.level() instanceof ServerLevel level)) {
            return;
        }

        // F9 "no vehicles of any kind": the vanilla canRide override already stops a Warden starting to
        // ride; this also dismounts one that is somehow already riding (old save, another mod).
        if (warden.isPassenger() && Config.WARDENS_UNMOVABLE.get()) {
            warden.stopRiding();
        }

        // F3: hold a forced burrow until the dig animation actually starts.
        ((BurrowingWarden) warden).wardenofnothing$tickForcedBurrow();

        boolean cityWarden = CityWardens.isCityWarden(warden);

        // F1: keep each boss Warden's boss bar in sync; drop the bar when a Warden is no longer a boss.
        if (BossWarden.isBoss(cityWarden) && warden.isAlive()) {
            BossBars.update(warden, level);
        } else {
            BossBars.remove(warden);
        }

        // F8: the home-range supervisor.
        if (cityWarden) {
            HomeRangeManager.tick(warden, level);
        }
    }
}
