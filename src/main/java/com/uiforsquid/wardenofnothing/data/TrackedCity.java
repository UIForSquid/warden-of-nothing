package com.uiforsquid.wardenofnothing.data;

import com.uiforsquid.wardenofnothing.warden.WardenHealth;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongLists;
import java.util.ArrayList;
import java.util.Collections;
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
 *
 * <p>The city owns every slot transition (summon, burrow, death, claim), so the slot rules live here
 * and nowhere else: a slot is held by at most one OUT Warden, summons take the lowest burrowed slot,
 * refills take the lowest dead slot, and the city is defeated exactly when every slot is dead. Each
 * transition marks the saved data dirty and reports what changed; callers only apply the in-world
 * effects (switching shriekers, dropping the Heart, marking the entity).
 */
public final class TrackedCity {

    /** What a City Warden's death did to its city. */
    public enum Death {
        /** Not this city's current Warden for that slot (stale copy, or already handled): nothing changed. */
        IGNORED,
        /** The slot is now dead; the city still has a living Warden. */
        KILLED,
        /** The slot is now dead and it was the last living one: the city is defeated. */
        DEFEATED
    }

    /** Where a claimed Warden went, and whether that brought a defeated city back. */
    public record Claim(int slotIndex, boolean revivedCity) {
    }

    /** Stable id: the packed long of the structure start's origin chunk. */
    private final long id;

    /** The vanilla structure bounding box (whole city). "The edge of the city" is the edge of this. */
    private final BoundingBox bounds;

    /** Positions of shriekers the structure placed, that have not since been removed. Maintained by {@link CityData}. */
    private final LongList naturalShriekers;

    /** Ordered Warden slots: index 0 = Warden_1, index 1 = Warden_2, ... */
    private final List<WardenSlot> slots;

    /** Marks the owning saved data dirty. */
    private final Runnable onChange;

    private TrackedCity(long id, BoundingBox bounds, LongList naturalShriekers, List<WardenSlot> slots, Runnable onChange) {
        this.id = id;
        this.bounds = bounds;
        this.naturalShriekers = naturalShriekers;
        this.slots = slots;
        this.onChange = onChange;
    }

    /** A freshly recorded city: no shriekers yet (filled in as chunks generate) and Warden_1 burrowed. */
    static TrackedCity fresh(long id, BoundingBox bounds, float maxHealth, Runnable onChange) {
        List<WardenSlot> slots = new ArrayList<>();
        slots.add(WardenSlot.newBurrowed(maxHealth));
        return new TrackedCity(id, bounds, new LongArrayList(), slots, onChange);
    }

    public long getId() {
        return id;
    }

    public BoundingBox getBounds() {
        return bounds;
    }

    /** Whether the given block position lies inside the city bounds. */
    public boolean containsPosition(BlockPos pos) {
        return bounds.isInside(pos);
    }

    /** Read-only view of the recorded natural shriekers; record and remove them through {@link CityData}. */
    public LongList getNaturalShriekers() {
        return LongLists.unmodifiable(naturalShriekers);
    }

    void addNaturalShrieker(long packed) {
        naturalShriekers.add(packed);
    }

    boolean removeNaturalShrieker(long packed) {
        return naturalShriekers.rem(packed);
    }

    /** Read-only view of the slots; change them through the transitions below. */
    public List<WardenSlot> getSlots() {
        return Collections.unmodifiableList(slots);
    }

