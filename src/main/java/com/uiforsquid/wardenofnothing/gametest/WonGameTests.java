package com.uiforsquid.wardenofnothing.gametest;

import com.uiforsquid.wardenofnothing.WardenOfNothing;
import com.uiforsquid.wardenofnothing.assign.AssignmentManager;
import com.uiforsquid.wardenofnothing.city.CityShriekers;
import com.uiforsquid.wardenofnothing.city.CityTracker;
import com.uiforsquid.wardenofnothing.config.Config;
import com.uiforsquid.wardenofnothing.data.SlotState;
import com.uiforsquid.wardenofnothing.data.TrackedCity;
import com.uiforsquid.wardenofnothing.data.WardenSlot;
import com.uiforsquid.wardenofnothing.heart.HeartBlockEntity;
import com.uiforsquid.wardenofnothing.heart.HeartIndex;
import com.uiforsquid.wardenofnothing.heart.HeartMound;
import com.uiforsquid.wardenofnothing.heart.HeartMoundBlock;
import com.uiforsquid.wardenofnothing.heart.HeartOfTheWardenBlock;
import com.uiforsquid.wardenofnothing.heart.HeartStage;
import com.uiforsquid.wardenofnothing.reg.WonBlocks;
import com.uiforsquid.wardenofnothing.reg.WonComponents;
import com.uiforsquid.wardenofnothing.reg.WonItems;
import com.uiforsquid.wardenofnothing.warden.BossBars;
import com.uiforsquid.wardenofnothing.warden.CityWardens;
import com.uiforsquid.wardenofnothing.warden.WardenBurrow;
import com.uiforsquid.wardenofnothing.warden.WardenEntityTags;
import com.uiforsquid.wardenofnothing.warden.WardenHealth;
import com.uiforsquid.wardenofnothing.warden.WardenSummoner;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.warden.AngerLevel;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkShriekerBlock;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import com.uiforsquid.wardenofnothing.warden.BossWarden;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Feature GameTests. Run headless with {@code ./gradlew runGameTestServer}, or in a dev client with
 * {@code /test runall}. Each test builds its own tracked city over its own 16x10x16 area (the template's
 * stone floor is at relative y=1), so tests running side by side never share a city. Expected values are read from the config,
 * so the tests hold for any settings.
 */
