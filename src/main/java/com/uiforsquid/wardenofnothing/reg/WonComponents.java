package com.uiforsquid.wardenofnothing.reg;

import com.uiforsquid.wardenofnothing.WardenOfNothing;
import com.uiforsquid.wardenofnothing.heart.HeartData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Data component registry (F5): the Heart item's hidden stage + progress. */
public final class WonComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, WardenOfNothing.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<HeartData>> HEART_DATA =
            COMPONENTS.registerComponentType("heart_data",
                    builder -> builder.persistent(HeartData.CODEC).networkSynchronized(HeartData.STREAM_CODEC));

    private WonComponents() {
    }
}
