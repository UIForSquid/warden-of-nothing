package com.uiforsquid.wardenofnothing.mixin;

import com.uiforsquid.wardenofnothing.config.Config;
import net.minecraft.world.entity.monster.warden.AngerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code angerThreshold}: the anger at which a Warden counts as ANGRY (vanilla 80, an enum constant).
 * Every "is it angry" decision goes through {@code byAnger}, so it is replaced there; AGITATED keeps
 * its vanilla floor of 40 below the threshold. Falls back to vanilla while the config isn't loaded.
 */
@Mixin(AngerLevel.class)
public abstract class AngerLevelMixin {

    @Inject(method = "byAnger", at = @At("HEAD"), cancellable = true)
    private static void wardenofnothing$threshold(int anger, CallbackInfoReturnable<AngerLevel> cir) {
        if (!Config.SPEC.isLoaded()) {
            return;
        }
        if (anger >= Config.ANGER_THRESHOLD.get()) {
            cir.setReturnValue(AngerLevel.ANGRY);
        } else if (anger >= AngerLevel.AGITATED.getMinimumAnger()) {
            cir.setReturnValue(AngerLevel.AGITATED);
        } else {
            cir.setReturnValue(AngerLevel.CALM);
        }
    }
}
