package com.uiforsquid.wardenofnothing.city;

import com.uiforsquid.wardenofnothing.config.Config;
import com.uiforsquid.wardenofnothing.data.CityData;
import com.uiforsquid.wardenofnothing.data.TrackedCity;
import it.unimi.dsi.fastutil.longs.LongList;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;

/** Entry point for all per-dimension city state: each level's {@link CityData}. */
public final class CityTracker {

    private static final SavedData.Factory<CityData> FACTORY =
            new SavedData.Factory<>(CityData::new, CityData::load, null);

    private CityTracker() {
    }

    /** The city data for a dimension, loaded on first use and then held by the level itself. */
    public static CityData get(ServerLevel level) {
        CityDataHolder holder = (CityDataHolder) level;
        CityData data = holder.wardenofnothing$cityData();
        if (data == null) {
            data = level.getDataStorage().computeIfAbsent(FACTORY, CityData.DATA_NAME);
            holder.wardenofnothing$setCityData(data);
        }
        return data;
    }

    /**
     * Records (or tops up) a city as one of its chunks generates: makes sure the city exists with its
     * bounds and Warden_1, and adds the natural shriekers found in this chunk. Main thread only.
     */
    public static void recordChunk(ServerLevel level, long cityId, BoundingBox bounds, LongList shriekerPositions) {
        CityData data = get(level);
        float maxHealth = (float) (double) Config.MAX_HEALTH.get();
        data.recordShriekers(data.getOrCreate(cityId, bounds, maxHealth), shriekerPositions);
    }

    /** Cheap guard for the block-change mixin: is this a recorded natural shrieker position? */
    public static boolean isNaturalShrieker(Level level, long packed) {
        return level instanceof ServerLevel serverLevel && get(serverLevel).isNaturalShrieker(packed);
    }

    @Nullable
    public static TrackedCity findCity(ServerLevel level, BlockPos pos) {
        return get(level).findCityContaining(pos);
    }
}
