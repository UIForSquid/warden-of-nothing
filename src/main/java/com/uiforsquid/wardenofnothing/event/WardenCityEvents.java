package com.uiforsquid.wardenofnothing.event;

import com.uiforsquid.wardenofnothing.WardenOfNothing;
import com.uiforsquid.wardenofnothing.city.CityShriekers;
import com.uiforsquid.wardenofnothing.city.CityTracker;
import com.uiforsquid.wardenofnothing.data.CityData;
import com.uiforsquid.wardenofnothing.data.SlotState;
import com.uiforsquid.wardenofnothing.data.TrackedCity;
import com.uiforsquid.wardenofnothing.data.WardenSlot;
import com.uiforsquid.wardenofnothing.warden.CityWardenRef;
import com.uiforsquid.wardenofnothing.warden.CityWardens;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** F2 death/defeat handling and the chunk-load switch for shriekers in unloaded chunks. */
@EventBusSubscriber(modid = WardenOfNothing.MOD_ID)
public final class WardenCityEvents {

    private WardenCityEvents() {
    }

    /**
     * Lowest priority and not for cancelled events: a death another mod cancels (a totem-style save)
     * must not mark the slot dead or drop the Heart.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onWardenDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Warden warden) || !(warden.level() instanceof ServerLevel level)) {
            return;
        }
        CityWardenRef ref = CityWardens.getRef(warden);
        if (ref == null) {
            return; // Unassigned Warden: ignored by all city logic.
        }
        CityData data = CityTracker.get(level);
        TrackedCity city = data.getCity(ref.cityId());
        WardenSlot slot = city == null ? null : city.outSlotOf(ref.slotIndex(), warden.getUUID());
        if (slot == null) {
            return; // stale Warden: its slot is no longer held by it
        }
        slot.setState(SlotState.DEAD);
        slot.setOutWardenId(null);
        data.setDirty();

        if (city.isDefeated()) {
            // Every slot is now dead: the city is defeated, so its natural shriekers stop summoning.
            CityShriekers.refreshLoaded(level, city);
            // F5: the last registered Warden (whose death defeats the city) drops a Heart (Dead).
            net.minecraft.world.level.block.Block.popResource(
                    level, warden.blockPosition(),
                    new net.minecraft.world.item.ItemStack(com.uiforsquid.wardenofnothing.reg.WonItems.HEART.get()));
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            CityShriekers.onChunkLoaded(level, chunk);
        }
    }
}
