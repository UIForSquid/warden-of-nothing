package com.uiforsquid.wardenofnothing.mixin;

import com.uiforsquid.wardenofnothing.config.Config;
import com.uiforsquid.wardenofnothing.warden.BossWarden;
import com.uiforsquid.wardenofnothing.warden.BurrowingWarden;
import com.uiforsquid.wardenofnothing.warden.WardenRuntime;
import java.util.Collections;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.AngerManagement;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Warden-side changes:
 * <ul>
 *   <li>F1: melee damage is rescaled for difficulty just before a boss hits.
 *   <li>F3: the undisturbed-before-burrow delay (vanilla 1200 ticks in finalizeSpawn) becomes
 *       {@code burrowDelaySeconds}.
 *   <li>F3: {@link BurrowingWarden#wardenofnothing$forceBurrow()} is implemented here so the mod can
 *       start a forced burrow using the Warden's own private brain/anger state. The burrow is held
 *       (re-applied each tick) until the dig animation starts, so a vibration that refreshes the dig
 *       cooldown in the meantime can't cancel it.
 *   <li>{@code emergeTime}: the emerge memory set in finalizeSpawn (the brain's emerge behaviour is
 *       changed in {@link WardenAiMixin}).
 *   <li>{@code angerPerVibration} (vibrations and touch both use {@code increaseAngerAt(Entity)}) and
 *       {@code angerPerHit}.
 * </ul>
 */
@Mixin(Warden.class)
public abstract class WardenMixin implements BurrowingWarden {

    @Shadow
    AngerManagement angerManagement;

    @Shadow
    public abstract boolean canTargetEntity(Entity entity);

    @Shadow
    protected abstract void syncClientAngerLevel();

    /** The mod's runtime state for this Warden; see {@link WardenRuntime}. Created on first use, never saved. */
    @Unique
    private WardenRuntime wardenofnothing$runtime;

    @Override
    public WardenRuntime wardenofnothing$runtime() {
        if (this.wardenofnothing$runtime == null) {
            this.wardenofnothing$runtime = WardenRuntime.create();
        }
        return this.wardenofnothing$runtime;
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"))
    private void wardenofnothing$scaleMelee(Entity target, CallbackInfoReturnable<Boolean> cir) {
        Warden self = (Warden) (Object) this;
        if (!(self.level() instanceof ServerLevel level) || !BossWarden.isBoss(self)) {
            return;
        }
        AttributeInstance instance = self.getAttribute(Attributes.ATTACK_DAMAGE);
        if (instance != null) {
            instance.setBaseValue(BossWarden.scaledMeleeDamage(level.getDifficulty()));
        }
    }

    /**
     * A player who is hit by a boss Warden's melee and survives is afflicted with brief nausea and
     * blindness (defaults 1s / 0.5s). Injected at RETURN so the hit (and its damage) has already
     * resolved: {@code isDeadOrDying()} then tells a survivor from a kill, and the boolean return tells a
     * landed hit from a missed one.
     */
    @Inject(method = "doHurtTarget", at = @At("RETURN"))
    private void wardenofnothing$afflictSurvivor(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return; // the hit didn't land
        }
        Warden self = (Warden) (Object) this;
        if (!(self.level() instanceof ServerLevel) || !BossWarden.isBoss(self)
                || !Config.WARDEN_HIT_AFFLICTS.get()) {
            return;
        }
        if (!(target instanceof Player player) || player.isDeadOrDying()) {
            return;
        }
        int nausea = Config.WARDEN_HIT_NAUSEA_TICKS.get();
        if (nausea > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, nausea, 0));
        }
        int blindness = Config.WARDEN_HIT_BLINDNESS_TICKS.get();
        if (blindness > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, blindness, 0));
        }
    }

    @ModifyConstant(method = "finalizeSpawn", constant = @Constant(longValue = 1200L))
    private long wardenofnothing$burrowDelay(long original) {
        return Config.BURROW_DELAY_SECONDS.get() * 20L;
    }

    @Redirect(method = "finalizeSpawn",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/monster/warden/WardenAi;EMERGE_DURATION:I"))
    private int wardenofnothing$emergeTime() {
        return Config.intOrVanilla(Config.EMERGE_TIME, WardenAi.EMERGE_DURATION);
    }

    @ModifyConstant(method = "increaseAngerAt(Lnet/minecraft/world/entity/Entity;)V", constant = @Constant(intValue = 35))
    private int wardenofnothing$angerPerVibration(int original) {
        return Config.intOrVanilla(Config.ANGER_PER_VIBRATION, original);
    }

    @ModifyArg(method = "hurt",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/monster/warden/Warden;increaseAngerAt(Lnet/minecraft/world/entity/Entity;IZ)V"),
            index = 1)
    private int wardenofnothing$angerPerHit(int original) {
        return Config.intOrVanilla(Config.ANGER_PER_HIT, original);
    }

    @Override
    public void wardenofnothing$forceBurrow() {
        Warden self = (Warden) (Object) this;
        Brain<Warden> brain = self.getBrain();
        brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        brain.eraseMemory(MemoryModuleType.ROAR_TARGET);
        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        brain.eraseMemory(MemoryModuleType.DIG_COOLDOWN);
        // Drop all anger so the brain runs DIG instead of ROAR/FIGHT on its next tick.
        this.angerManagement = new AngerManagement(self::canTargetEntity, Collections.emptyList());
        this.syncClientAngerLevel();
        this.wardenofnothing$runtime().setForcingBurrow(true);
    }

    @Override
    public void wardenofnothing$tickForcedBurrow() {
        WardenRuntime runtime = this.wardenofnothing$runtime();
        if (!runtime.isForcingBurrow()) {
            return;
        }
        Warden self = (Warden) (Object) this;
        if (self.hasPose(Pose.DIGGING) || self.isRemoved()) {
            runtime.setForcingBurrow(false);
            return;
        }
        // Undo anything that would keep the brain out of DIG (a vibration refreshing the dig cooldown,
        // a new roar target), without rebuilding the anger state every tick.
        Brain<Warden> brain = self.getBrain();
        brain.eraseMemory(MemoryModuleType.DIG_COOLDOWN);
        brain.eraseMemory(MemoryModuleType.ROAR_TARGET);
        brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        if (self.getEntityAngryAt().isPresent()) {
            this.angerManagement = new AngerManagement(self::canTargetEntity, Collections.emptyList());
            this.syncClientAngerLevel();
        }
    }

    @Override
    public void wardenofnothing$clearAllAnger() {
        Warden self = (Warden) (Object) this;
        Brain<Warden> brain = self.getBrain();
        brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
        brain.eraseMemory(MemoryModuleType.ROAR_TARGET);
        this.angerManagement = new AngerManagement(self::canTargetEntity, Collections.emptyList());
        this.syncClientAngerLevel();
    }
}
