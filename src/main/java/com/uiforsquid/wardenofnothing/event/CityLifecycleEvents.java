package com.uiforsquid.wardenofnothing.event;

import com.uiforsquid.wardenofnothing.WardenOfNothing;
import com.uiforsquid.wardenofnothing.city.CityTracker;
import com.uiforsquid.wardenofnothing.heart.HeartIndex;
import com.uiforsquid.wardenofnothing.home.HomeRangeManager;
import com.uiforsquid.wardenofnothing.warden.BossBars;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Builds each dimension's city data (and its natural-shrieker index) when the dimension loads, releases
 * the per-dimension indexes when it unloads, and clears the remaining runtime state when the server stops.
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
            CityTracker.forget(serverLevel);
            HeartIndex.forget(serverLevel);
        }
    }

    /** Runtime-only state is static, and in singleplayer the next world runs in the same JVM: clear it. */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        HomeRangeManager.clear();
        BossBars.clear();
    }
}
