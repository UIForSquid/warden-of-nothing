# Warden of Nothing — implementation report (v0.1.0)

NeoForge 1.21.1, `neo_version=21.1.264`, Java 21. Mod id `wardenofnothing`, package
`com.uiforsquid.wardenofnothing`. Built from the official MDK-1.21.1-ModDevGradle.

Status: **all DECIDED/DELEGATED features built and compiling; full `build` + jar produced; mod-load
(data-gen) smoke test passed.** The live dedicated-server smoke test is pending your Minecraft EULA
acceptance (see end).

## What was built (in build order)

1. **Config** (`config/Config.java`) — NeoForge SERVER config `wardenofnothing-server.toml` in the
   world's `serverconfig` folder (interim Q12). Every §4a setting, grouped, with plain-English
   comments (what it does, default, range, when it takes effect). Added two settings for the melee
   difficulty decision (`meleeDamageEasyMultiplier`, `meleeDamageHardMultiplier`) and split
   `sonicBoomRange` into horizontal/vertical to match vanilla.
2. **City tracking** (`city/`, `data/`, `mixin/StructureStartMixin`, `mixin/LevelChunkMixin`) — cities
   recorded at world generation (bounds, natural shriekers, Warden_1 burrowed/full health), saved
   per-dimension (`CityData` SavedData). Natural shriekers dropped from the record on removal.
3. **Slots & shriekers** (`warden/WardenSummoner`, `mixin/SculkShriekerBlockEntityMixin`,
   `city/CityShriekers`) — a natural shrieker summons the lowest-numbered burrowed slot (remembered
   health + healing); none when none burrowed; defeated cities switch shriekers to `can_summon=false`
   (including on chunk load); respawn routine switches them back.
4. **Boss Warden** (`warden/BossWarden`, `warden/BossBars`, `event/BossWardenEvents`,
   `mixin/SonicBoomMixin`, melee part of `mixin/WardenMixin`) — stats, zombie-style melee difficulty
   scaling (your decision), sonic boom damage/knockback/range/cooldown, and the 124×64 boss-bar
   cylinder (one per Warden).
5. **Burrow & health memory** (`warden/WardenBurrow`, `warden/WardenHealth`, `mixin/DiggingMixin`,
   `mixin/WardenAiMixin`, burrow part of `mixin/WardenMixin`, `event/ForcedBurrowEvents`) — 120 s
   timer, per-slot remembered health, exponential heal curve, and forced burrows (dimension change,
   far teleport/outside range, removal by another mod).
6. **Home range** (`home/`, `event/HomeRangeEvents`) — range box (+124 sides, +124 up, no down
   limit), give up out-of-range targets, return ignoring everything, the outside-target sonic boom
   (with its one exception while returning), the 10-hit counter with 30 s reset, walk-back vs burrow,
   and the 30 s walk-back timeout.
7. **Unmovable** (`mixin/EntityPortalMixin`, `event/WardenTickEvents`) — see F9 list below.
8. **Assignment** (`assign/AssignmentManager`, `event/AssignmentEvents`) — the `wardenofnothing.assigned`
   request tag (processed and always removed), the `wardenofnothing.city_warden` label, the limit
   check, slot filling, and the respawn routine into a defeated city.
9. **Heart of the Warden** (`heart/`, `reg/`, `mixin/SculkChargeCursorMixin`, `mixin/ItemEntityMixin`)
   — the drop, three-stage block + item (sniffer-egg placeholder with reversed cracks), heartbeat
   sounds, catalyst pulses steered in and absorbed, hidden progress on the item, hand-only breaking,
   the indestructible item, the ritual, the 5×5 snow-layer mound (heart's level, blocked columns
   skipped), chunk force-loading, the 10-minute revival (emerge + server-wide roar), and the 1-minute
   fade.

Not built, as instructed: F4 Phases, F6 city attack (F2 "no-slot-burrowed" path simply summons
nothing).

## VERIFY items (vanilla 1.21.1, read from decompiled sources)

