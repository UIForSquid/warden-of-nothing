package com.uiforsquid.wardenofnothing.heart;

import com.uiforsquid.wardenofnothing.config.Config;
import com.uiforsquid.wardenofnothing.reg.WonBlockEntities;
import com.uiforsquid.wardenofnothing.reg.WonBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Controller for a heart mound (F5). Only the centre column's block entity is a controller; it keeps
 * the mound's chunks loaded, revives the Warden after {@code heartReviveMinutes}, then fades every
 * column evenly over {@code moundFadeSeconds} and releases the chunks. Ring columns do nothing.
 */
public class HeartMoundBlockEntity extends BlockEntity {

    private boolean controller;
    private long cityId;
    private long startGameTime;
    private long[] columns = new long[0];
    private boolean fading;
    private long fadeStartTime;
    private boolean forcedApplied;

    /** Grows natural-looking sculk around the mound, like a vanilla catalyst's bloom (F5). */
    private final SculkSpreader spreader = SculkSpreader.createLevelSpreader();
    private boolean seeded;

    public HeartMoundBlockEntity(BlockPos pos, BlockState state) {
        super(WonBlockEntities.HEART_MOUND.get(), pos, state);
    }

    public void initController(long cityId, long startGameTime, long[] columns) {
        this.controller = true;
        this.cityId = cityId;
        this.startGameTime = startGameTime;
        this.columns = columns;
        setChanged();
    }

    public void serverTick() {
        if (!controller || !(this.level instanceof ServerLevel level)) {
            return;
        }
        if (!forcedApplied) {
            HeartMound.setForced(level, columns, true);
            forcedApplied = true;
        }

        playHeartbeat(level);

        if (!seeded) {
            seedSpreaders(level);
            seeded = true;
            setChanged();
        }
        // Keep the bloom growing while the mound stands (both before and during the fade).
        spreader.updateCursors(level, this.worldPosition, level.getRandom(), true);

        long now = level.getGameTime();
        if (!fading) {
            if (now - startGameTime >= Config.HEART_REVIVE_MINUTES.get() * 1200L) {
                HeartMound.revive(level, this.worldPosition, cityId);
                fading = true;
                fadeStartTime = now;
                setChanged();
            }
            return;
        }

        long fadeTicks = Config.MOUND_FADE_SECONDS.get() * 20L;
        double t = fadeTicks <= 0 ? 1.0 : (double) (now - fadeStartTime) / fadeTicks;
        if (t >= 1.0) {
            HeartMound.removeAll(level, columns);
            return;
        }
        applyFade(level, t);
    }

    /**
     * Seeds the mound's sculk bloom: {@code moundSpreaderCount} charges fanned evenly around the centre
     * at {@code moundSpreaderRadius}, each carrying {@code moundSpreaderCharge}. They spread on their own
     * across sculk-able ground, exactly like a vanilla catalyst's cursors.
     */
    private void seedSpreaders(ServerLevel level) {
        int count = Config.MOUND_SPREADER_COUNT.get();
        if (count <= 0) {
            return;
        }
        int charge = Config.MOUND_SPREADER_CHARGE.get();
        double radius = Config.MOUND_SPREADER_RADIUS.get();
        for (int i = 0; i < count; i++) {
            double angle = 2.0 * Math.PI * i / count;
            int x = this.worldPosition.getX() + (int) Math.round(radius * Math.cos(angle));
            int z = this.worldPosition.getZ() + (int) Math.round(radius * Math.sin(angle));
            spreader.addCursors(new BlockPos(x, this.worldPosition.getY(), z), charge);
        }
    }

    /**
     * Keeps the Heart's heartbeat going from the mound while the ritual runs, at the Alive rate and
     * range (the Alive Heart that became this mound is what's beating). Only the controller ticks.
     */
    private void playHeartbeat(ServerLevel level) {
        int intervalSeconds = Config.HEARTBEAT_ALIVE_SECONDS.get();
        if (intervalSeconds <= 0) {
            return;
        }
        if (level.getGameTime() % (intervalSeconds * 20L) == 0L) {
            float volume = Config.HEARTBEAT_RANGE.get() / 16.0F; // vanilla sound reach is 16 * volume
            level.playSound(null, this.worldPosition, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS, volume, 1.0F);
        }
    }

    /** Every column loses layers at an even pace so all reach 0 at the end of the fade. */
    private void applyFade(ServerLevel level, double t) {
        for (long packed : columns) {
            BlockPos pos = BlockPos.of(packed);
            BlockState state = level.getBlockState(pos);
            if (!state.is(WonBlocks.HEART_MOUND.get())) {
                continue;
            }
            int original = HeartMound.patternHeight(pos.getX() - this.worldPosition.getX(), pos.getZ() - this.worldPosition.getZ());
            int now = (int) Math.ceil(original * (1.0 - t));
            if (now <= 0) {
                level.removeBlock(pos, false);
            } else if (state.getValue(HeartMoundBlock.LAYERS) != now) {
                level.setBlock(pos, state.setValue(HeartMoundBlock.LAYERS, now), 3);
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.controller = tag.getBoolean("controller");
        this.cityId = tag.getLong("cityId");
        this.startGameTime = tag.getLong("startGameTime");
        this.columns = tag.getLongArray("columns");
        this.fading = tag.getBoolean("fading");
        this.fadeStartTime = tag.getLong("fadeStartTime");
        this.seeded = tag.getBoolean("seeded");
        if (tag.contains("spreader")) {
            this.spreader.load(tag.getCompound("spreader"));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("controller", controller);
        tag.putLong("cityId", cityId);
        tag.putLong("startGameTime", startGameTime);
        tag.putLongArray("columns", columns);
        tag.putBoolean("fading", fading);
        tag.putLong("fadeStartTime", fadeStartTime);
        tag.putBoolean("seeded", seeded);
        CompoundTag spreaderTag = new CompoundTag();
        spreader.save(spreaderTag);
        tag.put("spreader", spreaderTag);
    }
}
