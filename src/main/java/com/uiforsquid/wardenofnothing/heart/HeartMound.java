package com.uiforsquid.wardenofnothing.heart;

import com.uiforsquid.wardenofnothing.city.CityShriekers;
import com.uiforsquid.wardenofnothing.city.CityTracker;
import com.uiforsquid.wardenofnothing.data.CityData;
import com.uiforsquid.wardenofnothing.data.SlotState;
import com.uiforsquid.wardenofnothing.data.TrackedCity;
import com.uiforsquid.wardenofnothing.data.WardenSlot;
import com.uiforsquid.wardenofnothing.reg.WonBlocks;
import com.uiforsquid.wardenofnothing.warden.BossWarden;
import com.uiforsquid.wardenofnothing.warden.CityWardens;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** F5 heart mound: building the 5x5 pile, the 10-minute revival, the global roar, and the fade. */
public final class HeartMound {

    /** The 5x5 layer heights, indexed by Chebyshev ring: outer 3, middle corner 5, middle edge 6, centre 8. */
    public static int patternHeight(int dx, int dz) {
        int ax = Math.abs(dx);
        int az = Math.abs(dz);
        if (ax == 2 || az == 2) {
            return 3; // outer ring
        }
        if (ax == 0 && az == 0) {
            return 8; // centre
        }
        return (ax == 1 && az == 1) ? 5 : 6; // middle corners / edges
    }

    private HeartMound() {
    }

    /**
     * Consumes the Alive heart and builds the mound at the heart's level. Columns whose space is
     * already occupied by a non-replaceable block are skipped; nothing in the city is ever broken.
     * The centre column (the heart's spot) carries the controller block entity.
     */
    public static void build(ServerLevel level, BlockPos heartPos, TrackedCity city) {
        BlockState mound = WonBlocks.HEART_MOUND.get().defaultBlockState();
        LongArrayList placed = new LongArrayList();

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos column = heartPos.offset(dx, 0, dz);
                boolean centre = dx == 0 && dz == 0;
                BlockState existing = level.getBlockState(column);
                // The centre (the heart itself) is always replaced; other columns only if replaceable.
                // Sculk veins aren't replaceable but coat city surfaces, so overwrite any in the way
                // rather than leaving a gap in the mound.
                if (!centre && !existing.canBeReplaced() && !existing.is(Blocks.SCULK_VEIN)) {
                    continue;
                }
                int height = patternHeight(dx, dz);
                level.setBlock(column, mound.setValue(HeartMoundBlock.LAYERS, height), Block.UPDATE_ALL);
                // Put the pile on solid sculk: whatever sits directly under this column becomes sculk.
                level.setBlock(column.below(), Blocks.SCULK.defaultBlockState(), Block.UPDATE_ALL);
                placed.add(column.asLong());
            }
        }

        if (level.getBlockEntity(heartPos) instanceof HeartMoundBlockEntity controller) {
            controller.initController(city.getId(), level.getGameTime(), placed.toLongArray());
        }
    }

    /** The 10-minute revival: a brand-new Warden_1 climbs out, with a server-wide roar. */
    public static void revive(ServerLevel level, BlockPos centre, long cityId) {
        CityData data = CityTracker.get(level);
        TrackedCity city = data.getCity(cityId);
        if (city == null) {
            return;
        }
        Warden warden = EntityType.WARDEN.create(level);
        if (warden == null) {
            return;
        }
        // On top of the centre column (8 layers = a full block), not inside it, where it would suffocate.
        warden.moveTo(centre.getX() + 0.5, centre.getY() + 1.0, centre.getZ() + 0.5, 0.0F, 0.0F);
        warden.finalizeSpawn(level, level.getCurrentDifficultyAt(centre), MobSpawnType.TRIGGERED, null);
        level.addFreshEntity(warden);
        BossWarden.applyStats(warden);
        warden.setHealth(warden.getMaxHealth());

        WardenSlot slot = city.getSlots().isEmpty() ? null : city.getSlots().get(0);
        if (slot == null) {
            slot = WardenSlot.newBurrowed(warden.getMaxHealth());
            city.getSlots().add(slot);
        }
        slot.setState(SlotState.OUT);
        slot.setRememberedHealth(warden.getMaxHealth());
        slot.setBurrowedAtGameTime(-1L);
        slot.setOutWardenId(warden.getUUID());
        CityWardens.mark(warden, cityId, 0);
        data.setDirty();

        CityShriekers.refreshLoaded(level, city); // respawn routine: shriekers switch back on

        // Server-wide roar, for every player in any dimension, at full volume.
        MinecraftServer server = level.getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.playNotifySound(SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.0F, 1.0F);
        }
    }

    /** Removes every mound column and releases its forced chunks. */
    public static void removeAll(ServerLevel level, long[] columns) {
        for (long packed : columns) {
            BlockPos pos = BlockPos.of(packed);
            if (level.getBlockState(pos).is(WonBlocks.HEART_MOUND.get())) {
                level.removeBlock(pos, false);
            }
        }
        setForced(level, columns, false);
    }

    public static void setForced(ServerLevel level, long[] columns, boolean forced) {
        LongSet chunks = new LongOpenHashSet();
        for (long packed : columns) {
            BlockPos pos = BlockPos.of(packed);
            chunks.add(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        }
        for (long chunk : chunks) {
            ChunkPos cp = new ChunkPos(chunk);
            level.setChunkForced(cp.x, cp.z, forced);
        }
    }
}
