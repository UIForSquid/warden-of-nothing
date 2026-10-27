package com.uiforsquid.wardenofnothing.data;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Per-dimension saved data: every tracked city in one {@link net.minecraft.server.level.ServerLevel}.
 *
 * <p>Stored under the data name {@value #DATA_NAME} in each dimension's {@code data} folder.
 */
public final class CityData extends SavedData {

    public static final String DATA_NAME = "wardenofnothing_cities";

    private final Map<Long, TrackedCity> cities = new ConcurrentHashMap<>();

    public CityData() {
    }

    public static CityData load(CompoundTag tag, HolderLookup.Provider registries) {
        CityData data = new CityData();
        ListTag list = tag.getList("cities", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            TrackedCity city = TrackedCity.load(list.getCompound(i));
            data.cities.put(city.getId(), city);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (TrackedCity city : cities.values()) {
            list.add(city.save());
        }
        tag.put("cities", list);
        return tag;
    }

    public Collection<TrackedCity> getCities() {
        return cities.values();
    }

    @Nullable
    public TrackedCity getCity(long id) {
        return cities.get(id);
    }

    public TrackedCity getOrCreate(long id, net.minecraft.world.level.levelgen.structure.BoundingBox bounds, float maxHealth) {
        TrackedCity existing = cities.get(id);
        if (existing != null) {
            return existing;
        }
        TrackedCity city = TrackedCity.fresh(id, bounds, maxHealth);
        cities.put(id, city);
        setDirty();
        return city;
    }

    /** The tracked city whose bounds contain the position, or null if none does. */
    @Nullable
    public TrackedCity findCityContaining(BlockPos pos) {
        for (TrackedCity city : cities.values()) {
            if (city.containsPosition(pos)) {
                return city;
            }
        }
        return null;
    }
}
