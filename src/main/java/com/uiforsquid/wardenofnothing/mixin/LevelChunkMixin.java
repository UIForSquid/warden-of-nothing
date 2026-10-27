package com.uiforsquid.wardenofnothing.mixin;

import com.uiforsquid.wardenofnothing.city.CityTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Detects when a recorded natural shrieker is removed from its position (F2). As soon as the block at
 * a recorded position stops being a sculk shrieker &mdash; broken, blown up, replaced, or moved by any
 * mod &mdash; the position is dropped from the record for good. Switching only {@code can_summon}
 * leaves the block a sculk shrieker, so the mod's own switching never counts as removal.
 *
 * <p>No NeoForge event covers every way a block can change (mods, pistons, explosions, commands), so a
 * mixin on the single choke point is used. The hot path is guarded by a cheap position-set lookup.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {

    @Shadow
    public abstract Level getLevel();

    @Shadow
    public abstract BlockState getBlockState(BlockPos pos);

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void wardenofnothing$detectShriekerRemoval(
            BlockPos pos, BlockState state, boolean isMoving, CallbackInfoReturnable<BlockState> cir) {
        long packed = pos.asLong();
        Level level = this.getLevel();
        if (!CityTracker.isNaturalShrieker(level, packed)) {
            return;
        }
        BlockState old = this.getBlockState(pos);
        if (old.is(Blocks.SCULK_SHRIEKER) && !state.is(Blocks.SCULK_SHRIEKER)) {
            CityTracker.onNaturalShriekerRemoved((ServerLevel) level, packed);
        }
    }
}
