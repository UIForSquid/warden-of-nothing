package com.uiforsquid.wardenofnothing.config;

import net.minecraft.world.BossEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Every tunable for Warden of Nothing.
 *
 * <p>This is a NeoForge SERVER config ({@code wardenofnothing-server.toml}, created in each world's
 * {@code serverconfig} folder). Every number, chance, rate, timer, toggle and list in the design doc
 * lives here, with no hard-coded tuning values elsewhere in the mod.
 *
 * <p>Comment convention (from the owner's other mods): each setting says what it does in play, its
 * default and range with units, and when a change takes effect. Unless a comment says "Takes effect:
 * immediately", a changed value is read the next time the mod acts on it &mdash; so for most stats
 * that means Wardens spawned or summoned after the change, after the world has reloaded the config.
 *
 * <p>Vanilla 1.21.1 values quoted in comments were read from the decompiled sources and are used as
 * the defaults for every "kept at vanilla" setting.
 */
public final class Config {

    public static final ModConfigSpec SPEC;

    /** Upper ends of the stat ranges below; {@code AttributeCaps} lifts vanilla's attribute ceilings to fit them. */
    public static final double MAX_HEALTH_LIMIT = 100000.0;
    public static final double MELEE_DAMAGE_LIMIT = 100000.0;
    public static final double MELEE_MULTIPLIER_LIMIT = 100.0;
    public static final double ATTACK_KNOCKBACK_LIMIT = 100.0;

    // ----- Boss stats (changed from vanilla) -----
    public static final ModConfigSpec.BooleanValue BOSS_FOR_OTHER_SPAWNS;
    public static final ModConfigSpec.DoubleValue MAX_HEALTH;
    public static final ModConfigSpec.DoubleValue MELEE_DAMAGE;
    public static final ModConfigSpec.DoubleValue MELEE_DAMAGE_EASY_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MELEE_DAMAGE_HARD_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue ATTACK_KNOCKBACK;
    public static final ModConfigSpec.DoubleValue MOVEMENT_SPEED;
    public static final ModConfigSpec.DoubleValue SONIC_BOOM_DAMAGE;
    public static final ModConfigSpec.DoubleValue SONIC_BOOM_KNOCKBACK_MULTIPLIER;

    // ----- Boss stats and behaviour (kept at vanilla, but tunable) -----
    public static final ModConfigSpec.DoubleValue ARMOR;
    public static final ModConfigSpec.DoubleValue KNOCKBACK_RESISTANCE;
    public static final ModConfigSpec.IntValue MELEE_COOLDOWN;
    public static final ModConfigSpec.IntValue SONIC_BOOM_COOLDOWN;
    public static final ModConfigSpec.IntValue SONIC_BOOM_RANGE_HORIZONTAL;
    public static final ModConfigSpec.IntValue SONIC_BOOM_RANGE_VERTICAL;
    public static final ModConfigSpec.IntValue SONIC_BOOM_CHARGE_TIME;
    public static final ModConfigSpec.DoubleValue FOLLOW_RANGE;
    public static final ModConfigSpec.IntValue VIBRATION_LISTEN_RANGE;
    public static final ModConfigSpec.IntValue SNIFF_RANGE;
    public static final ModConfigSpec.IntValue ANGER_PER_VIBRATION;
    public static final ModConfigSpec.IntValue ANGER_PER_PROJECTILE;
    public static final ModConfigSpec.IntValue ANGER_PER_HIT;
    public static final ModConfigSpec.IntValue ANGER_THRESHOLD;
    public static final ModConfigSpec.IntValue ANGER_DECAY;
    public static final ModConfigSpec.IntValue EMERGE_TIME;
    public static final ModConfigSpec.IntValue DIG_TIME;

    // ----- Leaving and healing -----
    public static final ModConfigSpec.IntValue BURROW_DELAY_SECONDS;
    public static final ModConfigSpec.IntValue FULL_HEAL_MINUTES;
    public static final ModConfigSpec.DoubleValue HEAL_CURVE_STEEPNESS;
    public static final ModConfigSpec.BooleanValue BURROW_ON_TELEPORT;

    // ----- Staying home -----
    public static final ModConfigSpec.IntValue HOME_RANGE;
    public static final ModConfigSpec.IntValue HOME_RANGE_UP;
    public static final ModConfigSpec.IntValue WALK_BACK_DISTANCE;
    public static final ModConfigSpec.IntValue WALK_BACK_TIMEOUT_SECONDS;
    public static final ModConfigSpec.IntValue RETURN_HITS_BEFORE_BURROW;
    public static final ModConfigSpec.IntValue RETURN_HIT_RESET_SECONDS;
    public static final ModConfigSpec.IntValue HOME_CHECK_TICKS;

    // ----- Unmovable -----
    public static final ModConfigSpec.BooleanValue WARDENS_UNMOVABLE;

    // ----- Movement abilities -----
    public static final ModConfigSpec.DoubleValue WARDEN_STEP_HEIGHT;
    public static final ModConfigSpec.BooleanValue WARDEN_CAN_CLIMB;

    // ----- Shriekers -----
    public static final ModConfigSpec.BooleanValue SHRIEKER_ALERTS_WARDENS;

    // ----- Heart of the Warden -----
    public static final ModConfigSpec.IntValue HEART_TRIGGERS_TO_FAINT;
    public static final ModConfigSpec.IntValue HEART_TRIGGERS_TO_ALIVE;
    public static final ModConfigSpec.IntValue HEART_PULL_RANGE;
    public static final ModConfigSpec.IntValue HEART_REVIVE_MINUTES;
    public static final ModConfigSpec.IntValue MOUND_FADE_SECONDS;
    public static final ModConfigSpec.IntValue HEARTBEAT_FAINT_SECONDS;
    public static final ModConfigSpec.IntValue HEARTBEAT_ALIVE_SECONDS;
    public static final ModConfigSpec.IntValue HEARTBEAT_RANGE;
    public static final ModConfigSpec.IntValue MOUND_SPREADER_COUNT;
    public static final ModConfigSpec.IntValue MOUND_SPREADER_CHARGE;
    public static final ModConfigSpec.IntValue MOUND_SPREADER_RADIUS;

    // ----- Assignment -----
    public static final ModConfigSpec.IntValue MAX_ASSIGNABLE_WARDENS;
    public static final ModConfigSpec.IntValue ASSIGN_CHECK_TICKS;

    // ----- Boss bar -----
    public static final ModConfigSpec.EnumValue<BossEvent.BossBarColor> BOSS_BAR_COLOR;
    public static final ModConfigSpec.ConfigValue<String> BOSS_BAR_TEXT;
    public static final ModConfigSpec.IntValue BOSS_BAR_RANGE_HORIZONTAL;
    public static final ModConfigSpec.IntValue BOSS_BAR_RANGE_VERTICAL;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("Warden of Nothing — server configuration.",
                "Turns the Warden into a boss that belongs to its Ancient City.");

        // =====================================================================
        b.push("bossStats");
        b.comment("Boss stats that differ from vanilla.");

        BOSS_FOR_OTHER_SPAWNS = b
                .comment("Unassigned Wardens (spawn eggs, /summon, spawners, old cities, other mods) also get boss",
                        "stats and the boss bar. City Wardens are always bosses regardless of this setting.",
                        "Default: true. Takes effect: for Wardens spawned after the change.")
                .define("bossForOtherSpawns", true);

        MAX_HEALTH = b
                .comment("Boss max health, in half-hearts. Vanilla Warden is 500.",
                        "Default: 1500. Range: 1-100000. Takes effect: for Wardens spawned or summoned after the change")
                .defineInRange("maxHealth", 1500.0, 1.0, MAX_HEALTH_LIMIT);

        MELEE_DAMAGE = b
                .comment("Boss melee damage on Normal difficulty, in half-hearts. Vanilla Warden is 30.",
                        "Vanilla does NOT scale Warden melee by difficulty; this mod adds that (see the two",
                        "multipliers below). 90 on Normal one-shots a player in full netherite.",
                        "Default: 90. Range: 0-100000. Takes effect: for Wardens spawned or summoned after the change")
                .defineInRange("meleeDamage", 90.0, 0.0, MELEE_DAMAGE_LIMIT);

        MELEE_DAMAGE_EASY_MULTIPLIER = b
                .comment("Multiplier applied to meleeDamage on Easy difficulty (lower than Normal).",
                        "Easy melee = meleeDamage * this. This difficulty scaling does not exist in vanilla; it was",
                        "added at the owner's request so Easy hits softer and Hard hits harder.",
                        "Default: 0.5. Range: 0-100. Takes effect: per melee hit (live).")
                .defineInRange("meleeDamageEasyMultiplier", 0.5, 0.0, MELEE_MULTIPLIER_LIMIT);

        MELEE_DAMAGE_HARD_MULTIPLIER = b
                .comment("Multiplier applied to meleeDamage on Hard difficulty (higher than Normal).",
                        "Hard melee = meleeDamage * this. Normal always uses meleeDamage unchanged.",
                        "Default: 1.5. Range: 0-100. Takes effect: per melee hit (live).")
                .defineInRange("meleeDamageHardMultiplier", 1.5, 0.0, MELEE_MULTIPLIER_LIMIT);

        ATTACK_KNOCKBACK = b
                .comment("How far a melee hit throws the target. Vanilla Warden is 1.5.",
                        "Default: 4.5. Range: 0-100. Takes effect: for Wardens spawned or summoned after the change")
                .defineInRange("attackKnockback", 4.5, 0.0, ATTACK_KNOCKBACK_LIMIT);

        MOVEMENT_SPEED = b
                .comment("Boss walking speed (movement speed attribute). Vanilla Warden is 0.3.",
                        "Default: 0.45. Range: 0-10. Takes effect: for Wardens spawned or summoned after the change")
                .defineInRange("movementSpeed", 0.45, 0.0, 10.0);

        SONIC_BOOM_DAMAGE = b
                .comment("Sonic boom damage, in half-hearts. Ignores armour and shields. Vanilla is 10.",
                        "Vanilla does not scale the sonic boom by difficulty, so neither does the boss.",
                        "Default: 30. Range: 0-100000. Takes effect: per boom (live).")
                .defineInRange("sonicBoomDamage", 30.0, 0.0, 100000.0);

        SONIC_BOOM_KNOCKBACK_MULTIPLIER = b
                .comment("How much harder the sonic boom pushes than vanilla (vanilla push is 2.5 horizontal /",
                        "0.5 vertical, before knockback resistance). The boss multiplies both by this.",
                        "Default: 3. Range: 0-100. Takes effect: per boom (live).")
                .defineInRange("sonicBoomKnockbackMultiplier", 3.0, 0.0, 100.0);

        b.pop();

        // =====================================================================
        b.push("vanillaTunables");
        b.comment("Stats and behaviour kept at vanilla values, exposed so they can still be changed.",
                "Defaults are the real vanilla 1.21.1 values.");

        ARMOR = b
                .comment("Armour points. Vanilla Warden is 0.",
                        "Default: 0. Range: 0-30. Takes effect: for Wardens spawned or summoned after the change")
                .defineInRange("armor", 0.0, 0.0, 30.0);

        KNOCKBACK_RESISTANCE = b
                .comment("Knockback resistance. 1.0 = cannot be knocked back. Vanilla Warden is 1.0.",
                        "Default: 1.0. Range: 0-1. Takes effect: for Wardens spawned or summoned after the change")
                .defineInRange("knockbackResistance", 1.0, 0.0, 1.0);

        MELEE_COOLDOWN = b
                .comment("Ticks between melee attacks. Vanilla Warden is 18 (20 ticks = 1 second).",
                        "Default: 18. Range: 1-1200. Takes effect: for Wardens spawned or loaded after the change")
                .defineInRange("meleeCooldown", 18, 1, 1200);

        SONIC_BOOM_COOLDOWN = b
                .comment("Ticks between sonic booms. Vanilla Warden is 40.",
                        "Default: 40. Range: 1-6000. Takes effect: per boom (live).")
                .defineInRange("sonicBoomCooldown", 40, 1, 6000);

        SONIC_BOOM_RANGE_HORIZONTAL = b
                .comment("How far the sonic boom reaches horizontally, in blocks. Vanilla Warden is 15.",
                        "Default: 15. Range: 1-128. Takes effect: per boom (live).")
                .defineInRange("sonicBoomRangeHorizontal", 15, 1, 128);

        SONIC_BOOM_RANGE_VERTICAL = b
                .comment("How far the sonic boom reaches vertically, in blocks. Vanilla Warden is 20.",
                        "Default: 20. Range: 1-128. Takes effect: per boom (live).")
                .defineInRange("sonicBoomRangeVertical", 20, 1, 128);

        SONIC_BOOM_CHARGE_TIME = b
                .comment("Wind-up in ticks before the sonic boom fires (vanilla plays the sound/damage at 34).",
                        "Default: 34. Range: 1-200. Takes effect: per boom (live).")
                .defineInRange("sonicBoomChargeTime", 34, 1, 200);

        FOLLOW_RANGE = b
                .comment("How far the boss keeps chasing a target, in blocks. Vanilla Warden is 16.",
                        "Default: 16. Range: 0-2048. Takes effect: for Wardens spawned or summoned after the change")
                .defineInRange("followRange", 16.0, 0.0, 2048.0);

        VIBRATION_LISTEN_RANGE = b
                .comment("How far the boss hears vibrations, in blocks. Vanilla Warden is 16.",
                        "Default: 16. Range: 1-128. Takes effect: for Wardens spawned or summoned after the change")
                .defineInRange("vibrationListenRange", 16, 1, 128);

        SNIFF_RANGE = b
                .comment("How far the boss's sniffing detects entities, in blocks. Vanilla Warden is 24.",
                        "Default: 24. Range: 1-128. Takes effect: live.")
                .defineInRange("sniffRange", 24, 1, 128);

        ANGER_PER_VIBRATION = b
                .comment("Anger added by a vibration or being touched. Vanilla Warden is 35.",
                        "Default: 35. Range: 0-150. Takes effect: live.")
                .defineInRange("angerPerVibration", 35, 0, 150);

        ANGER_PER_PROJECTILE = b
                .comment("Anger added by a projectile landing nearby (when no recent projectile). Vanilla is 10.",
                        "Default: 10. Range: 0-150. Takes effect: live.")
                .defineInRange("angerPerProjectile", 10, 0, 150);

        ANGER_PER_HIT = b
                .comment("Anger added when the boss is hurt. Vanilla is 100 (the angry threshold of 80 plus 20).",
                        "Default: 100. Range: 0-150. Takes effect: live.")
                .defineInRange("angerPerHit", 100, 0, 150);

        ANGER_THRESHOLD = b
                .comment("Anger needed before the boss locks onto a target and attacks. Vanilla Warden is 80.",
                        "Default: 80. Range: 1-150. Takes effect: live.")
                .defineInRange("angerThreshold", 80, 1, 150);

        ANGER_DECAY = b
                .comment("How much anger fades per second toward each suspect. Vanilla Warden is 1.",
                        "Default: 1. Range: 0-150. Takes effect: live.")
                .defineInRange("angerDecay", 1, 0, 150);

        EMERGE_TIME = b
                .comment("Length of the emerge animation, in ticks. Vanilla Warden is 134.",
                        "Default: 134. Range: 1-1200. Takes effect: for Wardens spawned or loaded after the change.")
                .defineInRange("emergeTime", 134, 1, 1200);

        DIG_TIME = b
                .comment("Length of the dig (burrow) animation, in ticks. Vanilla Warden is 100.",
                        "Default: 100. Range: 1-1200. Takes effect: for Wardens spawned or loaded after the change.")
                .defineInRange("digTime", 100, 1, 1200);

        b.pop();

        // =====================================================================
        b.push("leavingAndHealing");

        BURROW_DELAY_SECONDS = b
                .comment("How long a Warden goes undisturbed before it leaves (burrows). Vanilla is 60.",
                        "Restarted by everything that restarts vanilla's dig timer.",
                        "Default: 120. Range: 1-3600 seconds. Takes effect: for Wardens spawned or summoned after")
                .defineInRange("burrowDelaySeconds", 120, 1, 3600);

        FULL_HEAL_MINUTES = b
                .comment("How long a burrowed City Warden takes to heal back to full health, in minutes.",
                        "Default: 10. Range: 1-1440 minutes. Takes effect: for heal amounts computed after the change")
                .defineInRange("fullHealMinutes", 10, 1, 1440);

        HEAL_CURVE_STEEPNESS = b
                .comment("How sharply underground healing speeds up the longer a Warden is down.",
                        "0 = straight line; higher = more back-loaded (most healing near the end).",
                        "Default: 3. Range: 0-20. Takes effect: for heal amounts computed after the change")
                .defineInRange("healCurveSteepness", 3.0, 0.0, 20.0);

        BURROW_ON_TELEPORT = b
                .comment("A City Warden that changes dimension, or is teleported too far from home, burrows and",
                        "returns home. When false, dimension change and far teleport do not force a burrow.",
                        "Default: true. Takes effect: live.")
                .define("burrowOnTeleport", true);

        b.pop();

        // =====================================================================
        b.push("stayingHome");

        HOME_RANGE = b
                .comment("How far outside the edge of its city (the structure bounding box) a City Warden will go",
                        "sideways, in blocks. There is no downward limit.",
                        "Default: 124. Range: 0-512 blocks. Takes effect: live.")
                .defineInRange("homeRange", 124, 0, 512);

        HOME_RANGE_UP = b
                .comment("How far above the top of its city's bounding box a City Warden will go, in blocks.",
                        "Default: 124. Range: 0-512 blocks. Takes effect: live.")
                .defineInRange("homeRangeUp", 124, 0, 512);

        WALK_BACK_DISTANCE = b
                .comment("A City Warden outside its home range by up to this many blocks walks back in; further than",
                        "this, it burrows.",
                        "Default: 32. Range: 0-512 blocks. Takes effect: live.")
                .defineInRange("walkBackDistance", 32, 0, 512);

        WALK_BACK_TIMEOUT_SECONDS = b
                .comment("If a City Warden walking back hasn't reached its home range in this time, it burrows.",
                        "Default: 30. Range: 1-600 seconds. Takes effect: live.")
                .defineInRange("walkBackTimeoutSeconds", 30, 1, 600);

        RETURN_HITS_BEFORE_BURROW = b
                .comment("If a returning City Warden is hurt this many times on the way back, it digs (forced burrow).",
                        "Default: 10. Range: 1-1000. Takes effect: live.")
                .defineInRange("returnHitsBeforeBurrow", 10, 1, 1000);

        RETURN_HIT_RESET_SECONDS = b
                .comment("How long a City Warden must stay inside its city bounds before its return-hit counter",
                        "resets to 0.",
                        "Default: 30. Range: 1-3600 seconds. Takes effect: live.")
                .defineInRange("returnHitResetSeconds", 30, 1, 3600);

        HOME_CHECK_TICKS = b
                .comment("How often each City Warden's position is checked against its home range (20 ticks = 1s).",
                        "Default: 20. Range: 1-200 ticks. Takes effect: live.")
                .defineInRange("homeCheckTicks", 20, 1, 200);

        b.pop();

        // =====================================================================
        b.push("unmovable");

        WARDENS_UNMOVABLE = b
                .comment("When true, every Warden (City and Unassigned) gets the same protection against being moved",
                        "that the Wither and Ender Dragon have, and can never ride any vehicle.",
                        "Default: true. Takes effect: live.")
                .define("wardensUnmovable", true);

        b.pop();

        // =====================================================================
        b.push("movement");

        WARDEN_STEP_HEIGHT = b
                .comment("How high, in blocks, a boss Warden can step up without jumping. Vanilla is 0.6.",
                        "Default: 2.0. Range: 0.0-10.0 blocks. Takes effect: for Wardens spawned or summoned after the change")
                .defineInRange("wardenStepHeight", 2.0, 0.0, 10.0);

        WARDEN_CAN_CLIMB = b
                .comment("When true, a Warden climbs walls like a spider: pressed against a wall, it climbs straight up.",
                        "Default: true. Takes effect: live.")
                .define("wardenCanClimb", true);

        b.pop();

        // =====================================================================
        b.push("shriekers");

        SHRIEKER_ALERTS_WARDENS = b
                .comment("When true, a shrieker going off inside a city points every one of the city's out Wardens at",
                        "the triggering player: the player becomes its anger and attack target, so it charges them.",
                        "Default: true. Takes effect: live.")
                .define("shriekerAlertsWardens", true);

        b.pop();

        // =====================================================================
        b.push("heartOfTheWarden");

        HEART_TRIGGERS_TO_FAINT = b
                .comment("Catalyst triggers (pulses absorbed) near a Dead heart before it becomes Faint.",
                        "Default: 10. Range: 1-10000. Takes effect: for triggers counted after the change")
                .defineInRange("heartTriggersToFaint", 10, 1, 10000);

        HEART_TRIGGERS_TO_ALIVE = b
                .comment("Further catalyst triggers near a Faint heart before it becomes Alive.",
                        "Default: 15. Range: 1-10000. Takes effect: for triggers counted after the change")
                .defineInRange("heartTriggersToAlive", 15, 1, 10000);

        HEART_PULL_RANGE = b
                .comment("Every catalyst pulse within this many blocks of a placed heart is pulled into it.",
                        "Default: 8. Range: 1-64 blocks. Takes effect: live.")
                .defineInRange("heartPullRange", 8, 1, 64);

        HEART_REVIVE_MINUTES = b
                .comment("How long the heart mound takes to revive the city's Warden, in minutes.",
                        "Default: 10. Range: 1-1440 minutes. Takes effect: for mounds created after the change")
                .defineInRange("heartReviveMinutes", 10, 1, 1440);

        MOUND_FADE_SECONDS = b
                .comment("How long the mound takes to fade away after the Warden climbs out. It keeps its chunk",
                        "loaded until it is gone.",
                        "Default: 60. Range: 1-3600 seconds. Takes effect: for mounds that start fading after the change")
                .defineInRange("moundFadeSeconds", 60, 1, 3600);

        HEARTBEAT_FAINT_SECONDS = b
                .comment("How often a placed Faint heart plays its heartbeat, in seconds.",
                        "Default: 3. Range: 1-60 seconds. Takes effect: live.")
                .defineInRange("heartbeatFaintSeconds", 3, 1, 60);

        HEARTBEAT_ALIVE_SECONDS = b
                .comment("How often a placed Alive heart plays its heartbeat, in seconds.",
                        "Default: 1. Range: 1-60 seconds. Takes effect: live.")
                .defineInRange("heartbeatAliveSeconds", 1, 1, 60);

        HEARTBEAT_RANGE = b
                .comment("How far away players can hear a placed heart's heartbeat, in blocks.",
                        "Default: 16. Range: 1-128 blocks. Takes effect: live.")
                .defineInRange("heartbeatRange", 16, 1, 128);

        MOUND_SPREADER_COUNT = b
                .comment("How many sculk-spreading charges (catalyst cursors) the mound sends out when it forms,",
                        "to grow natural-looking sculk around it. 0 disables the bloom.",
                        "Default: 10. Range: 0-64. Takes effect: for mounds created after the change")
                .defineInRange("moundSpreaderCount", 10, 0, 64);

        MOUND_SPREADER_CHARGE = b
                .comment("Charge each mound spreader carries. Higher spreads more sculk (and more sensors/shriekers)",
                        "per charge; vanilla splits charge into cursors of up to 1000.",
                        "Default: 50. Range: 1-1000. Takes effect: for mounds created after the change")
                .defineInRange("moundSpreaderCharge", 50, 1, 1000);

        MOUND_SPREADER_RADIUS = b
                .comment("How far from the mound's centre the spreaders are seeded, in blocks, so they fan outward.",
                        "Default: 3. Range: 1-16 blocks. Takes effect: for mounds created after the change")
                .defineInRange("moundSpreaderRadius", 3, 1, 16);

        b.pop();

        // =====================================================================
        b.push("assignment");

        MAX_ASSIGNABLE_WARDENS = b
                .comment("Most Wardens a city can have alive (burrowed or out) at once. Every city starts with one",
                        "(Warden_1), so with the default of 1 a Warden can only be assigned once the city's Warden is dead.",
                        "Default: 1. Range: 1-1000. Takes effect: for assignment requests processed after the change")
                .defineInRange("maxAssignableWardens", 1, 1, 1000);

        ASSIGN_CHECK_TICKS = b
                .comment("How often the mod looks for Wardens tagged wardenofnothing.assigned (20 ticks = 1 second).",
                        "Default: 20. Range: 1-200 ticks. Takes effect: live.")
                .defineInRange("assignCheckTicks", 20, 1, 200);

        b.pop();

        // =====================================================================
        b.push("bossBar");

        BOSS_BAR_COLOR = b
                .comment("Colour of the boss bar. Vanilla bars only come in these seven colours; blue is closest to sculk.",
                        "Default: BLUE. Takes effect: live.")
                .defineEnum("bossBarColor", BossEvent.BossBarColor.BLUE);

        BOSS_BAR_TEXT = b
                .comment("Text shown on the boss bar. Slot numbers never appear.",
                        "Default: Warden. Takes effect: live.")
                .define("bossBarText", "Warden");

        BOSS_BAR_RANGE_HORIZONTAL = b
                .comment("A player within this horizontal distance of a Warden sees its bar, in blocks.",
                        "Default: 124. Range: 1-512 blocks. Takes effect: live.")
                .defineInRange("bossBarRangeHorizontal", 124, 1, 512);

        BOSS_BAR_RANGE_VERTICAL = b
                .comment("...and within this many blocks above or below it. Together these make a cylinder.",
                        "Default: 64. Range: 1-512 blocks. Takes effect: live.")
                .defineInRange("bossBarRangeVertical", 64, 1, 512);

        b.pop();

        SPEC = b.build();
    }

    private Config() {
    }

    /**
     * Reads an int setting, or {@code vanilla} while the server config isn't loaded. For mixins into
     * code that also runs client-side (Warden brains are built on both sides), where reading an unloaded
     * config value would throw.
     */
    public static int intOrVanilla(ModConfigSpec.IntValue value, int vanilla) {
        return SPEC.isLoaded() ? value.get() : vanilla;
    }
}
