package com.uiforsquid.wardenofnothing.heart;

import com.uiforsquid.wardenofnothing.city.CityTracker;
import com.uiforsquid.wardenofnothing.data.TrackedCity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** F5 ritual: an Alive heart on a sculk block inside a Warden-free tracked city, hit by a pulse. */
public final class HeartRitual {

    private HeartRitual() {
    }

    /**
     * If the Alive heart at {@code heartPos} sits on a {@code minecraft:sculk} block inside a tracked
     * city that has no Warden (defeated), consume it into a mound and return true. Otherwise the heart
     * stays Alive and nothing happens.
     */
    public static boolean tryPerform(ServerLevel level, BlockPos heartPos) {
        if (!level.getBlockState(heartPos.below()).is(Blocks.SCULK)) {
            return false;
        }
        TrackedCity city = CityTracker.findCity(level, heartPos);
        if (city == null || !city.isDefeated()) {
            return false;
        }
        HeartMound.build(level, heartPos, city);
        return true;
    }
}
