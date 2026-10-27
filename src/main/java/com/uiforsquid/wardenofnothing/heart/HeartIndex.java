package com.uiforsquid.wardenofnothing.heart;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * A per-dimension index of placed Heart positions, so the sculk-pulse mixin can find a nearby Heart
 * cheaply. Maintained by the Heart block entity's ticker (adds itself) and removal (removes itself),
 * both on the main server thread.
 */
public final class HeartIndex {

    private static final Map<ResourceKey<Level>, Set<BlockPos>> HEARTS = new ConcurrentHashMap<>();

    private HeartIndex() {
    }

    public static void add(Level level, BlockPos pos) {
        HEARTS.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos.immutable());
    }

    public static void remove(Level level, BlockPos pos) {
        Set<BlockPos> set = HEARTS.get(level.dimension());
        if (set != null) {
            set.remove(pos);
        }
    }

    /** Drops a dimension's entries when it unloads (or the server stops), so no stale Heart survives into another world. */
    public static void forget(Level level) {
        HEARTS.remove(level.dimension());
    }

    /** The placed Heart nearest to {@code from} within {@code range} blocks, or null if none. */
    @Nullable
    public static BlockPos nearest(Level level, BlockPos from, double range) {
        Set<BlockPos> set = HEARTS.get(level.dimension());
        if (set == null || set.isEmpty()) {
            return null;
        }
        double rangeSq = range * range;
        BlockPos best = null;
        double bestSq = Double.MAX_VALUE;
        for (BlockPos pos : set) {
            double d = from.distSqr(pos);
            if (d <= rangeSq && d < bestSq) {
                bestSq = d;
                best = pos;
            }
        }
        return best;
    }
}
