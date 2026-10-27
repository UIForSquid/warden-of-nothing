package com.uiforsquid.wardenofnothing.city;

import com.uiforsquid.wardenofnothing.config.Config;
import com.uiforsquid.wardenofnothing.data.CityData;
import com.uiforsquid.wardenofnothing.data.TrackedCity;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Entry point for all per-dimension city state. Owns the {@link CityData} saved data and a fast
 * position index of natural shriekers used by the block-removal mixin.
 */
public final class CityTracker {

    private static final SavedData.Factory<CityData> FACTORY =
            new SavedData.Factory<>(CityData::new, CityData::load, null);

    /** Fast per-dimension lookup of natural shrieker positions (packed longs), for the removal mixin. */
    private static final Map<ResourceKey<Level>, LongOpenHashSet> SHRIEKER_INDEX = new ConcurrentHashMap<>();

    private CityTracker() {
    }

    /** Fetches (loading if needed) the city data for a dimension and makes sure its shrieker index is built. */
    public static CityData get(ServerLevel level) {
        CityData data = level.getDataStorage().computeIfAbsent(FACTORY, CityData.DATA_NAME);
        index(level, data);
        return data;
    }

    /** Called when a dimension unloads, so its index doesn't leak. */
    public static void forget(ServerLevel level) {
        SHRIEKER_INDEX.remove(level.dimension());
    }

    /** The dimension's shrieker index, built from its city data the first time it is needed. */
    private static LongOpenHashSet index(ServerLevel level, CityData data) {
        LongOpenHashSet existing = SHRIEKER_INDEX.get(level.dimension());
        if (existing != null) {
            return existing;
        }
        return SHRIEKER_INDEX.computeIfAbsent(level.dimension(), key -> {
            LongOpenHashSet set = new LongOpenHashSet();
            for (TrackedCity city : data.getCities()) {
                set.addAll(city.getNaturalShriekers());
            }
            return set;
        });
    }

    // --- Generation recording (called from the main thread via server.execute) ---

    /**
     * Records (or tops up) a city as one of its chunks generates: makes sure the city exists with its
     * bounds and Warden_1, and adds the natural shriekers found in this chunk.
     */
    public static void recordChunk(ServerLevel level, long cityId, BoundingBox bounds, LongList shriekerPositions) {
        CityData data = level.getDataStorage().computeIfAbsent(FACTORY, CityData.DATA_NAME);
        float maxHealth = (float) (double) Config.MAX_HEALTH.get();
        TrackedCity city = data.getOrCreate(cityId, bounds, maxHealth);
        LongOpenHashSet set = index(level, data);
        boolean changed = false;
        for (int i = 0; i < shriekerPositions.size(); i++) {
            long packed = shriekerPositions.getLong(i);
            // The dimension-wide index already holds every recorded shrieker, so it is the O(1) dedupe.
            if (set.add(packed)) {
                city.addNaturalShrieker(packed);
                changed = true;
            }
        }
        if (changed) {
            data.setDirty();
        }
    }

    // --- Natural shrieker removal (called from the block-set mixin, main thread) ---

    /** Cheap guard for the mixin: is this a recorded natural shrieker position? */
    public static boolean isNaturalShrieker(Level level, long packed) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        LongOpenHashSet set = SHRIEKER_INDEX.get(serverLevel.dimension());
        return set != null && set.contains(packed);
    }

    /** A recorded natural shrieker was removed from its position; drop it from the record for good. */
    public static void onNaturalShriekerRemoved(ServerLevel level, long packed) {
        LongOpenHashSet set = SHRIEKER_INDEX.get(level.dimension());
        if (set == null || !set.remove(packed)) {
            return;
        }
        CityData data = get(level);
        TrackedCity city = data.findCityContaining(BlockPos.of(packed));
        if (city == null) {
            // Not inside any known city bounds; still remove from whichever list holds it.
            for (TrackedCity c : data.getCities()) {
                if (c.removeNaturalShrieker(packed)) {
                    break;
                }
            }
        } else {
            city.removeNaturalShrieker(packed);
        }
        data.setDirty();
    }

    @Nullable
    public static TrackedCity findCity(ServerLevel level, BlockPos pos) {
        return get(level).findCityContaining(pos);
    }
}
