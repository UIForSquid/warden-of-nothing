package com.uiforsquid.wardenofnothing.reg;

import com.uiforsquid.wardenofnothing.WardenOfNothing;
import com.uiforsquid.wardenofnothing.heart.HeartBlockEntity;
import com.uiforsquid.wardenofnothing.heart.HeartMoundBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Block entity type registry (F5). */
public final class WonBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, WardenOfNothing.MOD_ID);

    public static final Supplier<BlockEntityType<HeartBlockEntity>> HEART = BLOCK_ENTITIES.register("heart",
            () -> BlockEntityType.Builder.of(HeartBlockEntity::new, WonBlocks.HEART.get()).build(null));

    public static final Supplier<BlockEntityType<HeartMoundBlockEntity>> HEART_MOUND = BLOCK_ENTITIES.register("heart_mound",
            () -> BlockEntityType.Builder.of(HeartMoundBlockEntity::new, WonBlocks.HEART_MOUND.get()).build(null));

    private WonBlockEntities() {
    }
}
