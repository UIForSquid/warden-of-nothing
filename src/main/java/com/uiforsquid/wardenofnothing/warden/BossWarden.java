package com.uiforsquid.wardenofnothing.warden;

import com.uiforsquid.wardenofnothing.config.Config;
import net.minecraft.core.Holder;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.warden.Warden;

/** F1: applies boss stats to a Warden and answers whether a given Warden is a boss. */
public final class BossWarden {

    private BossWarden() {
    }

    /**
     * A Warden is a boss if it's a City Warden (always), or it's any other Warden and
     * {@code bossForOtherSpawns} is on.
     */
    public static boolean isBoss(Warden warden) {
        return isBoss(CityWardens.isCityWarden(warden));
    }

    /** {@link #isBoss(Warden)} for a caller that already knows whether the Warden is a City Warden. */
    public static boolean isBoss(boolean cityWarden) {
        return cityWarden || Config.BOSS_FOR_OTHER_SPAWNS.get();
    }

    /**
     * Sets the boss attribute base values from config. Leaves the Warden's current health untouched;
     * the caller sets health (full for a fresh boss, remembered+healing for a slot summon).
     */
    public static void applyStats(Warden warden) {
        setBase(warden, Attributes.MAX_HEALTH, Config.MAX_HEALTH.get());
        setBase(warden, Attributes.MOVEMENT_SPEED, Config.MOVEMENT_SPEED.get());
        setBase(warden, Attributes.ATTACK_KNOCKBACK, Config.ATTACK_KNOCKBACK.get());
        setBase(warden, Attributes.ATTACK_DAMAGE, Config.MELEE_DAMAGE.get());
        setBase(warden, Attributes.ARMOR, Config.ARMOR.get());
        setBase(warden, Attributes.KNOCKBACK_RESISTANCE, Config.KNOCKBACK_RESISTANCE.get());
        setBase(warden, Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, Config.KNOCKBACK_RESISTANCE.get());
        setBase(warden, Attributes.FOLLOW_RANGE, Config.FOLLOW_RANGE.get());
        setBase(warden, Attributes.STEP_HEIGHT, Config.WARDEN_STEP_HEIGHT.get());
    }

    /** The melee damage a boss deals right now, after difficulty scaling (F1 / owner decision). */
    public static double scaledMeleeDamage(Difficulty difficulty) {
        double base = Config.MELEE_DAMAGE.get();
        return switch (difficulty) {
            case EASY -> base * Config.MELEE_DAMAGE_EASY_MULTIPLIER.get();
            case HARD -> base * Config.MELEE_DAMAGE_HARD_MULTIPLIER.get();
            default -> base; // NORMAL (and PEACEFUL, where no melee on players happens anyway)
        };
    }

    private static void setBase(Warden warden, Holder<Attribute> attribute, double value) {
        AttributeInstance instance = warden.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }
}
