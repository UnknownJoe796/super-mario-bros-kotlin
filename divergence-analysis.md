# SMB Translation TAS Replay Divergence Analysis

## Executive Summary

After analyzing 30 divergences across 11 frames in two test suites (HAPPYLEE-WARPS and SMB-0-FULL-PLAYTHROUGH), I've identified **5 distinct bug patterns**. The most critical issue is **systematic bubble object corruption** affecting 8 divergences.

---

## Divergence Patterns

### PATTERN 1: Bubble Object Corruption (8 occurrences) — CRITICAL ⚠️

**Affected Frames:** 7037, 11422, 14362, 16621

**Symptom:** Bubble slots (22-24) are completely inactive in Kotlin while NES has live data.

| Frame | Sprite Slot | Address | Kotlin | NES | Meaning |
|-------|-----------|---------|--------|-----|---------|
| 16621 | BUBBLE[0] | $009C | 0 | 65 | X position missing |
| 16621 | BUBBLE[0] | $00CB | 0 | 1 | Y High missing |
| 16621 | BUBBLE[0] | $00E4 | -1 | -10 | Y position wrong |
| 14362 | BUBBLE[0] | $009C | 0 | 49 | X position missing |
| 14362 | BUBBLE[0] | $00CB | 0 | 1 | Y High missing |
| 14362 | BUBBLE[0] | $00E4 | -1 | -10 | Y position wrong |
| 11422 | BUBBLE[1] | $006E | 6 | 0 | X speed wrong |
| 7037  | BUBBLE[2] | $006F | 4 | 0 | X speed wrong |

**Analysis:**
- Frames 16621 and 14362 are **identical patterns** (W8-4 transitions = water/castle level)
- Bubbles spawn only in water levels (underwater castles, W8)
- Kotlin either never spawns bubbles OR kills them prematurely

**Root Cause Hypotheses (ranked):**
1. `airBubbleTimer` (at `timers[0x12]`) not decrementing → bubble spawn condition never triggered
2. Bubble spawn code disabled or missing from game loop
3. Bubble state handling in `miscObjectsCore.kt` broken
4. Water level detection (`areaType`) incorrect

**Files to Check:**
- `/src/main/kotlin/miscObjectsCore.kt` — check air bubble spawning logic
- `/src/main/kotlin/gameMode.kt` — check if `airBubbleTimer` decrements in game loop
- `/src/main/kotlin/GameRam.kt` — ensure `timers[0x12]` is properly managed

---

### PATTERN 2: Enemy Spawn / Positioning Bug (7 occurrences)

**Affected Frames:** 7037, 11391, 16451, 16523, 11422, 11650

**Sub-Pattern 2A: Wrong Enemy State (Frames 7037, 11422)**

| Frame | Enemy Slot | Address | Kotlin | NES | Meaning |
|-------|-----------|---------|--------|-----|---------|
| 7037 | enemy[4] | $0023 | 1 | 0 | State active/dead mismatch |
| 11422 | enemy[4] | $0023 | 1 | 0 | State active/dead mismatch |

Kotlin thinks enemy[4] is alive (state=1) but NES has it dead (state=0).
Also accompanied by wrong position data in related frames.

**Sub-Pattern 2B: Large Position Differences (Frames 16451, 16523, 11650)**

| Frame | Slot | Address | Kotlin | NES | Type |
|-------|------|---------|--------|-----|------|
| 11650 | block[0] | $008F | 0 | 64 | X position (should be 64) |
| 11650 | block[0] | $00BE | 0 | 1 | Y High (should be 1) |
| 16451 | enemy[3] | $008A | 109 | -27 | X position wrong direction |
| 16523 | enemy[1] | $0088 | 12 | 60 | X position incomplete |

**Analysis:** Enemies/blocks positioned completely wrong or not moving at all.

**Root Cause Hypotheses:**
1. Enemy spawn slot allocation mismatch (ProcessEnemyData writing to wrong array indices)
2. Enemy movement not applied (`moveNormalEnemy` never called or broken)
3. Horizontal movement physics broken (`moveEnemyHorizontally` has math bug)
4. Enemy type-to-slot mapping incorrect

---

### PATTERN 3: Off-by-One Y Position Errors (2 occurrences)

**Affected Frames:** 9014, 9054

