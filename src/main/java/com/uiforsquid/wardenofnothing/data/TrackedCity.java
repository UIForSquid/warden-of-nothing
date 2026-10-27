package com.uiforsquid.wardenofnothing.data;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * An Ancient City the mod recorded as it generated: its bounds, its natural shriekers, and its
 * ordered list of Warden slots.
 */
public final class TrackedCity {

    /** Stable id: the packed long of the structure start's origin chunk. */
    private final long id;

    /** The vanilla structure bounding box (whole city). "The edge of the city" is the edge of this. */
    private final BoundingBox bounds;

    /** Positions of shriekers the structure placed, that have not since been removed. */
    private final LongList naturalShriekers;

    /** Ordered Warden slots: index 0 = Warden_1, index 1 = Warden_2, ... */
    private final List<WardenSlot> slots;

    public TrackedCity(long id, BoundingBox bounds, LongList naturalShriekers, List<WardenSlot> slots) {
        this.id = id;
        this.bounds = bounds;
        this.naturalShriekers = naturalShriekers;
        this.slots = slots;
    }

    /** A freshly recorded city: no shriekers yet (filled in as chunks generate) and Warden_1 burrowed. */
    public static TrackedCity fresh(long id, BoundingBox bounds, float maxHealth) {
        List<WardenSlot> slots = new ArrayList<>();
        slots.add(WardenSlot.newBurrowed(maxHealth));
        return new TrackedCity(id, bounds, new LongArrayList(), slots);
    }

    public long getId() {
        return id;
    }

    public BoundingBox getBounds() {
        return bounds;
    }

    public LongList getNaturalShriekers() {
        return naturalShriekers;
    }

    /** Records a natural shrieker. The caller dedupes (via the dimension's shrieker index). */
    public void addNaturalShrieker(long packed) {
        naturalShriekers.add(packed);
    }

    /** Drops a natural shrieker from the record. Never re-added, since shriekers are only recorded at generation. */
    public boolean removeNaturalShrieker(long packed) {
        return naturalShriekers.rem(packed);
    }

    public List<WardenSlot> getSlots() {
        return slots;
    }

    /**
     * The slot the given Warden is currently out for, or null if the index is stale or the slot is no
     * longer held by that Warden (it burrowed, died, or the slot was refilled). Guards death and burrow
     * handling against a stale copy of a Warden (e.g. a duplicated entity) rewriting someone else's slot.
     */
    @Nullable
    public WardenSlot outSlotOf(int slotIndex, UUID wardenId) {
        if (slotIndex < 0 || slotIndex >= slots.size()) {
            return null;
        }
        WardenSlot slot = slots.get(slotIndex);
        return slot.getState() == SlotState.OUT && wardenId.equals(slot.getOutWardenId()) ? slot : null;
    }

    /** The city is defeated when every slot is dead. Every city always has at least Warden_1. */
    public boolean isDefeated() {
        for (WardenSlot slot : slots) {
            if (slot.getState().isAlive()) {
                return false;
            }
        }
        return true;
    }

    /** Count of slots holding an alive Warden (burrowed or out). */
    public int aliveCount() {
        int count = 0;
        for (WardenSlot slot : slots) {
            if (slot.getState().isAlive()) {
                count++;
            }
        }
        return count;
    }

    /** Index of the lowest-numbered burrowed slot, or -1 if none is burrowed. */
    public int lowestBurrowedSlotIndex() {
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).getState() == SlotState.BURROWED) {
                return i;
            }
        }
        return -1;
    }

    /** Index of the lowest-numbered dead slot, or -1 if none is dead. */
    public int lowestDeadSlotIndex() {
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).getState() == SlotState.DEAD) {
                return i;
            }
        }
        return -1;
    }

    /** Whether the given block position lies inside the city bounds. */
    public boolean containsPosition(BlockPos pos) {
        return bounds.isInside(pos);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("id", id);
        int[] box = {bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ()};
        tag.putIntArray("bounds", box);
        tag.putLongArray("shriekers", naturalShriekers.toLongArray());
        ListTag slotList = new ListTag();
        for (WardenSlot slot : slots) {
            slotList.add(slot.save());
        }
        tag.put("slots", slotList);
        return tag;
    }

    public static TrackedCity load(CompoundTag tag) {
        long id = tag.getLong("id");
        int[] b = tag.getIntArray("bounds");
        BoundingBox bounds = new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5]);
        LongList shriekers = new LongArrayList(tag.getLongArray("shriekers"));
        List<WardenSlot> slots = new ArrayList<>();
        ListTag slotList = tag.getList("slots", Tag.TAG_COMPOUND);
        for (int i = 0; i < slotList.size(); i++) {
            slots.add(WardenSlot.load(slotList.getCompound(i)));
        }
        if (slots.isEmpty()) {
            // Defensive: a city must always have at least Warden_1.
            slots.add(new WardenSlot(SlotState.DEAD, 0.0F, -1L, null));
        }
        return new TrackedCity(id, bounds, shriekers, slots);
    }
}
