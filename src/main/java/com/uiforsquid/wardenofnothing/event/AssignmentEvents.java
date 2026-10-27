package com.uiforsquid.wardenofnothing.event;

import com.uiforsquid.wardenofnothing.WardenOfNothing;
import com.uiforsquid.wardenofnothing.assign.AssignmentManager;
import com.uiforsquid.wardenofnothing.config.Config;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** F7: scans for assignment requests every {@code assignCheckTicks}. */
@EventBusSubscriber(modid = WardenOfNothing.MOD_ID)
public final class AssignmentEvents {

    private AssignmentEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int interval = Config.ASSIGN_CHECK_TICKS.get();
        if (server.getTickCount() % interval != 0) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            AssignmentManager.processRequests(level);
        }
    }
}
