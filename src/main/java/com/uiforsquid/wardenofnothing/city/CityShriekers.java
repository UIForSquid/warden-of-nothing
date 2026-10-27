package com.uiforsquid.wardenofnothing.city;

import com.uiforsquid.wardenofnothing.data.TrackedCity;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Switches natural shriekers between {@code can_summon=true} and {@code false} to match whether their
 * city is defeated (F2). Shriekers in unloaded chunks are switched when their chunk next loads.
 */
public final class CityShriekers {

    private CityShriekers() {
    }

    /** Switches every loaded natural shrieker of a city to match its current defeated state. */
    public static void refreshLoaded(ServerLevel level, TrackedCity city) {
        boolean canSummon = !city.isDefeated();
        LongList shriekers = city.getNaturalShriekers();
        // Backwards, so apply() dropping the current entry from the list doesn't skip the next one.
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = shriekers.size() - 1; i >= 0; i--) {
            long packed = shriekers.getLong(i);
            pos.set(packed);
            if (level.isLoaded(pos)) {
                apply(level, pos, packed, canSummon);
            }
        }
    }

    /**
     * When a chunk loads, bring its natural shriekers in line with their city's defeated state. This
     * runs while the chunk is still being promoted to full, so it edits the given chunk directly rather
     * than going through the level (which would ask the chunk cache for this same chunk). Players are
     * sent the chunk afterwards, so no block update is needed.
     */
    public static void onChunkLoaded(ServerLevel level, LevelChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxX = chunkPos.getMaxBlockX();
        int maxZ = chunkPos.getMaxBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (TrackedCity city : CityTracker.get(level).getCities()) {
            // Natural shriekers always lie inside their city's bounds: skip cities this chunk misses.
            if (!city.getBounds().intersects(minX, minZ, maxX, maxZ)) {
                continue;
            }
            boolean canSummon = !city.isDefeated();
            LongList shriekers = city.getNaturalShriekers();
            for (int i = shriekers.size() - 1; i >= 0; i--) {
                long packed = shriekers.getLong(i);
                int x = BlockPos.getX(packed);
                int z = BlockPos.getZ(packed);
                if (x >= minX && x <= maxX && z >= minZ && z <= maxZ) {
                    BlockState state = chunk.getBlockState(pos.set(packed));
                    if (!state.is(Blocks.SCULK_SHRIEKER)) {
                        CityTracker.onNaturalShriekerRemoved(level, packed);
                    } else if (state.getValue(SculkShriekerBlock.CAN_SUMMON) != canSummon) {
                        chunk.setBlockState(pos.immutable(), state.setValue(SculkShriekerBlock.CAN_SUMMON, canSummon), false);
                    }
                }
            }
        }
    }

    private static void apply(ServerLevel level, BlockPos pos, long packed, boolean canSummon) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.SCULK_SHRIEKER)) {
            // No longer a shrieker: drop it from the record (belt-and-suspenders with the removal mixin).
            CityTracker.onNaturalShriekerRemoved(level, packed);
            return;
        }
        if (state.getValue(SculkShriekerBlock.CAN_SUMMON) != canSummon) {
            // Immutable copy: the caller's cursor is mutable and block updates may keep the position.
            level.setBlock(pos.immutable(), state.setValue(SculkShriekerBlock.CAN_SUMMON, canSummon), 3);
        }
    }
}