    /** The city is defeated when every slot is dead. Every city always has at least Warden_1. */
    public boolean isDefeated() {
        return aliveCount() == 0;
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

    /** The entity ids of the city's Wardens currently out in the world, lowest slot first. */
    public List<UUID> outWardenIds() {
        List<UUID> ids = new ArrayList<>();
        for (WardenSlot slot : slots) {
            if (slot.getState() == SlotState.OUT && slot.getOutWardenId() != null) {
                ids.add(slot.getOutWardenId());
            }
        }
        return ids;
    }

    /** Whether another Warden may join: fewer than {@code maxAlive} slots are alive. */
    public boolean hasRoomFor(int maxAlive) {
        return aliveCount() < maxAlive;
    }

    // ----- slot transitions -----

    /** The slot a shrieker would summon (the lowest-numbered burrowed one), or -1 if none is burrowed. */
    public int summonableSlot() {
        return lowestSlotIn(SlotState.BURROWED);
    }

    /** The health the Warden in a burrowed slot comes back at: remembered health plus underground healing. */
    public float summonHealth(int slotIndex, float maxHealth, long now) {
        WardenSlot slot = slots.get(slotIndex);
        return WardenHealth.computeSummonHealth(slot.getRememberedHealth(), slot.getBurrowedAtGameTime(), maxHealth, now);
    }

    /** A burrowed slot's Warden is out in the world as {@code wardenId}. */
    public void sendOut(int slotIndex, UUID wardenId) {
        WardenSlot slot = slots.get(slotIndex);
        if (slot.getState() != SlotState.BURROWED) {
            throw new IllegalStateException("slot " + slotIndex + " of city " + id + " is " + slot.getState() + ", not BURROWED");
        }
        slot.setState(SlotState.OUT);
        slot.setOutWardenId(wardenId);
        slot.setBurrowedAtGameTime(-1L);
        onChange.run();
    }

    /**
     * The Warden out for this slot burrowed with {@code health} at game time {@code now}. A no-op (false)
     * if that Warden doesn't hold the slot any more (already burrowed, dead, refilled, or a stale copy).
     */
    public boolean burrow(int slotIndex, UUID wardenId, float health, long now) {
        WardenSlot slot = outSlotOf(slotIndex, wardenId);
        if (slot == null) {
            return false;
        }
        slot.setRememberedHealth(health);
        slot.setBurrowedAtGameTime(now);
        slot.setState(SlotState.BURROWED);
        slot.setOutWardenId(null);
        onChange.run();
        return true;
    }

    /** The Warden out for this slot died. */
    public Death kill(int slotIndex, UUID wardenId) {
        WardenSlot slot = outSlotOf(slotIndex, wardenId);
        if (slot == null) {
            return Death.IGNORED;
        }
        slot.setState(SlotState.DEAD);
        slot.setOutWardenId(null);
        onChange.run();
        return isDefeated() ? Death.DEFEATED : Death.KILLED;
    }

    /**
     * A Warden joins the city with {@code health}: it fills the lowest dead slot, or a new slot after the
     * last. With an id it is OUT in the world as that Warden (assignment, revival); without one it is
     * BURROWED as of {@code now} (a Warden claimed as it dug down). The caller checks the alive limit.
     */
    public Claim claim(@Nullable UUID outWardenId, float health, long now) {
        boolean wasDefeated = isDefeated();
        int index = lowestSlotIn(SlotState.DEAD);
        WardenSlot slot;
        if (index >= 0) {
            slot = slots.get(index);
        } else {
            slot = WardenSlot.newBurrowed(health);
            slots.add(slot);
            index = slots.size() - 1;
        }
        slot.setRememberedHealth(health);
        if (outWardenId != null) {
            slot.setState(SlotState.OUT);
            slot.setOutWardenId(outWardenId);
            slot.setBurrowedAtGameTime(-1L);
        } else {
            slot.setState(SlotState.BURROWED);
            slot.setOutWardenId(null);
            slot.setBurrowedAtGameTime(now);
        }
        onChange.run();
        return new Claim(index, wasDefeated);
    }

    /** The slot the given Warden currently holds as OUT, or null (stale index, or no longer that Warden's). */
    @Nullable
    private WardenSlot outSlotOf(int slotIndex, UUID wardenId) {
        if (slotIndex < 0 || slotIndex >= slots.size()) {
            return null;
        }
        WardenSlot slot = slots.get(slotIndex);
        return slot.getState() == SlotState.OUT && wardenId.equals(slot.getOutWardenId()) ? slot : null;
    }

    private int lowestSlotIn(SlotState state) {
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).getState() == state) {
                return i;
            }
        }
        return -1;
    }

    // ----- persistence -----

    CompoundTag save() {
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

    static TrackedCity load(CompoundTag tag, Runnable onChange) {
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
        return new TrackedCity(id, bounds, shriekers, slots, onChange);
    }
}
