package com.uiforsquid.wardenofnothing.event;

import com.uiforsquid.wardenofnothing.WardenOfNothing;
import com.uiforsquid.wardenofnothing.home.HomeRangeManager;
import com.uiforsquid.wardenofnothing.warden.CityWardens;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * F8 wiring: the returning hit counter. The per-tick supervisor is driven from {@link WardenTickEvents};
 * the home-range state lives on the Warden (see {@code WardenRuntime}), so there is nothing to clean up.
 */
@EventBusSubscriber(modid = WardenOfNothing.MOD_ID)
public final class HomeRangeEvents {

    private HomeRangeEvents() {
    }

    /** Every time a returning City Warden actually takes damage, count it toward the dig threshold. */
    @SubscribeEvent
    public static void onWardenDamaged(LivingDamageEvent.Post event) {
        if (event.getNewDamage() > 0.0F
                && event.getEntity() instanceof Warden warden
                && warden.level() instanceof ServerLevel
                && CityWardens.isCityWarden(warden)) {
            HomeRangeManager.onDamagedWhileReturning(warden);
        }
    }
}
