package com.uiforsquid.wardenofnothing.home;

import com.uiforsquid.wardenofnothing.config.Config;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;

/** F8 geometry: the home range box, "outside by" distance, and the nearest point inside the city. */
public final class HomeRange {

    private HomeRange() {
    }

    /**
     * The home range: the city bounds grown by {@code homeRange} on the four sides, {@code homeRangeUp}
     * above the top, and with no limit below (down to the bottom of the world).
     */
    public static BoundingBox homeRangeBox(Level level, BoundingBox cityBounds) {
        int h = Config.HOME_RANGE.get();
        int up = Config.HOME_RANGE_UP.get();
        return new BoundingBox(
                cityBounds.minX() - h,
                level.getMinBuildHeight(),
                cityBounds.minZ() - h,
                cityBounds.maxX() + h,
                cityBounds.maxY() + up,
                cityBounds.maxZ() + h);
    }

    public static boolean isInside(BoundingBox box, Vec3 pos) {
        return pos.x >= box.minX() && pos.x <= box.maxX() + 1
                && pos.y >= box.minY() && pos.y <= box.maxY() + 1
                && pos.z >= box.minZ() && pos.z <= box.maxZ() + 1;
    }

    /** Shortest distance from a point to the box (0 if inside), counting all three axes. */
    public static double distanceOutside(BoundingBox box, Vec3 pos) {
        double dx = axisOverflow(pos.x, box.minX(), box.maxX() + 1);
        double dy = axisOverflow(pos.y, box.minY(), box.maxY() + 1);
        double dz = axisOverflow(pos.z, box.minZ(), box.maxZ() + 1);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double axisOverflow(double value, double min, double max) {
        if (value < min) {
            return min - value;
        }
        if (value > max) {
            return value - max;
        }
        return 0.0;
    }

    /** The nearest point inside the city bounds to the given position (used as the return target). */
    public static Vec3 nearestInside(BoundingBox cityBounds, Vec3 pos) {
        double x = Mth.clamp(pos.x, cityBounds.minX() + 0.5, cityBounds.maxX() + 0.5);
        double y = Mth.clamp(pos.y, cityBounds.minY(), cityBounds.maxY());
        double z = Mth.clamp(pos.z, cityBounds.minZ() + 0.5, cityBounds.maxZ() + 0.5);
        return new Vec3(x, y, z);
    }
}
