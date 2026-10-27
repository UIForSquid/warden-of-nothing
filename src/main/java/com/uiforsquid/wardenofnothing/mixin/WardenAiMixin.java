package com.uiforsquid.wardenofnothing.mixin;

import com.uiforsquid.wardenofnothing.config.Config;
import net.minecraft.world.entity.monster.warden.WardenAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Warden brain constants made configurable:
 * <ul>
 *   <li>F3: the dig cooldown that every disturbance refreshes (vanilla 1200 ticks = 60s) becomes
 *       {@code burrowDelaySeconds}. This is the timer that decides how long a Warden waits, undisturbed,
 *       before it burrows. Vanilla sets it in two places: {@code setDigCooldown} and the
 *       dig-cooldown-setter behaviour ({@code lambda$static$0}) that runs while it has a target.
 *   <li>{@code meleeCooldown}, {@code emergeTime} and {@code digTime}, which vanilla bakes into the
 *       brain as it is built (so they apply to Wardens created after a change).
 * </ul>
 * The brain is also built client-side, so the brain-building reads fall back to vanilla while the
 * server config isn't loaded.
 */
@Mixin(WardenAi.class)
public abstract class WardenAiMixin {

    /**
     * The setter lambda is matched by its signature, not its compiler-assigned name ({@code lambda$static$0}),
     * which differs between the dev and production Minecraft jars (production keeps the obfuscated name). It is the only method with this
     * signature in {@code WardenAi}.
     */
    @ModifyConstant(method = {
            "setDigCooldown",
            "*(Lnet/minecraft/world/entity/ai/behavior/declarative/BehaviorBuilder$Instance;Lnet/minecraft/world/entity/ai/behavior/declarative/MemoryAccessor;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/monster/warden/Warden;J)Z"},
            constant = @Constant(longValue = 1200L),
            require = 2) // both places, so a signature that stops matching fails loudly instead of silently
    private static long wardenofnothing$burrowDelay(long original) {
        return Config.BURROW_DELAY_SECONDS.get() * 20L;
    }

    @ModifyConstant(method = "initFightActivity", constant = @Constant(intValue = 18))
    private static int wardenofnothing$meleeCooldown(int original) {
        return Config.intOrVanilla(Config.MELEE_COOLDOWN, original);
    }

    @Redirect(method = "initEmergeActivity",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/monster/warden/WardenAi;EMERGE_DURATION:I"))
    private static int wardenofnothing$emergeTime() {
        return Config.intOrVanilla(Config.EMERGE_TIME, WardenAi.EMERGE_DURATION);
    }

    @Redirect(method = "initDiggingActivity",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/monster/warden/WardenAi;DIGGING_DURATION:I"))
    private static int wardenofnothing$digTime() {
        return Config.intOrVanilla(Config.DIG_TIME, 100);
    }
}
