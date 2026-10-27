package com.uiforsquid.wardenofnothing.mixin;

import com.uiforsquid.wardenofnothing.reg.WonItems;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * F5: a dropped Heart item can't be destroyed by anything (lava, fire, cactus, explosions, anvils,
 * other mods). It can still despawn after its normal lifetime, and falling into the void still deletes
 * it (that happens through the below-world discard, not through hurt).
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void wardenofnothing$heartIsIndestructible(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (self.getItem().is(WonItems.HEART.get())) {
            cir.setReturnValue(false);
        }
    }
}
