package com.uiforsquid.wardenofnothing.mixin;

import com.uiforsquid.wardenofnothing.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * {@code vibrationListenRange} (vanilla 16) and {@code angerPerProjectile} (vanilla 10), both hardcoded
 * in the Warden's vibration listener. The listen range is read when the listener (re)registers, so it
 * applies fully to Wardens that spawn, load or move after a change.
 */
@Mixin(targets = "net.minecraft.world.entity.monster.warden.Warden$VibrationUser")
public abstract class WardenVibrationUserMixin {

    @ModifyConstant(method = "getListenerRadius", constant = @Constant(intValue = 16))
    private int wardenofnothing$listenRange(int original) {
        return Config.intOrVanilla(Config.VIBRATION_LISTEN_RANGE, original);
    }

    @ModifyConstant(method = "onReceiveVibration", constant = @Constant(intValue = 10))
    private int wardenofnothing$angerPerProjectile(int original) {
        return Config.intOrVanilla(Config.ANGER_PER_PROJECTILE, original);
    }
}
