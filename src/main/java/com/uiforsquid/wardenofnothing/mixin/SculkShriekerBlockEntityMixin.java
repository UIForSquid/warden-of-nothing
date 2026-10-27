package com.uiforsquid.wardenofnothing.mixin;

import com.uiforsquid.wardenofnothing.city.CityTracker;
import com.uiforsquid.wardenofnothing.data.TrackedCity;
import com.uiforsquid.wardenofnothing.warden.WardenSummoner;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * F2: for a shrieker inside a tracked city, replace the vanilla "spawn a fresh Warden" with the
 * slot-based summon (lowest-numbered burrowed Warden, remembered health plus healing). Shriekers
 * outside any tracked city are left entirely to vanilla. The surrounding shriek behaviour (warning
 * levels, Darkness, the spawn search near the shrieker) is untouched: this only changes which Warden
 * appears, and whether one appears.
 *
 * <p>Routing is by the city's bounds rather than by the per-shrieker record: a city only records the
 * shriekers in chunks that generated while tracking, so a city explored piecemeal (or across installs)
 * can have un-recorded shriekers that would otherwise fall back to vanilla and break the per-city
 * limit, the defeat/shut-off, and the heart drop. Only ancient-city (can_summon) shriekers ever reach
 * this method, so routing by bounds is faithful to "the city's shriekers summon the city's Wardens".
 *
 * <p>A shrieker whose city is defeated (its Warden dead) goes fully silent: {@code tryShriek} is
 * cancelled, so it emits no shriek sound, particles, Darkness or warning escalation. Switching the
 * shrieker to {@code can_summon=false} alone is not enough, because vanilla {@code tryShriek} still
 * shrieks (just without raising the warning level) when it can't respond.
 *
 * <p>{@code trySummonWarden} is private with no event hook, so a mixin is used.
 */
@Mixin(SculkShriekerBlockEntity.class)
public abstract class SculkShriekerBlockEntityMixin {

    @Shadow
    private int warningLevel;

    @Inject(method = "tryShriek", at = @At("HEAD"), cancellable = true)
    private void wardenofnothing$silentWhenDefeated(ServerLevel level, @Nullable ServerPlayer player, CallbackInfo ci) {
        BlockPos pos = ((SculkShriekerBlockEntity) (Object) this).getBlockPos();
        TrackedCity city = CityTracker.findCity(level, pos);
        if (city != null && city.isDefeated()) {
            ci.cancel();
        }
    }

    /**
     * The moment a city shrieker actually shrieks at a player, alert the city's out Wardens to the
     * player (anger + attack target), so they charge toward where the shriek came from.
     */
    @Inject(method = "tryShriek",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/SculkShriekerBlockEntity;shriek(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)V"))
    private void wardenofnothing$alertCityWardens(ServerLevel level, ServerPlayer player, CallbackInfo ci) {
        if (player != null) {
            BlockPos pos = ((SculkShriekerBlockEntity) (Object) this).getBlockPos();
            WardenSummoner.alertOutWardens(level, pos, player);
        }
    }

    @Inject(method = "trySummonWarden", at = @At("HEAD"), cancellable = true)
    private void wardenofnothing$slotSummon(ServerLevel level, CallbackInfoReturnable<Boolean> cir) {
        BlockPos pos = ((SculkShriekerBlockEntity) (Object) this).getBlockPos();
        if (CityTracker.findCity(level, pos) == null) {
            return; // not inside a tracked city: let vanilla summon a normal Warden
        }
        // Preserve vanilla's warning-level-4 gate before summoning.
        if (this.warningLevel < 4) {
            cir.setReturnValue(false);
            return;
        }
        cir.setReturnValue(WardenSummoner.summonFromSlot(level, pos));
    }
}
