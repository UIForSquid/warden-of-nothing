package com.uiforsquid.wardenofnothing.heart;

import com.uiforsquid.wardenofnothing.reg.WonBlockEntities;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The heart mound (F5): the placeholder pile of snow-layer-style blocks an Alive heart turns into.
 * Solid to walk on (collision matches height, 1-8 layers), and unbreakable while the ritual runs
 * (bedrock-like). Only the centre column carries a controller block entity that runs the timer.
 */
public class HeartMoundBlock extends Block implements EntityBlock {

    public static final IntegerProperty LAYERS = BlockStateProperties.LAYERS;

    private static final VoxelShape[] SHAPE_BY_LAYER = new VoxelShape[9];

    static {
        for (int i = 0; i <= 8; i++) {
            SHAPE_BY_LAYER[i] = Shapes.box(0.0, 0.0, 0.0, 1.0, i * 2 / 16.0, 1.0);
        }
    }

    public HeartMoundBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LAYERS, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYERS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_LAYER[state.getValue(LAYERS)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE_BY_LAYER[state.getValue(LAYERS)];
    }

    /** Unbreakable in survival (like bedrock) while the ritual runs. */
    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return 0.0F;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeartMoundBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return type == WonBlockEntities.HEART_MOUND.get()
                ? (lvl, pos, st, be) -> ((HeartMoundBlockEntity) be).serverTick()
                : null;
    }
}
