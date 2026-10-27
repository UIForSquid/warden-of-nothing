package com.uiforsquid.wardenofnothing.warden;

import com.uiforsquid.wardenofnothing.city.CityTracker;
import com.uiforsquid.wardenofnothing.data.TrackedCity;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.Warden;

/** Helpers for the City Warden ↔ city/slot relationship. */
public final class CityWardens {

    private CityWardens() {
    }

    public static boolean isCityWarden(Entity entity) {
        return entity instanceof Warden warden && warden.hasData(WonAttachments.CITY_WARDEN.get());
    }

    @Nullable
    public static CityWardenRef getRef(Warden warden) {
        return warden.hasData(WonAttachments.CITY_WARDEN.get()) ? warden.getData(WonAttachments.CITY_WARDEN.get()) : null;
    }

    /** Makes a Warden a City Warden of the given city + slot: sets the attachment and adds the label tag. */
    public static void mark(Warden warden, long cityId, int slotIndex) {
        warden.setData(WonAttachments.CITY_WARDEN.get(), new CityWardenRef(cityId, slotIndex));
        warden.addTag(WardenEntityTags.CITY_WARDEN_LABEL);
    }

    /** Drops all of a City Warden's anger and targets (used while it is returning home). */
    public static void clearAllAnger(Warden warden) {
        if (warden instanceof com.uiforsquid.wardenofnothing.warden.BurrowingWarden control) {
            control.wardenofnothing$clearAllAnger();
        }
    }

    /** The tracked city a City Warden belongs to, or null if it isn't a City Warden / the city is gone. */
    @Nullable
    public static TrackedCity getCity(ServerLevel level, Warden warden) {
        CityWardenRef ref = getRef(warden);
        if (ref == null) {
            return null;
        }
        return CityTracker.get(level).getCity(ref.cityId());
    }
}
