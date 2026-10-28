package com.uiforsquid.wardenofnothing.event;

import com.uiforsquid.wardenofnothing.WardenOfNothing;
import com.uiforsquid.wardenofnothing.city.CityTracker;
import com.uiforsquid.wardenofnothing.heart.HeartIndex;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Loads each dimension's city data (and with it the natural-shrieker index) when the dimension loads,
 * and drops its placed-Heart index when it unloads. Per-Warden runtime state lives on the Warden
 * entities, so nothing else needs clearing.
 */
@EventBusSubscriber(modid = WardenOfNothing.MOD_ID)
public final class CityLifecycleEvents {

    private CityLifecycleEvents() {
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            CityTracker.get(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            HeartIndex.forget(serverLevel);
        }
    }
}
