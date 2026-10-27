package com.uiforsquid.wardenofnothing.mixin;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets {@link com.uiforsquid.wardenofnothing.warden.AttributeCaps} raise a vanilla attribute's ceiling. */
@Mixin(RangedAttribute.class)
public interface RangedAttributeAccessor {

    @Mutable
    @Accessor("maxValue")
    void wardenofnothing$setMaxValue(double maxValue);
}
