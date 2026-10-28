package com.uiforsquid.wardenofnothing.mixin;

import com.uiforsquid.wardenofnothing.config.Config;
import com.uiforsquid.wardenofnothing.warden.BossWarden;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Weakens negative (harmful) potion effects applied to the boss Warden: the incoming effect is swapped
 * for a copy with its duration and amplifier scaled by config (defaults 0.5 each = half the time, half
 * the strength). Beneficial and neutral effects pass through unchanged.
 *
 * <p>Hooked on {@code addEffect(MobEffectInstance, Entity)} (the path every potion, splash, lingering
 * cloud and command effect goes through) by replacing the instance argument. An infinite-duration effect
 * (duration {@code -1}) keeps its duration, since halving it is meaningless; its amplifier is still
 * scaled. Amplifier is floored, so a level-I effect (amplifier 0) stays level I — it can't drop lower
 * without being removed entirely.
 */
@Mixin(LivingEntity.class)
public abstract class WardenPotionMixin {

    @ModifyVariable(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), argsOnly = true, index = 1)
    private MobEffectInstance wardenofnothing$weakenNegativeEffects(MobEffectInstance instance) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Warden warden) || !Config.SPEC.isLoaded()
                || !Config.WARDEN_WEAKEN_NEGATIVE_EFFECTS.get() || !BossWarden.isBoss(warden)) {
            return instance;
        }
        if (instance.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) {
            return instance;
        }
        int duration = instance.getDuration();
        if (duration > 0) {
            duration = Math.max(1, (int) Math.round(duration * Config.WARDEN_NEGATIVE_EFFECT_DURATION_MULTIPLIER.get()));
        }
        int amplifier = (int) Math.floor(instance.getAmplifier() * Config.WARDEN_NEGATIVE_EFFECT_AMPLIFIER_MULTIPLIER.get());
        if (amplifier < 0) {
            amplifier = 0;
        }
        return new MobEffectInstance(instance.getEffect(), duration, amplifier,
                instance.isAmbient(), instance.isVisible(), instance.showIcon());
    }
}
