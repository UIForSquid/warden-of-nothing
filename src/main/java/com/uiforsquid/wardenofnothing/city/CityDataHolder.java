package com.uiforsquid.wardenofnothing.city;

import com.uiforsquid.wardenofnothing.data.CityData;
import javax.annotation.Nullable;

/**
 * Implemented on {@link net.minecraft.server.level.ServerLevel} by a mixin: each level keeps a direct
 * reference to its own {@link CityData}, so the per-block-change lookup is a field read and the
 * reference goes away with the level (no static per-dimension map to clear).
 */
public interface CityDataHolder {

    @Nullable
    CityData wardenofnothing$cityData();

    void wardenofnothing$setCityData(CityData data);
}
