package com.uiforsquid.wardenofnothing.mixin;

import com.uiforsquid.wardenofnothing.config.Config;
import net.minecraft.world.entity.monster.warden.AngerManagement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * {@code angerDecay}: vanilla takes 1 anger off each suspect per anger tick, dropping a suspect once it
 * would reach 0 ({@code if (anger > 1) anger - 1 else forget}). Each of the three suspect loops in
 * {@code tick} has that compare-then-subtract pair; both constants become the decay so the rule
 * generalises to "forget once anger would reach 0". Ordinal 0 is the UUID-conversion countdown and is
 * left alone.
 */
@Mixin(AngerManagement.class)
public abstract class AngerManagementMixin {

    @ModifyConstant(method = "tick", constant = {
            @Constant(intValue = 1, ordinal = 1), @Constant(intValue = 1, ordinal = 2),
            @Constant(intValue = 1, ordinal = 3), @Constant(intValue = 1, ordinal = 4),
            @Constant(intValue = 1, ordinal = 5), @Constant(intValue = 1, ordinal = 6)})
    private int wardenofnothing$angerDecay(int original) {
        return Config.intOrVanilla(Config.ANGER_DECAY, original);
    }
}
