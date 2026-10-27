package com.uiforsquid.wardenofnothing.mixin;

import com.uiforsquid.wardenofnothing.config.Config;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * F9: Wardens can't travel through portals, exactly as the Wither and Ender Dragon override
 * {@code canUsePortal()} to return false. (The Warden already can't ride vehicles or be leashed in
 * vanilla, matching the other two bosses, so only this needs adding.) canUsePortal is inherited from
 * Entity and not overridden by Warden, so the guard goes here, limited to Wardens.
 */
@Mixin(Entity.class)
public abstract class EntityPortalMixin {

    @Inject(method = "canUsePortal", at = @At("HEAD"), cancellable = true)
    private void wardenofnothing$wardenNoPortal(boolean allowPassengers, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Warden && Config.SPEC.isLoaded() && Config.WARDENS_UNMOVABLE.get()) {
            cir.setReturnValue(false);
        }
    }
}
