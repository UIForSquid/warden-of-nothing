package com.uiforsquid.wardenofnothing.mixin;

import com.uiforsquid.wardenofnothing.config.Config;
import net.minecraft.world.entity.ai.sensing.WardenEntitySensor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** {@code sniffRange}: the Warden's entity-detection (sniff) sensor radius, vanilla 24 on every axis. */
@Mixin(WardenEntitySensor.class)
public abstract class WardenEntitySensorMixin {

    @ModifyConstant(method = {"radiusXZ", "radiusY"}, constant = @Constant(intValue = 24))
    private int wardenofnothing$sniffRange(int original) {
        return Config.intOrVanilla(Config.SNIFF_RANGE, original);
    }
}
