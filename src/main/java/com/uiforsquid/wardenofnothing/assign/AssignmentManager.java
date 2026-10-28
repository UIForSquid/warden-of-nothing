package com.uiforsquid.wardenofnothing.assign;

import com.uiforsquid.wardenofnothing.city.CityShriekers;
import com.uiforsquid.wardenofnothing.city.CityTracker;
import com.uiforsquid.wardenofnothing.config.Config;
import com.uiforsquid.wardenofnothing.data.TrackedCity;
import com.uiforsquid.wardenofnothing.warden.BossWarden;
import com.uiforsquid.wardenofnothing.warden.CityWardens;
import com.uiforsquid.wardenofnothing.warden.WardenEntityTags;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.warden.Warden;

/** F7: processes {@code wardenofnothing.assigned} requests, filling a city slot or refusing. */
public final class AssignmentManager {

    private AssignmentManager() {
    }

    /** Finds every Warden tagged with the request, processes it, and always removes the request tag. */
    public static void processRequests(ServerLevel level) {
        List<? extends Warden> requested =
                level.getEntities(EntityType.WARDEN, w -> w.getTags().contains(WardenEntityTags.ASSIGNED_REQUEST));
        for (Warden warden : requested) {
            warden.removeTag(WardenEntityTags.ASSIGNED_REQUEST);
            process(level, warden);
        }
    }

    private static void process(ServerLevel level, Warden warden) {
        if (CityWardens.isCityWarden(warden)) {
            return; // already a City Warden: ignored, no message (the tag was already removed).
        }
        TrackedCity city = CityTracker.findCity(level, warden.blockPosition());
        if (city == null) {
            refuse(level.getServer(), "not_in_city");
            return;
        }
        if (!city.hasRoomFor(Config.MAX_ASSIGNABLE_WARDENS.get())) {
            refuse(level.getServer(), "warden_alive");
            return;
        }

        // The assigned Warden keeps its current health (decision 53) and gets boss stats in every way.
        TrackedCity.Claim claim = city.claim(warden.getUUID(), warden.getHealth(), level.getGameTime());
        BossWarden.applyStats(warden);
        CityWardens.mark(warden, city.getId(), claim.slotIndex());
        if (claim.revivedCity()) {
            CityShriekers.refreshLoaded(level, city); // respawn routine: natural shriekers switch back on
        }
    }

    private static void refuse(MinecraftServer server, String key) {
        if (server == null) {
            return;
        }
        Component message = Component.translatable("message.wardenofnothing.assign." + key).withStyle(ChatFormatting.BLUE);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.hasPermissions(2)) {
                player.sendSystemMessage(message);
            }
        }
    }
}
