package com.uiforsquid.wardenofnothing.heart;

import com.uiforsquid.wardenofnothing.reg.WonComponents;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Heart of the Warden item (F5). Carries the heart's stage and hidden progress as a data component,
 * places the block in that stage, shows a stage-specific name, and never stacks. Indestructibility of
 * the dropped item is handled by {@code ItemEntityMixin}.
 */
public class HeartItem extends BlockItem {

    public HeartItem(Block block, Properties properties) {
        super(block, properties);
    }

    private static HeartData data(ItemStack stack) {
        return stack.getOrDefault(WonComponents.HEART_DATA.get(), HeartData.DEAD);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.wardenofnothing.heart_of_the_warden." + data(stack).stage().getSerializedName());
    }

    @Nullable
    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null) {
            return null;
        }
        return state.setValue(HeartOfTheWardenBlock.STAGE, data(context.getItemInHand()).stage());
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        HeartData data = data(context.getItemInHand());
        InteractionResult result = super.place(context);
        if (result.consumesAction()
                && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof HeartBlockEntity heart) {
            heart.setProgress(data.progress());
        }
        return result;
    }
}
