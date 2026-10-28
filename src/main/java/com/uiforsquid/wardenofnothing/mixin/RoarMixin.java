package com.uiforsquid.wardenofnothing.mixin;

import com.uiforsquid.wardenofnothing.config.Config;
import com.uiforsquid.wardenofnothing.warden.BossWarden;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.warden.Roar;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Heals a boss Warden each time it roars. The roar behaviour ticks for the whole roar animation but
 * plays the roar sound exactly once, on the first tick where neither {@code ROAR_SOUND_DELAY} nor
 * {@code ROAR_SOUND_COOLDOWN} is set; vanilla sets the cooldown in that same tick. Injecting at the head
 * of {@code tick} and testing the same two memories (before vanilla sets the cooldown) fires the heal on
 * exactly that one tick — once per roar, in step with the audible yell.
 */
@Mixin(Roar.class)
public abstract class RoarMixin {

    @Inject(method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/monster/warden/Warden;J)V",
            at = @At("HEAD"))
    private void wardenofnothing$healOnRoar(ServerLevel level, Warden warden, long gameTime, CallbackInfo ci) {
        if (!Config.SPEC.isLoaded() || !BossWarden.isBoss(warden)) {
            return;
        }
        double heal = Config.WARDEN_ROAR_HEAL.get();
        if (heal <= 0.0) {
            return;
        }
        if (!warden.getBrain().hasMemoryValue(MemoryModuleType.ROAR_SOUND_DELAY)
                && !warden.getBrain().hasMemoryValue(MemoryModuleType.ROAR_SOUND_COOLDOWN)) {
            warden.heal((float) heal);
        }
    }
}
