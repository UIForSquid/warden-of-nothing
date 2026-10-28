package com.uiforsquid.wardenofnothing.data;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Per-dimension saved data: every tracked city in one {@link net.minecraft.server.level.ServerLevel},
 * and the index of their natural shriekers.
 *
 * <p>The shrieker index (position &rarr; owning city) is built here from the saved cities and updated by
 * the same calls that update the cities' own lists, so the two can't drift apart. It answers the
 * block-change mixin's hot-path question in one hash lookup.
 *
 * <p>Stored under the data name {@value #DATA_NAME} in each dimension's {@code data} folder.
 */
public final class CityData extends SavedData {

    public static final String DATA_NAME = "wardenofnothing_cities";

    private final Map<Long, TrackedCity> cities = new ConcurrentHashMap<>();

    /** Natural shrieker position (packed) &rarr; the city that recorded it. Runtime-only, rebuilt on load. */
    private final Long2ObjectOpenHashMap<TrackedCity> shriekerOwners = new Long2ObjectOpenHashMap<>();

    public CityData() {
    }

    public static CityData load(CompoundTag tag, HolderLookup.Provider registries) {
        CityData data = new CityData();
        ListTag list = tag.getList("cities", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            TrackedCity city = TrackedCity.load(list.getCompound(i), data::setDirty);
            data.cities.put(city.getId(), city);
            for (long packed : city.getNaturalShriekers()) {
                data.shriekerOwners.put(packed, city);
            }
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
        return Collections.unmodifiableCollection(cities.values());
    }

    @Nullable
    public TrackedCity getCity(long id) {
        return cities.get(id);
    }

    /** The city with this id, recording it (bounds, Warden_1 burrowed at {@code maxHealth}) if it's new. */
    public TrackedCity getOrCreate(long id, BoundingBox bounds, float maxHealth) {
        TrackedCity existing = cities.get(id);
        if (existing != null) {
            return existing;
        }
        TrackedCity city = TrackedCity.fresh(id, bounds, maxHealth, this::setDirty);
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

    // ----- natural shriekers -----

    /** Records natural shriekers a city placed. Positions already recorded (by any city) are skipped. */
    public void recordShriekers(TrackedCity city, LongList positions) {
        boolean changed = false;
        for (int i = 0; i < positions.size(); i++) {
            long packed = positions.getLong(i);
            if (shriekerOwners.putIfAbsent(packed, city) == null) {
                city.addNaturalShrieker(packed);
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
    }

    /** Whether this position holds a recorded natural shrieker. The block-change mixin's hot-path check. */
    public boolean isNaturalShrieker(long packed) {
        return shriekerOwners.containsKey(packed);
    }

    /** The city that recorded the natural shrieker at this position, or null if there isn't one. */
    @Nullable
    public TrackedCity ownerOf(long packed) {
        return shriekerOwners.get(packed);
    }

    /** A recorded natural shrieker was removed from its position: drop it for good. False if none was recorded. */
    public boolean removeNaturalShrieker(long packed) {
        TrackedCity owner = shriekerOwners.remove(packed);
        if (owner == null) {
            return false;
        }
        owner.removeNaturalShrieker(packed);
        setDirty();
        return true;
    }

    /** The recorded natural shriekers inside one chunk (a fresh list, safe to remove from while iterating it). */
    public LongList naturalShriekersIn(ChunkPos chunk) {
        int minX = chunk.getMinBlockX();
        int minZ = chunk.getMinBlockZ();
        int maxX = chunk.getMaxBlockX();
        int maxZ = chunk.getMaxBlockZ();
        LongList found = new LongArrayList();
        for (TrackedCity city : cities.values()) {
            // Natural shriekers always lie inside their city's bounds: skip cities this chunk misses.
            if (!city.getBounds().intersects(minX, minZ, maxX, maxZ)) {
                continue;
            }
            for (long packed : city.getNaturalShriekers()) {
                int x = BlockPos.getX(packed);
                int z = BlockPos.getZ(packed);
                if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
                    found.add(packed);
                }
            }
        }
        return found;
    }
}
