package com.uiforsquid.wardenofnothing.mixin;

import com.uiforsquid.wardenofnothing.config.Config;
import com.uiforsquid.wardenofnothing.warden.BossWarden;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cobweb immunity for the boss Warden. A cobweb's only effect is {@code entityInside} calling
 * {@code makeStuckInBlock}, which slows the entity to a crawl; cancelling that call for a boss Warden
 * means the web neither slows nor holds it. Cobwebs have no collision shape, so nothing else about the
 * block affects movement. Gated to bosses, like the Warden's other combat traits.
 */
@Mixin(WebBlock.class)
public abstract class WebBlockMixin {

    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void wardenofnothing$wardenIgnoresWebs(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (entity instanceof Warden warden && Config.SPEC.isLoaded()
                && Config.WARDEN_COBWEB_IMMUNE.get() && BossWarden.isBoss(warden)) {
            ci.cancel();
        }
    }
}
