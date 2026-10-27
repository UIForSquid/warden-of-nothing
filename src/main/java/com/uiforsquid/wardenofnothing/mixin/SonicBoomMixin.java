package com.uiforsquid.wardenofnothing.mixin;

import com.uiforsquid.wardenofnothing.config.Config;
import net.minecraft.world.entity.ai.behavior.warden.SonicBoom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;

/**
 * F1: makes the sonic boom's reach and between-booms cooldown configurable (defaults = vanilla 15/20
 * blocks, 40 ticks). The reach is checked both when the boom starts (checkExtraStartConditions) and
 * just before it fires (the tick range filter, lambda$tick$1), so both are changed to stay consistent.
 * Sonic boom damage and knockback are handled victim-side in BossWardenEvents, not here.
 *
 * <p>These constants are baked into vanilla with no event hook, so a mixin is used. They apply to all
 * Wardens; with the defaults that is no change from vanilla.
 */
@Mixin(SonicBoom.class)
public abstract class SonicBoomMixin {

    @ModifyConstant(
            method = "checkExtraStartConditions(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/monster/warden/Warden;)Z",
            constant = @Constant(doubleValue = 15.0))
    private double wardenofnothing$startHorizontal(double original) {
        return Config.SONIC_BOOM_RANGE_HORIZONTAL.get();
    }

    @ModifyConstant(
            method = "checkExtraStartConditions(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/monster/warden/Warden;)Z",
            constant = @Constant(doubleValue = 20.0))
    private double wardenofnothing$startVertical(double original) {
        return Config.SONIC_BOOM_RANGE_VERTICAL.get();
    }

    @ModifyConstant(method = "lambda$tick$1", constant = @Constant(doubleValue = 15.0))
    private static double wardenofnothing$tickHorizontal(double original) {
        return Config.SONIC_BOOM_RANGE_HORIZONTAL.get();
    }

    @ModifyConstant(method = "lambda$tick$1", constant = @Constant(doubleValue = 20.0))
    private static double wardenofnothing$tickVertical(double original) {
        return Config.SONIC_BOOM_RANGE_VERTICAL.get();
    }

    @ModifyConstant(method = "stop", constant = @Constant(intValue = 40))
    private int wardenofnothing$cooldown(int original) {
        return Config.SONIC_BOOM_COOLDOWN.get();
    }
}
