package com.uiforsquid.wardenofnothing.warden;

import com.uiforsquid.wardenofnothing.city.CityTracker;
import com.uiforsquid.wardenofnothing.config.Config;
import com.uiforsquid.wardenofnothing.data.CityData;
import com.uiforsquid.wardenofnothing.data.SlotState;
import com.uiforsquid.wardenofnothing.data.TrackedCity;
import com.uiforsquid.wardenofnothing.data.WardenSlot;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.warden.AngerLevel;
import net.minecraft.world.entity.monster.warden.Warden;

/** F2: turns a natural shrieker's summon into a slot-based summon of one of the city's Wardens. */
public final class WardenSummoner {

    private WardenSummoner() {
    }

    /**
     * Summons the lowest-numbered burrowed Warden of the tracked city containing the shrieker, the
     * vanilla way (TRIGGERED spawn near the shrieker, emerge animation), with its remembered health
     * plus healing. Returns whether a Warden was summoned.
     *
     * <ul>
     *   <li>No tracked city / defeated city → no summon.
     *   <li>No burrowed slot (all alive Wardens already out) → no summon (the parked city attack would
     *       respond here).
     *   <li>Vanilla spawn placement fails (no room) → no summon; the slot stays burrowed.
     * </ul>
     */
    public static boolean summonFromSlot(ServerLevel level, BlockPos shriekerPos) {
        CityData data = CityTracker.get(level);
        TrackedCity city = data.findCityContaining(shriekerPos);
        if (city == null || city.isDefeated()) {
            return false;
        }
        // Self-heal partial recording: ensure this shrieker is one of the city's recorded shriekers,
        // so it is switched off with the rest when the city is defeated (it may not have been recorded
        // at generation if its chunk generated before the city was tracked).
        CityTracker.recordChunk(level, city.getId(), city.getBounds(),
                new LongArrayList(new long[]{shriekerPos.asLong()}));
        int index = city.lowestBurrowedSlotIndex();
        if (index < 0) {
            return false;
        }

        Optional<Warden> spawned = SpawnUtil.trySpawnMob(
                EntityType.WARDEN, MobSpawnType.TRIGGERED, level, shriekerPos, 20, 5, 6,
                SpawnUtil.Strategy.ON_TOP_OF_COLLIDER);
        if (spawned.isEmpty()) {
            return false;
        }

        Warden warden = spawned.get();
        // City Wardens are always bosses, regardless of bossForOtherSpawns.
        BossWarden.applyStats(warden);
        WardenSlot slot = city.getSlots().get(index);
        float maxHealth = warden.getMaxHealth();
        float health = WardenHealth.computeSummonHealth(slot, maxHealth, level.getGameTime());
        warden.setHealth(Math.min(health, maxHealth));

        CityWardens.mark(warden, city.getId(), index);
        slot.setState(SlotState.OUT);
        slot.setOutWardenId(warden.getUUID());
        slot.setBurrowedAtGameTime(-1L);
        data.setDirty();
        return true;
    }

    /**
     * When a shrieker inside a tracked, undefeated city shrieks at a player, point every one of the
     * city's out Wardens at that player: the player becomes its top anger target (so it knows where the
     * player is) and its attack target, which puts the brain into the FIGHT/chase activity — the
     * Warden's running pursuit. Burrowed Wardens aren't in the world, so only out ones are alerted.
     */
    public static void alertOutWardens(ServerLevel level, BlockPos shriekerPos, ServerPlayer player) {
        if (!Config.SHRIEKER_ALERTS_WARDENS.get()) {
            return;
        }
        TrackedCity city = CityTracker.findCity(level, shriekerPos);
        if (city == null || city.isDefeated()) {
            return;
        }
        for (WardenSlot slot : city.getSlots()) {
            if (slot.getState() != SlotState.OUT || slot.getOutWardenId() == null) {
                continue;
            }
            if (level.getEntity(slot.getOutWardenId()) instanceof Warden warden) {
                // Anger first: increaseAngerAt can clear a non-player attack target, so set it after.
                warden.increaseAngerAt(player, AngerLevel.ANGRY.getMinimumAnger() + 20, true);
                warden.setAttackTarget(player);
            }
        }
    }
}
