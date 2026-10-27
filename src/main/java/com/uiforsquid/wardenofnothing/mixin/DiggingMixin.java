package com.uiforsquid.wardenofnothing.mixin;

import com.uiforsquid.wardenofnothing.warden.WardenBurrow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.ai.behavior.warden.Digging;

/**
 * F3: when a Warden finishes digging (the Digging behaviour is about to remove it), save a City
 * Warden's health and time into its slot before it disappears. Covers both the normal burrow and
 * forced burrows (which run the same dig animation). Vanilla's {@code stop} removes the Warden with no
 * hook, so a mixin is used.
 */
@Mixin(Digging.class)
public abstract class DiggingMixin {

    @Inject(
            method = "stop(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/monster/warden/Warden;J)V",
            at = @At("HEAD"))
    private void wardenofnothing$saveBurrow(ServerLevel level, Warden entity, long gameTime, CallbackInfo ci) {
        // stop() removes the Warden only when it isn't already removed; save the slot in that case.
        if (entity.getRemovalReason() == null) {
            WardenBurrow.onBurrowed(entity, level);
        }
    }
}