@GameTestHolder(WardenOfNothing.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WonGameTests {

    private static final String NS = WardenOfNothing.MOD_ID;
    private static final String FLOOR = "floor";

    private WonGameTests() {
    }

    // ----- helpers -----

    /** This test's whole area as absolute bounds. */
    private static BoundingBox area(GameTestHelper h) {
        return BoundingBox.fromCorners(h.absolutePos(new BlockPos(0, 1, 0)), h.absolutePos(new BlockPos(15, 10, 15)));
    }

    /** Records a fresh city (Warden_1 burrowed) over the given bounds, with the given natural shriekers. */
    private static TrackedCity newCity(GameTestHelper h, BoundingBox bounds, BlockPos... shriekers) {
        ServerLevel level = h.getLevel();
        long id = h.absolutePos(BlockPos.ZERO).asLong();
        LongArrayList packed = new LongArrayList();
        for (BlockPos rel : shriekers) {
            h.setBlock(rel, Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, true));
            packed.add(h.absolutePos(rel).asLong());
        }
        CityTracker.recordChunk(level, id, bounds, packed);
        TrackedCity city = CityTracker.get(level).getCity(id);
        h.assertTrue(city != null, "city was not recorded");
        return city;
    }

    private static void defeat(TrackedCity city) {
        for (WardenSlot slot : city.getSlots()) {
            slot.setState(SlotState.DEAD);
            slot.setOutWardenId(null);
        }
    }

    private static Warden outWarden(GameTestHelper h, WardenSlot slot) {
        h.assertTrue(slot.getOutWardenId() != null, "slot has no out Warden");
        if (!(h.getLevel().getEntity(slot.getOutWardenId()) instanceof Warden warden)) {
            throw new AssertionError("slot's Warden is not in the level");
        }
        return warden;
    }

    private static void assertClose(GameTestHelper h, double expected, double actual, String what) {
        h.assertTrue(Math.abs(expected - actual) < 1.0E-3, what + ": expected " + expected + " but was " + actual);
    }

    // ----- F2 city tracking -----

    /** An Ancient City placed the way world generation does is recorded, with its natural shriekers. */
    @GameTest(templateNamespace = NS, template = FLOOR, timeoutTicks = 200, batch = "worldgen")
    public static void ancientCityIsRecordedAtGeneration(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.ANCIENT_CITY);
        ChunkPos startChunk = new ChunkPos(h.absolutePos(BlockPos.ZERO).offset(8192, 0, 8192));
        StructureStart start = structure.generate(level.registryAccess(), generator, generator.getBiomeSource(),
                level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(), startChunk, 0,
                level, biome -> true);
        h.assertTrue(start.isValid(), "ancient city failed to generate");

        // Same as /place structure: place the start into every chunk it covers.
        BoundingBox bb = start.getBoundingBox();
        ChunkPos min = new ChunkPos(SectionPos.blockToSectionCoord(bb.minX()), SectionPos.blockToSectionCoord(bb.minZ()));
        ChunkPos max = new ChunkPos(SectionPos.blockToSectionCoord(bb.maxX()), SectionPos.blockToSectionCoord(bb.maxZ()));
        ChunkPos.rangeClosed(min, max).forEach(cp -> {
            level.getChunk(cp.x, cp.z);
            start.placeInChunk(level, level.structureManager(), generator, level.getRandom(),
                    new BoundingBox(cp.getMinBlockX(), level.getMinBuildHeight(), cp.getMinBlockZ(),
                            cp.getMaxBlockX(), level.getMaxBuildHeight() - 1, cp.getMaxBlockZ()), cp);
        });

        // Recording hops to the main thread via server.execute, so check from the next tick on.
        h.succeedWhen(() -> {
            TrackedCity city = CityTracker.get(level).getCity(startChunk.toLong());
            h.assertTrue(city != null, "generated city not tracked");
            h.assertTrue(city.getSlots().size() == 1 && city.getSlots().get(0).getState() == SlotState.BURROWED,
                    "a new city should start with Warden_1 burrowed");
            h.assertFalse(city.getNaturalShriekers().isEmpty(), "no natural shriekers recorded");
            for (long packed : city.getNaturalShriekers()) {
                BlockPos pos = BlockPos.of(packed);
                h.assertTrue(level.getBlockState(pos).is(Blocks.SCULK_SHRIEKER), "recorded shrieker at " + pos + " isn't one");
                h.assertTrue(CityTracker.isNaturalShrieker(level, packed), "shrieker at " + pos + " missing from the index");
            }
        });
    }

    /** Breaking a recorded natural shrieker drops it from the record and the index for good. */
    @GameTest(templateNamespace = NS, template = FLOOR)
    public static void removedShriekerIsForgotten(GameTestHelper h) {
        BlockPos rel = new BlockPos(4, 2, 4);
        TrackedCity city = newCity(h, area(h), rel);
        long packed = h.absolutePos(rel).asLong();
        h.assertTrue(city.getNaturalShriekers().contains(packed), "shrieker not recorded");

        // Switching can_summon keeps it a shrieker, so it must stay recorded.
        h.setBlock(rel, Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, false));
        h.assertTrue(city.getNaturalShriekers().contains(packed), "a can_summon switch counted as removal");

        h.setBlock(rel, Blocks.AIR);
        h.assertFalse(city.getNaturalShriekers().contains(packed), "removed shrieker still recorded");
        h.assertFalse(CityTracker.isNaturalShrieker(h.getLevel(), packed), "removed shrieker still indexed");
        h.succeed();
    }

    /** A chunk loading in a defeated city switches its natural shriekers off. */
    @GameTest(templateNamespace = NS, template = FLOOR)
    public static void chunkLoadSwitchesDefeatedShriekersOff(GameTestHelper h) {
        BlockPos rel = new BlockPos(3, 2, 3);
        TrackedCity city = newCity(h, area(h), rel);
        defeat(city);
        BlockPos abs = h.absolutePos(rel);
        CityShriekers.onChunkLoaded(h.getLevel(), h.getLevel().getChunkAt(abs));
        h.assertFalse(h.getBlockState(rel).getValue(SculkShriekerBlock.CAN_SUMMON), "defeated city's shrieker still summons");
        h.succeed();
    }

    // ----- F1/F2/F3 slots, summoning, burrowing, healing -----

    /** A shrieker summons the lowest burrowed slot as a boss; burrowing saves health; a resummon restores it. */
    @GameTest(templateNamespace = NS, template = FLOOR, timeoutTicks = 100)
    public static void slotSummonBurrowAndResummon(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TrackedCity city = newCity(h, area(h));
        BlockPos shrieker = h.absolutePos(new BlockPos(8, 2, 8));

        h.assertTrue(WardenSummoner.summonFromSlot(level, shrieker), "slot summon failed");
        WardenSlot slot = city.getSlots().get(0);
        h.assertTrue(slot.getState() == SlotState.OUT, "slot not OUT after summon");
        Warden warden = outWarden(h, slot);
        h.assertTrue(CityWardens.isCityWarden(warden), "summoned Warden isn't a City Warden");
        h.assertTrue(warden.getTags().contains(WardenEntityTags.CITY_WARDEN_LABEL), "City Warden label missing");
        assertClose(h, Config.MAX_HEALTH.get(), warden.getMaxHealth(), "boss max health");
        assertClose(h, Config.MOVEMENT_SPEED.get(), warden.getAttributeBaseValue(Attributes.MOVEMENT_SPEED), "boss speed");
        h.assertFalse(WardenSummoner.summonFromSlot(level, shrieker), "summoned a second Warden with none burrowed");

        float hurtHealth = (float) (Config.MAX_HEALTH.get() / 3.0);
        warden.setHealth(hurtHealth);
        h.assertTrue(WardenBurrow.onBurrowed(warden, level), "burrow not saved");
        h.assertTrue(slot.getState() == SlotState.BURROWED, "slot not BURROWED");
        assertClose(h, hurtHealth, slot.getRememberedHealth(), "remembered health");
        h.assertFalse(WardenBurrow.onBurrowed(warden, level), "a second burrow save wasn't a no-op");
        warden.discard();

        // Same tick: no time underground, so it comes back at exactly its remembered health.
        h.assertTrue(WardenSummoner.summonFromSlot(level, shrieker), "resummon failed");
        Warden again = outWarden(h, slot);
        assertClose(h, hurtHealth, again.getHealth(), "resummoned health");
        again.discard();
        h.succeed();
    }

    /** The heal curve: none at 0, full at fullHealMinutes, a straight line at steepness 0, back-loaded above it. */
    @GameTest(templateNamespace = NS, template = FLOOR)
    public static void healCurve(GameTestHelper h) {
        assertClose(h, 0.0, WardenHealth.healedShare(0.0, 10.0, 3.0), "share at 0");
        assertClose(h, 1.0, WardenHealth.healedShare(10.0, 10.0, 3.0), "share at full");
        assertClose(h, 1.0, WardenHealth.healedShare(50.0, 10.0, 3.0), "share past full");
        assertClose(h, 0.5, WardenHealth.healedShare(5.0, 10.0, 0.0), "linear half");
        h.assertTrue(WardenHealth.healedShare(5.0, 10.0, 3.0) < 0.5, "steep curve isn't back-loaded");

        WardenSlot slot = new WardenSlot(SlotState.BURROWED, 100.0F, 0L, null);
        long tenMinutes = Config.FULL_HEAL_MINUTES.get() * 1200L;
        assertClose(h, 1000.0, WardenHealth.computeSummonHealth(slot, 1000.0F, tenMinutes), "fully healed after fullHealMinutes");
        h.succeed();
    }

    /** Killing the last Warden defeats the city: shriekers stop summoning and an indestructible Heart drops. */
    @GameTest(templateNamespace = NS, template = FLOOR, timeoutTicks = 100)
    public static void lastDeathDefeatsCityAndDropsHeart(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos shriekerRel = new BlockPos(2, 2, 2);
        TrackedCity city = newCity(h, area(h), shriekerRel);
        h.assertTrue(WardenSummoner.summonFromSlot(level, h.absolutePos(new BlockPos(8, 2, 8))), "summon failed");
        Warden warden = outWarden(h, city.getSlots().get(0));

        warden.kill();
        h.assertTrue(city.getSlots().get(0).getState() == SlotState.DEAD, "slot not DEAD");
        h.assertTrue(city.isDefeated(), "city not defeated");
        h.assertFalse(h.getBlockState(shriekerRel).getValue(SculkShriekerBlock.CAN_SUMMON), "shrieker still summons");

        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(warden.blockPosition()).inflate(3.0),
                e -> e.getItem().is(WonItems.HEART.get()));
        h.assertTrue(drops.size() == 1, "expected one Heart drop, found " + drops.size());
        ItemEntity heart = drops.get(0);
        h.assertFalse(heart.hurt(level.damageSources().lava(), 1000.0F), "Heart item took damage");
        h.assertTrue(heart.isAlive(), "Heart item was destroyed");
        heart.discard();
        h.succeed();
    }

    // ----- F7 assignment -----

    /** A tagged Warden in a city with room becomes its City Warden; one outside any city doesn't. The tag always goes. */
    @GameTest(templateNamespace = NS, template = FLOOR)
    public static void assignmentFillsDeadSlot(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos lo = h.absolutePos(new BlockPos(0, 1, 0));
        TrackedCity city = newCity(h, BoundingBox.fromCorners(lo, lo.offset(7, 9, 15)));
        defeat(city);

        Warden inside = h.spawn(EntityType.WARDEN, new BlockPos(3, 2, 8));
        Warden outside = h.spawn(EntityType.WARDEN, new BlockPos(12, 2, 8));
        inside.addTag(WardenEntityTags.ASSIGNED_REQUEST);
        outside.addTag(WardenEntityTags.ASSIGNED_REQUEST);
        AssignmentManager.processRequests(level);

        h.assertFalse(inside.getTags().contains(WardenEntityTags.ASSIGNED_REQUEST), "request tag not removed (inside)");
        h.assertFalse(outside.getTags().contains(WardenEntityTags.ASSIGNED_REQUEST), "request tag not removed (outside)");
        h.assertTrue(CityWardens.isCityWarden(inside), "Warden in a defeated city wasn't assigned");
        h.assertFalse(CityWardens.isCityWarden(outside), "Warden outside any city was assigned");
        WardenSlot slot = city.getSlots().get(0);
        h.assertTrue(slot.getState() == SlotState.OUT && inside.getUUID().equals(slot.getOutWardenId()), "slot not filled by the assigned Warden");
        inside.discard();
        outside.discard();
        h.succeed();
    }

    // ----- F1 boss bar -----

    /** A boss Warden's bar is shown inside its cylinder and hidden once the player leaves it. */
    @GameTest(templateNamespace = NS, template = FLOOR, timeoutTicks = 60)
    @SuppressWarnings("removal") // the mock-player helper still works in 1.21.1
    public static void bossBarFollowsCylinder(GameTestHelper h) {
        Warden warden = h.spawn(EntityType.WARDEN, new BlockPos(8, 2, 8));
        CityWardens.mark(warden, Long.MIN_VALUE + 1, 0); // a boss whatever bossForOtherSpawns says; no such city
        warden.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 24000L);
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.teleportTo(warden.getX() + 2.0, warden.getY(), warden.getZ());

        h.runAfterDelay(2, () -> {
            h.assertTrue(BossBars.viewersOf(warden.getUUID()).contains(player), "player in range doesn't see the bar");
            player.teleportTo(warden.getX() + Config.BOSS_BAR_RANGE_HORIZONTAL.get() + 10.0, warden.getY(), warden.getZ());
            h.runAfterDelay(2, () -> {
                h.assertFalse(BossBars.viewersOf(warden.getUUID()).contains(player), "player out of range still sees the bar");
                warden.discard();
                player.discard();
                h.succeed();
            });
        });
    }

    // ----- F3/F8 home range and forced burrow -----

    /** A City Warden far outside its home range burrows (forced), and its slot is saved as burrowed. */
    @GameTest(templateNamespace = NS, template = FLOOR, timeoutTicks = 400)
    public static void farOutsideHomeRangeForcesBurrow(GameTestHelper h) {
        BlockPos far = h.absolutePos(BlockPos.ZERO).offset(16384, 0, 0);
        TrackedCity city = newCity(h, BoundingBox.fromCorners(far, far.offset(15, 9, 15)));
        Warden warden = h.spawn(EntityType.WARDEN, new BlockPos(8, 2, 8));
        // Spawned without finalizeSpawn it has no dig cooldown and would dig on its own; give it one so
        // only the forced burrow can make it dig.
        warden.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 24000L);
        WardenSlot slot = city.getSlots().get(0);
        slot.setState(SlotState.OUT);
        slot.setOutWardenId(warden.getUUID());
        CityWardens.mark(warden, city.getId(), 0);

        h.succeedWhen(() -> {
            h.assertTrue(slot.getState() == SlotState.BURROWED,
                    "not burrowed yet (pose " + warden.getPose() + ", removed " + warden.isRemoved() + ")");
            h.assertTrue(warden.isRemoved(), "slot burrowed but the Warden is still in the world");
        });
    }

    // ----- F9 unmovable -----

    /** Wardens can't use portals and are dismounted from any vehicle. */
    @GameTest(templateNamespace = NS, template = FLOOR, timeoutTicks = 40)
    public static void wardensAreUnmovable(GameTestHelper h) {
        Warden warden = h.spawn(EntityType.WARDEN, new BlockPos(8, 2, 8));
        warden.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 24000L);
        boolean unmovable = Config.WARDENS_UNMOVABLE.get();
        h.assertTrue(warden.canUsePortal(false) != unmovable, "portal use doesn't match wardensUnmovable");
        Boat boat = h.spawn(EntityType.BOAT, new BlockPos(4, 2, 4));
        warden.startRiding(boat, true);
        h.assertTrue(warden.isPassenger(), "test setup: forced ride failed");
        h.succeedWhen(() -> {
            h.assertTrue(warden.isPassenger() != unmovable, "vehicle rule doesn't match wardensUnmovable");
            warden.discard();
            boat.discard();
        });
    }

    // ----- configured vanilla tunables -----

    /** The vanilla-tunable mixins apply and read the config (anger threshold, listen range). */
    @GameTest(templateNamespace = NS, template = FLOOR)
    public static void vanillaTunablesAreWired(GameTestHelper h) {
        int threshold = Config.ANGER_THRESHOLD.get();
        h.assertTrue(AngerLevel.byAnger(threshold) == AngerLevel.ANGRY, "anger at the threshold isn't ANGRY");
        h.assertTrue(AngerLevel.byAnger(threshold - 1) != AngerLevel.ANGRY, "anger below the threshold is ANGRY");
        Warden warden = h.spawn(EntityType.WARDEN, new BlockPos(8, 2, 8));
        h.assertTrue(warden.getVibrationUser().getListenerRadius() == Config.VIBRATION_LISTEN_RANGE.get(), "listen range not configured");
        warden.discard();
        h.succeed();
    }

    // ----- F1 boss combat -----

    /** Boss melee is meleeDamage scaled by difficulty, and is applied to the Warden right before it hits. */
    @GameTest(templateNamespace = NS, template = FLOOR)
    public static void bossMeleeScalesWithDifficulty(GameTestHelper h) {
        double base = Config.MELEE_DAMAGE.get();
        assertClose(h, base * Config.MELEE_DAMAGE_EASY_MULTIPLIER.get(), BossWarden.scaledMeleeDamage(Difficulty.EASY), "easy melee");
        assertClose(h, base, BossWarden.scaledMeleeDamage(Difficulty.NORMAL), "normal melee");
        assertClose(h, base * Config.MELEE_DAMAGE_HARD_MULTIPLIER.get(), BossWarden.scaledMeleeDamage(Difficulty.HARD), "hard melee");

        Warden warden = h.spawn(EntityType.WARDEN, new BlockPos(8, 2, 8));
        CityWardens.mark(warden, Long.MIN_VALUE + 1, 0);
        warden.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 24000L);
        IronGolem target = h.spawn(EntityType.IRON_GOLEM, new BlockPos(9, 2, 8));
        warden.doHurtTarget(target);
        assertClose(h, BossWarden.scaledMeleeDamage(h.getLevel().getDifficulty()),
                warden.getAttributeBaseValue(Attributes.ATTACK_DAMAGE), "melee damage at this difficulty");
        warden.discard();
        target.discard();
        h.succeed();
    }

    /** A boss Warden's sonic boom deals sonicBoomDamage instead of vanilla's 10. */
    @GameTest(templateNamespace = NS, template = FLOOR)
    public static void bossSonicBoomDamage(GameTestHelper h) {
        Warden warden = h.spawn(EntityType.WARDEN, new BlockPos(4, 2, 8));
        CityWardens.mark(warden, Long.MIN_VALUE + 1, 0);
        warden.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 24000L);
        IronGolem victim = h.spawn(EntityType.IRON_GOLEM, new BlockPos(10, 2, 8));
        victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000.0);
        victim.setHealth(10000.0F);
        victim.hurt(h.getLevel().damageSources().sonicBoom(warden), 10.0F);
        assertClose(h, 10000.0 - Config.SONIC_BOOM_DAMAGE.get(), victim.getHealth(), "health after a boss boom");
        warden.discard();
        victim.discard();
        h.succeed();
    }

    // ----- F3 forced burrow on dimension change -----

    /** A City Warden about to change dimension stays and burrows instead (burrowOnTeleport). */
    @GameTest(templateNamespace = NS, template = FLOOR)
    public static void dimensionChangeBurrowsInstead(GameTestHelper h) {
        TrackedCity city = newCity(h, area(h));
        Warden warden = h.spawn(EntityType.WARDEN, new BlockPos(8, 2, 8));
        warden.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 24000L);
        CityWardens.mark(warden, city.getId(), 0);
        EntityTravelToDimensionEvent event = NeoForge.EVENT_BUS.post(new EntityTravelToDimensionEvent(warden, Level.NETHER));
        boolean burrows = Config.BURROW_ON_TELEPORT.get();
        h.assertTrue(event.isCanceled() == burrows, "dimension change cancelled: " + event.isCanceled() + ", burrowOnTeleport: " + burrows);
        h.assertTrue(warden.getBrain().getMemory(MemoryModuleType.DIG_COOLDOWN).isPresent() != burrows, "forced burrow state doesn't match burrowOnTeleport");
        warden.discard();
        h.succeed();
    }

    // ----- F5 revival -----

    /** The mound revives Warden_1: a fresh full-health City Warden emerges on top of the mound, shriekers switch back on. */
    @GameTest(templateNamespace = NS, template = FLOOR)
    public static void moundRevivesTheCity(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos shriekerRel = new BlockPos(2, 2, 2);
        TrackedCity city = newCity(h, area(h), shriekerRel);
        defeat(city);
        h.setBlock(shriekerRel, Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, false));
        BlockPos centreRel = new BlockPos(8, 2, 8);
        h.setBlock(centreRel, WonBlocks.HEART_MOUND.get().defaultBlockState().setValue(HeartMoundBlock.LAYERS, 8));
        BlockPos centre = h.absolutePos(centreRel);

        HeartMound.revive(level, centre, city.getId());
        WardenSlot slot = city.getSlots().get(0);
        h.assertTrue(slot.getState() == SlotState.OUT, "Warden_1 not revived");
        h.assertFalse(city.isDefeated(), "city still defeated after revival");
        Warden warden = outWarden(h, slot);
        h.assertTrue(CityWardens.isCityWarden(warden), "revived Warden isn't a City Warden");
        assertClose(h, warden.getMaxHealth(), warden.getHealth(), "revived health");
        assertClose(h, centre.getY() + 1.0, warden.getY(), "revived Warden's height (on top of the mound)");
        h.assertTrue(warden.hasPose(Pose.EMERGING), "revived Warden isn't emerging");
        h.assertTrue(h.getBlockState(shriekerRel).getValue(SculkShriekerBlock.CAN_SUMMON), "shriekers didn't switch back on");
        warden.discard();
        h.succeed();
    }

    // ----- F5 Heart of the Warden -----

    /** Pulses advance the Heart Dead -> Faint -> Alive; an Alive Heart on sculk in a defeated city becomes the mound. */
    @GameTest(templateNamespace = NS, template = FLOOR, timeoutTicks = 40)
    public static void heartStagesAndRitual(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        TrackedCity city = newCity(h, area(h));
        defeat(city);
        BlockPos rel = new BlockPos(8, 2, 8);
        h.setBlock(rel.below(), Blocks.SCULK);
        h.setBlock(rel, WonBlocks.HEART.get());
        HeartBlockEntity heart = h.getBlockEntity(rel);

        for (int i = 0; i < Config.HEART_TRIGGERS_TO_FAINT.get(); i++) {
            h.assertTrue(heart.getStage() == HeartStage.DEAD, "turned Faint too early, at trigger " + i);
            heart.onPulseAbsorbed(level);
        }
        h.assertTrue(heart.getStage() == HeartStage.FAINT, "not Faint after heartTriggersToFaint");
        for (int i = 0; i < Config.HEART_TRIGGERS_TO_ALIVE.get(); i++) {
            heart.onPulseAbsorbed(level);
        }
        h.assertTrue(heart.getStage() == HeartStage.ALIVE, "not Alive after heartTriggersToAlive");

        heart.onPulseAbsorbed(level); // an Alive Heart hit by a pulse runs the ritual
        h.assertTrue(h.getBlockState(rel).is(WonBlocks.HEART_MOUND.get()), "ritual didn't build the mound");
        h.assertTrue(h.getBlockState(rel).getValue(HeartMoundBlock.LAYERS) == 8, "centre column isn't 8 layers");
        h.assertTrue(h.getBlockState(rel.offset(2, 0, 0)).getValue(HeartMoundBlock.LAYERS) == 3, "outer ring isn't 3 layers");
        h.assertTrue(h.getBlockState(rel.offset(1, 0, 1)).getValue(HeartMoundBlock.LAYERS) == 5, "middle corner isn't 5 layers");
        h.assertTrue(h.getBlockState(rel.offset(1, 0, 0)).getValue(HeartMoundBlock.LAYERS) == 6, "middle edge isn't 6 layers");

        // Let the controller force its chunks on its first tick, then release everything.
        h.runAfterDelay(2, () -> {
            BlockPos abs = h.absolutePos(rel);
            h.assertTrue(level.getForcedChunks().contains(ChunkPos.asLong(abs)), "mound didn't force-load its chunk");
            long[] columns = new long[25];
            int n = 0;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    columns[n++] = abs.offset(dx, 0, dz).asLong();
                }
            }
            HeartMound.removeAll(level, columns);
            h.succeed();
        });
    }

    /** A sculk charge reaching a placed Heart is absorbed (no sculk spread) and counts as one trigger. */
    @GameTest(templateNamespace = NS, template = FLOOR, timeoutTicks = 40)
    public static void pulseIsAbsorbedByHeart(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos rel = new BlockPos(8, 2, 8);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                h.setBlock(rel.offset(dx, -1, dz), Blocks.SCULK);
            }
        }
        h.setBlock(rel, WonBlocks.HEART.get().defaultBlockState().setValue(HeartOfTheWardenBlock.STAGE, HeartStage.DEAD));

        // The Heart joins the index on its first tick.
        h.runAfterDelay(2, () -> {
            BlockPos abs = h.absolutePos(rel);
            h.assertTrue(abs.equals(HeartIndex.nearest(level, abs, 1.0)), "Heart isn't indexed");
            SculkSpreader spreader = SculkSpreader.createLevelSpreader();
            spreader.addCursors(abs.below(), 10);
            spreader.updateCursors(level, abs.below(), level.getRandom(), true);
            HeartBlockEntity heart = h.getBlockEntity(rel);
            int progress = heart.toItemStack().get(WonComponents.HEART_DATA.get()).progress();
            h.assertTrue(progress == 1, "expected 1 absorbed pulse, got " + progress);
            h.assertTrue(spreader.getCursors().isEmpty(), "the absorbed charge wasn't discharged");
            h.succeed();
        });
    }
}
