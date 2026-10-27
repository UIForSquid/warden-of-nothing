package com.uiforsquid.wardenofnothing.reg;

import com.uiforsquid.wardenofnothing.WardenOfNothing;
import com.uiforsquid.wardenofnothing.heart.HeartMoundBlock;
import com.uiforsquid.wardenofnothing.heart.HeartOfTheWardenBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Block registry (F5). */
public final class WonBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(WardenOfNothing.MOD_ID);

    /** The Heart of the Warden: hand-breakable, otherwise bedrock-like (huge blast resistance, immovable). */
    public static final DeferredBlock<HeartOfTheWardenBlock> HEART = BLOCKS.register("heart_of_the_warden",
            () -> new HeartOfTheWardenBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.5F, 3_600_000.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)
                    .noLootTable()));

    /** The heart mound: snow-layer-style pile, solid to walk on, unbreakable while the ritual runs. */
    public static final DeferredBlock<HeartMoundBlock> HEART_MOUND = BLOCKS.register("heart_mound",
            () -> new HeartMoundBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(-1.0F, 3_600_000.0F)
                    .sound(SoundType.SNOW)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)
                    .noLootTable()));

    private WonBlocks() {
    }
}
