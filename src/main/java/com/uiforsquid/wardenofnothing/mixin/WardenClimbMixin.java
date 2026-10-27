package com.uiforsquid.wardenofnothing.mixin;

import com.uiforsquid.wardenofnothing.config.Config;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Spider-style wall climbing for the Warden. Vanilla {@code LivingEntity} already climbs straight up
 * whenever {@code onClimbable()} is true and it is pressed against a wall ({@code horizontalCollision}),
 * which is exactly how a Spider climbs: its {@code onClimbable()} returns the "climbing" flag it sets to
 * {@code horizontalCollision} each tick. So making a Warden's {@code onClimbable()} report true while it
 * is against a wall gives it the same climb. {@code onClimbable()} is declared on {@code LivingEntity},
 * so the guard is {@code instanceof Warden}.
 */
@Mixin(LivingEntity.class)
public abstract class WardenClimbMixin {

    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    private void wardenofnothing$wardenClimbsWalls(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Warden && Config.SPEC.isLoaded() && Config.WARDEN_CAN_CLIMB.get()
                && self.horizontalCollision) {
            cir.setReturnValue(true);
        }
    }
}