| Item | Found in vanilla | Matches doc? |
|---|---|---|
| Max health | 500 | ✓ (boss 1500) |
| Movement speed | 0.3 | ✓ (×1.5 = 0.45) |
| Attack knockback | 1.5 | ✓ (×3 = 4.5) |
| Attack damage | 30, flat `ATTACK_DAMAGE` attribute | value ✓; **difficulty scaling: NO — see below** |
| Armor / knockback resistance | 0 / 1.0 | ✓ |
| Sonic boom damage | hardcoded **10.0F** (not an attribute), **not** difficulty-scaled | boss = flat 30 |
| Sonic boom knockback | hardcoded 2.5 horizontal / 0.5 vertical × (1−resistance) | ×3 via config |
| Sonic boom range | 15 horizontal / 20 vertical | ✓ |
| Sonic boom charge / duration / cooldown | 34 / 60 / 40 ticks | ✓ |
| Melee cooldown | 18 ticks | ✓ |
| Follow range | 16 (Mob default; Warden doesn't override) | ✓ |
| Vibration listener range | 16 | ✓ |
| Sniff/detection sensor range | 24 (WardenEntitySensor) | ✓ |
| Anger per vibration / touch | 35 | ✓ |
| Anger per projectile | 10 | ✓ |
| Anger per hit | 100 (ANGRY min 80 + 20) | ✓ |
| Anger threshold (ANGRY) | 80 (AGITATED 40, max 150) | ✓ |
| Anger decay | 1 per anger tick (every 20 ticks) | ✓ |
| Emerge / dig durations | 134 / 100 ticks | ✓ |
| Burrow (dig-cooldown) delay | 1200 ticks = 60 s | ✓ (boss 120 s) |
| Warden invulnerable while digging/emerging | TRUE (`isInvulnerableTo`) | ✓ — forced burrow can't be interrupted |
| `StructureStart.getBoundingBox()` | whole-structure (all pieces) box | ✓ |
| Sculk shrieker summon | `SpawnUtil.trySpawnMob(WARDEN, TRIGGERED, …)` at warning level 4 | ✓ (slot logic replaces which/whether) |
| Sniffer egg crack property / hardness | `HATCH` 0/1/2 (0 none … 2 most), strength 0.5 | ✓ (Dead=2, Faint=1, Alive=0) |
| Snow layer property | `LAYERS` 1–8 | ✓ |
| Sounds | `entity.warden.heartbeat`, `entity.warden.roar` | ✓ |

### Contradiction found and raised: melee difficulty scaling
The doc (F1 table, decision 35, the "known consequence" note) assumes vanilla Warden melee is scaled
by difficulty. It is **not** — `Mob.doHurtTarget` uses the flat `ATTACK_DAMAGE` attribute, and the
Warden adds no difficulty modifier. I stopped and asked; you chose **add zombie-style scaling**, so
`meleeDamage` is the Normal value and Easy/Hard multipliers (config) scale it, applied per hit in
`WardenMixin`.

### Decision raised: Heart catalyst trigger
You chose **faithful pulse-steering**: `SculkChargeCursorMixin` steers charge cursors within
`heartPullRange` toward a placed Heart and absorbs them on contact (each = one trigger), keeping
vanilla's charge bookkeeping and only overriding the destination choice. A Heart with no sculk path
to it can't be reached, as specified.

## F9 — Wither / Ender Dragon movement protections (found and copied)
Both `WitherBoss` and `EnderDragon` override **exactly two** movement-related methods in 1.21.1:
- `canRide(Entity)` → `false`
- `canUsePortal(boolean)` → `false`

They override nothing else about being moved (no leash/push/collision overrides); they rely on
defaults. The Warden **already** has `canRide → false` natively, and `Mob.canBeLeashed()` is
`!(this instanceof Enemy)` so a Warden (an `Enemy`) already can't be leashed — matching the two
bosses. The only missing piece was `canUsePortal`, added via `EntityPortalMixin` (config-gated). The
extra DECIDED "no vehicles / dismount if already riding" rule is enforced by `WardenTickEvents` (it
also dismounts a Warden that somehow ends up riding). All gated by `wardensUnmovable`.

## Mixins added (why no event sufficed)
| Mixin | Target | Why a mixin |
|---|---|---|
| `StructureStartMixin` | `StructureStart.placeInChunk` (TAIL) | no structure-generation event; this fires only at gen (the "new cities only" signal) |
| `LevelChunkMixin` | `LevelChunk.setBlockState` (HEAD) | no event covers every block change (mods, pistons, explosions); guarded by a cheap position set |
| `SculkShriekerBlockEntityMixin` | `trySummonWarden` (HEAD) | private method, no hook, to pick which/whether a slot Warden is summoned |
| `SonicBoomMixin` | `checkExtraStartConditions`, `lambda$tick$1`, `stop` (`@ModifyConstant`) | sonic boom range/cooldown are hardcoded constants |
| `WardenMixin` | `doHurtTarget` (HEAD), `finalizeSpawn` (`@ModifyConstant` + emerge `@Redirect`), `increaseAngerAt(Entity)`, `hurt` (`@ModifyArg`) + forced-burrow/clear-anger impl | melee scaling, burrow-delay/emerge constants, anger per vibration/touch/hit, and controlling the private brain/anger for forced burrows (held until digging starts) |
| `WardenAiMixin` | `setDigCooldown` + the dig-cooldown-setter lambda (`@ModifyConstant`), `initFightActivity`, `initEmergeActivity`/`initDiggingActivity` (`@Redirect`) | the 1200-tick dig cooldown (both places vanilla sets it), melee cooldown, emerge/dig durations |
| `WardenVibrationUserMixin` | `Warden$VibrationUser.getListenerRadius`, `onReceiveVibration` | listen range and projectile anger are hardcoded |
| `AngerManagementMixin` | `AngerManagement.tick` (`@ModifyConstant`) | anger decay is a hardcoded `-1` |
| `AngerLevelMixin` | `AngerLevel.byAnger` (HEAD) | the ANGRY threshold is an enum constant |
| `WardenEntitySensorMixin` | `WardenEntitySensor.radiusXZ/radiusY` | sniff range is hardcoded |
| `ServerLevelMixin` | `ServerLevel` (`@Unique` field) | each level holds its own city data, so the per-block-change shrieker check is a field read with no static per-dimension map |
| `RangedAttributeAccessor` | `RangedAttribute.maxValue` (`@Mutable @Accessor`) | vanilla caps max health at 1024 (below the 1500 default), attack damage at 2048 and attack knockback at 5 |
| `DiggingMixin` | `Digging.stop` (HEAD) | vanilla removes the Warden here with no hook; save health first |
| `EntityPortalMixin` | `Entity.canUsePortal` (HEAD, Warden-only) | the method is inherited and not overridden by Warden |
| `SculkChargeCursorMixin` | `ChargeCursor.update` (HEAD + `@Redirect getValidMovementPos`) | faithful pulse steering/absorption needs the private charge-cursor internals |
| `ItemEntityMixin` | `ItemEntity.hurt` (HEAD) | no per-item invulnerability hook; makes the Heart item indestructible (void still deletes) |

## Known caveats / not fully faithful
- **Sonic boom charge time** (`sonicBoomChargeTime`): wired for the mod-driven outside-target boom
  (home range). The *vanilla in-range* boom still uses vanilla 34 ticks, because that value is a
  static-final field access (not a literal) and couldn't be rewired live safely. Default = vanilla.
- **Home range "never paths outside"**: enforced by the per-tick supervisor (give up out-of-range
  targets + pull back + burrow), not by clamping the pathfinder. Observable behaviour matches the doc.
- **Sonic boom knockback ×multiplier**: applied victim-side as an approximate extra push (eye-to-eye
  direction) so the total is ≈ vanilla × multiplier.
- **Attribute ceilings raised globally**: max health, attack damage and attack knockback ceilings are lifted to the
  config's maxima (vanilla clamps max health at 1024, which silently cut the 1500 default). This only lifts the limit.
- **`burrowOnTeleport = false`** stops the dimension-change burrow, but a Warden teleported further than
  `walkBackDistance` outside its home range still burrows through the home-range rule, which can't tell a teleport
  from walking out. Open design question.
- **Mound** is unbreakable in survival; a creative player could still instant-break it (bedrock-like edge).
- Boss bar, home-range and forced-burrow states that are runtime-only are rebuilt on reload (intended).

## To run the live server test
The dedicated-server smoke test needs the Minecraft EULA accepted (your agreement). Create
`run/eula.txt` with `eula=true`, then:
```bash
./gradlew runServer
```
Watch the log reach `Done (…)!` with no mixin/registration errors. (`server.properties` is already
written: flat world, offline mode, for a quick boot.)

## Testing
`./gradlew runGameTestServer` runs 21 GameTests (`gametest/WonGameTests`) headlessly: city recording from a real
Ancient City placement, shrieker removal and chunk-load switching, slot summon/burrow/resummon, the heal curve,
death/defeat/Heart drop, indestructible Heart item, assignment, boss bar range, home-range forced burrow, dimension-change
burrow, unmovable, melee scaling, boss sonic boom damage, Heart stages + ritual + mound, pulse absorption, revival,
and the home-range rules (walk back, timeout, outside-target boom and cooldown, boom charge, return hits) checked
directly from plain values.
The same tests run in a dev client with `/test runall`. The Heart (all three stages) is in the Functional Blocks tab.

## Code layout (where the rules live)
- **City slots**: `data/TrackedCity` owns every slot transition (`sendOut`, `burrow`, `kill`, `claim`) and reports
  what changed (defeated / revived); callers only switch shriekers, drop the Heart or mark the entity.
  `WardenSlot` is read-only outside the `data` package.
- **Natural shrieker index**: private to `data/CityData` (position -> owning city), updated by the same calls that
  update each city's list. Each `ServerLevel` holds its own `CityData` (`ServerLevelMixin`).
- **Per-Warden runtime state** (home-range state, boss bar, forced-burrow hold): `warden/WardenRuntime`, held by
  the Warden entity, so it goes away with the entity; there are no per-Warden static maps to clean up.
- **Home range**: `home/HomeRangeRules` decides from plain values and returns a verdict; `home/HomeRangeManager`
  is the adapter that reads the world and applies it.
