package com.uiforsquid.wardenofnothing.mixin;

import com.uiforsquid.wardenofnothing.city.CityDataHolder;
import com.uiforsquid.wardenofnothing.data.CityData;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Gives each server level a slot for its {@link CityData}; see {@link CityDataHolder}. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin implements CityDataHolder {

    @Unique
    @Nullable
    private CityData wardenofnothing$cityData;

    @Override
    @Nullable
    public CityData wardenofnothing$cityData() {
        return this.wardenofnothing$cityData;
    }

    @Override
    public void wardenofnothing$setCityData(CityData data) {
        this.wardenofnothing$cityData = data;
    }
}
