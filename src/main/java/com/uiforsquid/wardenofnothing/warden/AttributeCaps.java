package com.uiforsquid.wardenofnothing.warden;

import com.uiforsquid.wardenofnothing.config.Config;
import com.uiforsquid.wardenofnothing.mixin.RangedAttributeAccessor;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/**
 * Vanilla clamps attribute values to a ceiling: max health 1024, attack damage 2048, attack knockback 5.
 * The boss defaults already pass the first (1500 health), and the config allows far more, so a value
 * above the ceiling would silently be cut down. These ceilings are raised (never lowered) to cover every
 * value the config can produce. This is global, like other attribute-cap mods: it only lifts the limit,
 * so no entity's stats change unless something sets them that high.
 */
public final class AttributeCaps {

    private AttributeCaps() {
    }

    public static void raise() {
        raise(Attributes.MAX_HEALTH, Config.MAX_HEALTH_LIMIT);
        raise(Attributes.ATTACK_DAMAGE, Config.MELEE_DAMAGE_LIMIT * Config.MELEE_MULTIPLIER_LIMIT);
        raise(Attributes.ATTACK_KNOCKBACK, Config.ATTACK_KNOCKBACK_LIMIT);
    }

    private static void raise(Holder<Attribute> holder, double needed) {
        if (holder.value() instanceof RangedAttribute ranged && ranged.getMaxValue() < needed) {
            ((RangedAttributeAccessor) ranged).wardenofnothing$setMaxValue(needed);
        }
    }
}
