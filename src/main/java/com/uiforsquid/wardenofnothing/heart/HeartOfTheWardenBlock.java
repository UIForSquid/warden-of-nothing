package com.uiforsquid.wardenofnothing.heart;

import com.uiforsquid.wardenofnothing.reg.WonBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The Heart of the Warden block (F5). Three stages (Dead/Faint/Alive) via the {@link #STAGE} property.
 * Only a player's empty hand can break it; to everything else it is treated like bedrock (huge blast
 * resistance, immovable by pistons). Creative players can break it, as with bedrock.
 */
public class HeartOfTheWardenBlock extends Block implements EntityBlock {

    public static final EnumProperty<HeartStage> STAGE = EnumProperty.create("stage", HeartStage.class);

    public HeartOfTheWardenBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, HeartStage.DEAD));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeartBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return type == WonBlockEntities.HEART.get()
                ? (lvl, pos, st, be) -> ((HeartBlockEntity) be).serverTick()
                : null;
    }

    /** Only an empty main hand can break it; otherwise it behaves like bedrock (never breaks). */
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!player.getMainHandItem().isEmpty()) {
            return 0.0F;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    /** Drop the Heart item (carrying its stage and hidden progress) when broken by hand in survival. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.getAbilities().instabuild
                && level.getBlockEntity(pos) instanceof HeartBlockEntity heart) {
            ItemStack drop = heart.toItemStack();
            Block.popResource(level, pos, drop);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
