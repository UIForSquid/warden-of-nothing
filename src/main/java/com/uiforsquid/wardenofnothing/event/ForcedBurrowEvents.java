package com.uiforsquid.wardenofnothing.event;

import com.uiforsquid.wardenofnothing.WardenOfNothing;
import com.uiforsquid.wardenofnothing.config.Config;
import com.uiforsquid.wardenofnothing.warden.CityWardens;
import com.uiforsquid.wardenofnothing.warden.WardenBurrow;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;

/** F3 forced burrows that don't go through the dig timer: dimension change and removal by another mod. */
@EventBusSubscriber(modid = WardenOfNothing.MOD_ID)
public final class ForcedBurrowEvents {

    private ForcedBurrowEvents() {
    }

    /** Case 1: a City Warden that would change dimension burrows at home instead (if burrowOnTeleport). */
    @SubscribeEvent
    public static void onTravel(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof Warden warden) || !(warden.level() instanceof ServerLevel)) {
            return;
        }
        if (CityWardens.isCityWarden(warden) && Config.BURROW_ON_TELEPORT.get()) {
            event.setCanceled(true);
            WardenBurrow.forceBurrow(warden);
        }
    }

    /**
     * Case 4: a City Warden removed by another mod (not killed, teleported or unloaded) is saved as
     * burrowed with its last health. Normal and forced burrows also end in a DISCARDED removal, but by
     * then the slot is already burrowed, so {@link WardenBurrow#onBurrowed} is a guarded no-op. Chunk
     * unloads (UNLOADED_*) and dimension changes are deliberately not treated as burrows here.
     */
    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof Warden warden) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (warden.getRemovalReason() == Entity.RemovalReason.DISCARDED) {
            WardenBurrow.onBurrowed(warden, level);
        }
    }
}
