package com.uiforsquid.wardenofnothing.city;

import com.uiforsquid.wardenofnothing.data.CityData;
import com.uiforsquid.wardenofnothing.data.TrackedCity;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Switches natural shriekers between {@code can_summon=true} and {@code false} to match whether their
 * city is defeated (F2). Shriekers in unloaded chunks are switched when their chunk next loads. A
 * recorded position found to no longer hold a shrieker is dropped (belt-and-suspenders with the
 * removal mixin).
 */
public final class CityShriekers {

    private CityShriekers() {
    }

    /** Switches every loaded natural shrieker of a city to match its current defeated state. */
    public static void refreshLoaded(ServerLevel level, TrackedCity city) {
        CityData data = CityTracker.get(level);
        boolean canSummon = !city.isDefeated();
        // A snapshot: dropping a missing shrieker changes the city's list.
        for (long packed : city.getNaturalShriekers().toLongArray()) {
            BlockPos pos = BlockPos.of(packed);
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.SCULK_SHRIEKER)) {
                data.removeNaturalShrieker(packed);
            } else if (state.getValue(SculkShriekerBlock.CAN_SUMMON) != canSummon) {
                level.setBlock(pos, state.setValue(SculkShriekerBlock.CAN_SUMMON, canSummon), 3);
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
        CityData data = CityTracker.get(level);
        LongList shriekers = data.naturalShriekersIn(chunk.getPos());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < shriekers.size(); i++) {
            long packed = shriekers.getLong(i);
            BlockState state = chunk.getBlockState(pos.set(packed));
            TrackedCity city = data.ownerOf(packed);
            if (!state.is(Blocks.SCULK_SHRIEKER)) {
                data.removeNaturalShrieker(packed);
            } else if (city != null && state.getValue(SculkShriekerBlock.CAN_SUMMON) == city.isDefeated()) {
                chunk.setBlockState(pos.immutable(), state.setValue(SculkShriekerBlock.CAN_SUMMON, !city.isDefeated()), false);
            }
        }
    }
}
