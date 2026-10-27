package com.uiforsquid.wardenofnothing.heart;

import com.uiforsquid.wardenofnothing.config.Config;
import com.uiforsquid.wardenofnothing.reg.WonBlockEntities;
import com.uiforsquid.wardenofnothing.reg.WonComponents;
import com.uiforsquid.wardenofnothing.reg.WonItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** F5: stores a placed Heart's hidden trigger progress, plays its heartbeat, and advances its stage. */
public class HeartBlockEntity extends BlockEntity {

    private int progress;

    /** Whether this Heart is in {@link HeartIndex}; added on its first tick, cleared on removal. */
    private boolean indexed;

    public HeartBlockEntity(BlockPos pos, BlockState state) {
        super(WonBlockEntities.HEART.get(), pos, state);
    }

    public HeartStage getStage() {
        return getBlockState().getValue(HeartOfTheWardenBlock.STAGE);
    }

    public void setProgress(int progress) {
        this.progress = progress;
        setChanged();
    }

    public void serverTick() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!indexed) {
            HeartIndex.add(serverLevel, this.worldPosition);
            indexed = true;
        }
        playHeartbeat(serverLevel);
    }

    private void playHeartbeat(ServerLevel level) {
        HeartStage stage = getStage();
        int intervalSeconds = switch (stage) {
            case FAINT -> Config.HEARTBEAT_FAINT_SECONDS.get();
            case ALIVE -> Config.HEARTBEAT_ALIVE_SECONDS.get();
            default -> 0; // Dead: silent
        };
        if (intervalSeconds <= 0) {
            return;
        }
        if (level.getGameTime() % (intervalSeconds * 20L) == 0L) {
            float volume = Config.HEARTBEAT_RANGE.get() / 16.0F; // vanilla sound reach is 16 * volume
            level.playSound(null, this.worldPosition, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS, volume, 1.0F);
        }
    }

    /** Called when a sculk pulse is absorbed by this Heart (F5). Advances the stage, or runs the ritual. */
    public void onPulseAbsorbed(ServerLevel level) {
        HeartStage stage = getStage();
        if (stage == HeartStage.ALIVE) {
            HeartRitual.tryPerform(level, this.worldPosition);
            return;
        }
        progress++;
        int threshold = stage == HeartStage.DEAD
                ? Config.HEART_TRIGGERS_TO_FAINT.get()
                : Config.HEART_TRIGGERS_TO_ALIVE.get();
        if (progress >= threshold) {
            progress = 0;
            level.setBlock(this.worldPosition, getBlockState().setValue(HeartOfTheWardenBlock.STAGE, stage.next()), 3);
        }
        setChanged();
    }

    /** The item form of this Heart, carrying its current stage and hidden progress. */
    public ItemStack toItemStack() {
        ItemStack stack = new ItemStack(WonItems.HEART.get());
        stack.set(WonComponents.HEART_DATA.get(), new HeartData(getStage(), progress));
        return stack;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.progress = tag.getInt("progress");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("progress", this.progress);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        indexed = false;
        if (this.level != null) {
            HeartIndex.remove(this.level, this.worldPosition);
        }
    }
}