| Frame | Enemy Slot | Address | Kotlin | NES | Diff |
|-------|-----------|---------|--------|-----|------|
| 9014 | enemy[0] | $00CF | -98 | -97 | -1 |
| 9054 | enemy[4] | $00D3 | -114 | -113 | -1 |

**Analysis:** Consistent 1-pixel discrepancy in Y position.

**Root Cause Hypotheses:**
1. `moveObjectVertically` decrementing one extra frame
2. `enemyJump` applying gravity on wrong frame
3. Falling platform collision detection off-by-one

---

### PATTERN 4: Unmapped NES Memory ($0F-$17) (4 occurrences)

**Affected Frames:** 11391, 11422

| Frame | Address | Kotlin | NES | Name |
|-------|---------|--------|-----|------|
| 11391 | $000E | 11 | 8 | gameEngineSubroutine (known) |
| 11391 | $0017 | 14 | 0 | enemyID[1] (known) |
| 11422 | $000F | 0 | 1 | **UNMAPPED** |
| 11422 | $0016 | 0 | 47 | **UNMAPPED** |

**NES Memory Layout (addresses $0A-$1B):**
```
$0A: aBButtons
$0B: upDownButtons
$0C: leftRightButtons
$0D: previousABButtons
$0E: gameEngineSubroutine ✓
$0F: ??? (UNMAPPED)
$10-$15: ??? (UNMAPPED)
$16: enemyID[0] ✓
$17: enemyID[1] ✓
$18-$1B: enemyID[2-5] ✓
```

**Analysis:** Addresses $0F and $10-$15 in GameRam.kt need to be mapped. These might be:
- Flagpole completion state variables
- Jump spring / power-up state
- Button input history
- Floatey number state (though that starts at $110)

---

### PATTERN 5: Player Movement Bug (1 occurrence)

**Frame 11391 (W8-2 - Bowser's Castle)**

| Address | Kotlin | NES | Meaning |
|---------|--------|-----|---------|
| $0057 | 0 | 40 | Player X speed (should be 40) |

Player should be moving right at speed 40, but Kotlin has 0.

**Root Cause Hypothesis:** Input reading failure in `readJoypads()` or input processing in frame 11391.

---

## Summary Table: Bugs by File

| Bug | Likely File(s) | Type | Severity |
|-----|-----------------|------|----------|
| Bubble spawning | `miscObjectsCore.kt`, `gameMode.kt` | Game Logic | CRITICAL |
| Enemy spawn/position | `enemiesAndLoopsCore.kt`, `runEnemyObjectsCore.kt` | Game Logic | HIGH |
| Off-by-one Y pos | `movement.kt`, `gravity.kt` | Physics | MEDIUM |
| Unmapped variables | `GameRam.kt` | Data Struct | MEDIUM |
| Player input | `input.kt` or `gameMode.kt` | Input | MEDIUM |

---

## Immediate Action Items

### Priority 1: Map Unmapped Addresses
```kotlin
// In GameRam.kt, add around line 90:
@RamLocation(0x0F) var flag0F: Byte = 0  // TBD
@RamLocation(0x10) var flag10: Byte = 0  // TBD
@RamLocation(0x11) var flag11: Byte = 0  // TBD
@RamLocation(0x12) var airBubbleTimer: Byte = 0  // AIR BUBBLE TIMER
@RamLocation(0x13) var flag13: Byte = 0  // TBD
@RamLocation(0x14) var flag14: Byte = 0  // TBD
@RamLocation(0x15) var flag15: Byte = 0  // TBD
```

### Priority 2: Debug Bubble Spawning
Search for:
- `airBubbleTimer` usage
- `timers[0x12]` decrement
- Bubble state initialization
- Water level check (`areaType`)

### Priority 3: Test with Shadow Validator
Run frame 7037 with breakpoint on `enemyState[5]` change to identify when divergence occurs.

---

## Slot Reference (SprObject array)

```
Index: Object Type
0:     Player
1-6:   Enemies[0-5]
7-8:   Fireballs[0-1]
9-10:  Blocks[0-1]
13-17: Misc objects[0-4] (hammers, coins, etc.)
22-24: BUBBLES[0-2]  ← These are dying in Kotlin
```

---

## Next Steps

1. Add the unmapped addresses to GameRam.kt and re-test
2. Trace bubble spawning with debug logging
3. Review enemy spawn logic in ProcessEnemyData
4. Use shadow validator frame-by-frame on critical divergences
