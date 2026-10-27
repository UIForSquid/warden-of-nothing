package com.uiforsquid.wardenofnothing.mixin;

import com.uiforsquid.wardenofnothing.config.Config;
import com.uiforsquid.wardenofnothing.heart.HeartBlockEntity;
import com.uiforsquid.wardenofnothing.heart.HeartIndex;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SculkBehaviour;
import net.minecraft.world.level.block.SculkSpreader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * F5 faithful pulse-steering: a sculk charge cursor (a "pulse") within {@code heartPullRange} of a
 * placed Heart steers toward it, and when it reaches the Heart it is absorbed (removed, spreading no
 * sculk) and counts as one trigger. Pulses still only travel across sculk-spreadable blocks, so a
 * Heart with no sculk path to it can't be reached. No event covers this, so it mixes into the private
 * vanilla charge-cursor internals.
 */
@Mixin(SculkSpreader.ChargeCursor.class)
public abstract class SculkChargeCursorMixin {

    @Shadow
    private BlockPos pos;

    @Shadow
    private int charge;

    @Shadow
    @Final
    private static ObjectArrayList<Vec3i> NON_CORNER_NEIGHBOURS;

    @Invoker("getValidMovementPos")
    private static BlockPos wardenofnothing$origValidMovementPos(LevelAccessor level, BlockPos pos, RandomSource random) {
        throw new AssertionError();
    }

    @Invoker("isMovementUnobstructed")
    private static boolean wardenofnothing$isMovementUnobstructed(LevelAccessor level, BlockPos from, BlockPos to) {
        throw new AssertionError();
    }

    /** Absorb the pulse when it reaches (touches) a nearby Heart. */
    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void wardenofnothing$absorbIntoHeart(
            LevelAccessor level, BlockPos origin, RandomSource random, SculkSpreader spreader, boolean convert, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel) || this.charge <= 0) {
            return;
        }
        BlockPos heart = HeartIndex.nearest(serverLevel, this.pos, Config.HEART_PULL_RANGE.get());
        if (heart != null && this.pos.distManhattan(heart) <= 1) {
            if (!(serverLevel.getBlockEntity(heart) instanceof HeartBlockEntity be)) {
                HeartIndex.remove(serverLevel, heart); // stale entry: no Heart there any more, so no absorbing
                return;
            }
            be.onPulseAbsorbed(serverLevel);
            this.charge = 0; // discharged this pass: updateCursors prunes it
            ci.cancel();
        }
    }

    /** Steer spreading toward a nearby Heart, when a sculk-spreadable step gets closer to it. */
    @Redirect(
            method = "update",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/SculkSpreader$ChargeCursor;getValidMovementPos(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos wardenofnothing$steerTowardHeart(LevelAccessor level, BlockPos cursorPos, RandomSource random) {
        if (level instanceof ServerLevel serverLevel) {
            BlockPos heart = HeartIndex.nearest(serverLevel, cursorPos, Config.HEART_PULL_RANGE.get());
            if (heart != null) {
                BlockPos biased = bestStepToward(level, cursorPos, heart);
                if (biased != null) {
                    return biased;
                }
            }
        }
        return wardenofnothing$origValidMovementPos(level, cursorPos, random);
    }

    /**
     * The adjacent sculk-spreadable block that gets strictly closer to the heart, nearest one wins. Uses
     * vanilla's own movement rule (a spreadable block, reachable without cutting through a solid edge).
     */
    private static BlockPos bestStepToward(LevelAccessor level, BlockPos from, BlockPos heart) {
        double currentSq = from.distSqr(heart);
        BlockPos best = null;
        double bestSq = currentSq;
        for (Vec3i offset : NON_CORNER_NEIGHBOURS) {
            BlockPos candidate = from.offset(offset);
            if (!(level.getBlockState(candidate).getBlock() instanceof SculkBehaviour)
                    || !wardenofnothing$isMovementUnobstructed(level, from, candidate)) {
                continue;
            }
            double d = candidate.distSqr(heart);
            if (d < bestSq) {
                bestSq = d;
                best = candidate;
            }
        }
        return best;
    }
}
