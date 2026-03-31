# SMB2J TAS Divergence Tracker

## Summary

| Scenario | Divergent Frames | Compared Frames | Rate |
|---|---|---|---|
| smb2j-warps-mario | 11 | 28,391 | 0.039% |
| smb2j-warps-luigi | 13 | 28,928 | 0.045% |
| smb2j-allitems-mario | 17 | 84,180 | 0.020% |

SMB1 TAS: 0 divergent frames (warps, smb-0); 6 on warpless (all proven NES lag frame artifacts).

## NES Address Reference

| Address | Field | SprObject Slot |
|---|---|---|
| $000E | gameEngineSubroutine | — |
| $000F–$0014 | enemyFlags[0–5] | — |
| $003A–$003B | fireballBouncingFlags[0–1] | — |
| $0046–$004B | enemyMovingDirs[0–5] | — |
| $0086–$009E | sprObjXPos[0–24] | 0=player, 1–6=enemy, 7–8=fireball |
| $009F–$00B7 | sprObjYSpeed[0–24] | same layout |
| $00CE–$00E6 | sprObjYPos[0–24] | same layout |
| $0484 | stompChainCounter | — |
| $0500–$05CF | blockBuffer1[0x00–0xCF] | layout: buf[col + row\*0x10] |
| $05D0–$069F | blockBuffer2[0x00–0xCF] | same layout |
| $0755 | playerPosForScroll | — |

---

## Category 1: Block Buffer USED_BLOCK (kotlin=0x00, fceux=0x20)

**Status**: OPEN — no code bug found, needs 6502 interpreter tracing.

**Impact**: ~2 frames directly (warps-mario 1494, 21795; warps-luigi 1382, 8304, 22165;
allitems 1584, 1714, 53577, 53799, 53815). Causes Category 2 (fireball bounce failures).

### Occurrences

| Scenario | Frame | World | Address | Buffer Position |
|---|---|---|---|---|
| warps-mario | 1494 | W1-1 | $0647 | buf2[0x77] (row 7, col 7) |
| warps-mario | 21795 | W8-2 | $057D | buf1[0x7D] (row 7, col 13) |
| warps-luigi | 1382 | W1-1 | $0533 | buf1[0x33] (row 3, col 3) |
| warps-luigi | 8304 | W4-2 | $060F | buf2[0x3F] (row 3, col 15) |
| warps-luigi | 22165 | W8-2 | $057D | buf1[0x7D] (row 7, col 13) |
| allitems | 1584 | W1-1 | $0532 | buf1[0x32] (row 3, col 2) |
| allitems | 1714 | W1-1 | $0647 | buf2[0x77] (row 7, col 7) |
| allitems | 53577 | W6-1 | $057F | buf1[0x7F] (row 7, col 15) |
| allitems | 53799 | W6-1 | $0572 | buf1[0x72] (row 7, col 2) |
| allitems | 53815 | W6-1 | $0574 | buf1[0x74] (row 7, col 4) |

### Root Cause Analysis

Exhaustive code review found no translation errors. The block buffer address computation
(`blockBufferCollision` / `blockBufferCollisionEnemy`), USED_BLOCK constants, comparison
chains in `chkForNonSolids`, and clearing logic in `handleEToBGCollision` are all faithful
to the NES assembly (verified against both smbdism.asm and sm2main.asm).

The consistent pattern on every affected frame:

1. **Player bumps block** during `gameRoutines()` → `playerCtrlRoutine()` → `playerHeadCollision()`:
   - Writes USED_BLOCK (0x20) directly to block buffer via `sta ($06),y`
   - Writes 4 VRAM buffer entries to `vRAMBuffer1` for the visual update
   - Sets `Block_Metatile[x]`, `Block_State[x]=0x11`, `BlockBounceTimer=0x10`

2. **Enemy walks over same position** during `gameEngine()` → `enemiesAndLoopsCore()` → `handleEToBGCollision()`:
   - `chkUnderEnemy()` reads the just-written USED_BLOCK from the block buffer
   - `chkForNonSolids()` returns false (0x20 is not vine/coin/hidden block)
   - `cmp #$20; bne LandEnemyProperly` matches → enters clearing code
   - Writes 0x00 to the block buffer position

3. **blockObjMTUpdater skips** because `vRAMBuffer1.isNotEmpty()` (4 entries from step 1)

4. **Result**: Kotlin ends with 0x00 (cleared by enemy). FCEUX ends with 0x20.

### Leading Hypothesis

Since the code is verified correct, the remaining hypothesis is that the NES enemy computes
a block buffer index that maps to a **different position** than where the player wrote
USED_BLOCK, due to a frame-specific alignment condition that doesn't manifest in the code
review.

### Next Step

Run 6502 interpreter on frame 1494 to trace exact NES execution:
1. Load frame 1494's FCEUX dump state
2. Step through `EnemyToBGCollisionDet` for each enemy slot
3. Record which enemies reach `ChkUnderEnemy`, what `BlockBufferCollision` computes, and
   whether any actually reach `HandleEToBGCollision` with metatile 0x20
4. Compare the NES computed `bufIndex` values with what Kotlin computes

---

## Category 2: Fireball Bounce Failure (downstream of Category 1)

**Status**: OPEN — would be fixed by fixing Category 1.

**Impact**: ~5-6 frames per scenario. Fireball checks block buffer for a solid metatile to
bounce off. When Category 1 causes USED_BLOCK to be missing (0x00 instead of 0x20), the
fireball falls through instead of bouncing.

| Address | Field | Kotlin | FCEUX | Meaning |
|---|---|---|---|---|
| $003A | fireballBouncingFlags[0] | 0x00 | 0x01 | Not bouncing vs bouncing |
| $003B | fireballBouncingFlags[1] | 0x00 | 0x01 | Same, fireball slot 1 |
| $00A6 | sprObjYSpeed[7] (fb0) | 0x03 | 0xFD | Falling vs bouncing upward |
| $00A7 | sprObjYSpeed[8] (fb1) | 0x03 | 0xFD | Same |
| $00D5 | sprObjYPos[7] (fb0) | varies | 0xE8 | Y position diverges |
| $00D6 | sprObjYPos[8] (fb1) | varies | 0xE8 | Same |

### Occurrences

| Scenario | Frame | World | Addresses |
|---|---|---|---|
| warps-mario | 18772 | W5-3 | $003B, $00A7, $00D6 |
| warps-mario | 20736 | W8-1 | $003B, $00A7, $00D6 |
| warps-mario | 22823 | W8-2 | $003A, $00A6, $00D5 |
| warps-mario | 22954 | W8-2 | $003B, $00A7, $00D6 |
| warps-mario | 22974 | W8-2 | $003B, $00A7, $00D6 |
| warps-luigi | 19111 | W5-3 | $003B, $00A7, $00D6 |
| warps-luigi | 21093 | W8-1 | $003A, $00A6, $00D5 |
| warps-luigi | 23188 | W8-2 | $003A, $00A6, $00D5 |
| warps-luigi | 23270 | W8-2 | $003A, $00A6, $00D5 |
| allitems | 50430 | W5-4 | $003B, $00A7 |
| allitems | 68001 | W7-2 | $003B, $00A7, $00D6 |

---

## Category 3: Collision Cascade — RESOLVED

**Root cause**: `checkPlayerVertical()` (collisionDetection.kt:261) used SMB1 assembly logic
for SMB2J. The two variants have completely different implementations:

**SMB1** (smbdism.asm:12708): `cmp #$f0; bcs` → check `Player_Y_HighPos` → `cmp #$d0`
**SMB2J** (sm2main.asm:10901): `and #$f0; clc; beq ExCPV; sec`

SMB1 returns "offscreen" when `yPos >= 0xD0` (player near bottom of screen). SMB2J only
checks if the upper nibble of `Player_OffscreenBits` is non-zero. When the player is near
the bottom of the screen (Y >= 0xD0) stomping enemies, the SMB1 logic incorrectly skipped
the collision check, preventing stomps that the NES SMB2J would register.

**Fix**: Added variant dispatch — SMB2J returns `(offscreenBits & 0xF0) != 0`.

**Secondary fix**: `injurePlayer()` (collisionDetection.kt:1325) — SMB2J checks both
`InjuryTimer` and `StarInvincibleTimer` via `ora` (sm2main.asm:10424), but Kotlin only
checked `InjuryTimer`. Fixed with variant dispatch.

### Previous Occurrences (now resolved)

| Scenario | Frame | World | Divergence Count |
|---|---|---|---|
| warps-mario | 1940 | W1-1 | 8 |
| warps-luigi | 2012 | W1-1 | 10 |

The allitems frame 19886 (W2-3) may still appear — to be verified.

---

## Category 4: Single-Pixel Position Differences

### Sub-category 4a: sprObjYPos[20] off-by-one — RESOLVED

**Root cause**: `setupJumpCoin()` (enemyBGCollision.kt:1224) missing carry from 4 ASL
operations on NES zero-page `$06`. The NES `adc #$20` carries from the ASLs: when the block
is in buffer 2, `$06 = 0xD0 + column`, bit 4 is 1, and 4 ASLs shift it into carry. The
effective add becomes `0x21` for buffer 2, but Kotlin always added `0x20`.

SprObject index 20 = misc slot 7 (13 + 7 = 20), used for jumping coins. The coin spawns
with Y = `vertOffset + 0x20 + carry`, where carry = 1 for buffer 2 blocks.

**Fix**: Reconstruct NES `$06` value, compute carry from bit 4 after 4 ASLs.

**Secondary finding** (not fixed): `coinBlock()` at line 1216 has an untracked carry from
`FindEmptyMiscSlot`. When misc slot 8 is the first empty slot, carry=0 from JumpEngine,
making the NES compute `blockY - 0x11` while Kotlin computes `blockY - 0x10`. This produces
a +1 divergence (opposite direction). Not yet observed in TAS.

### Sub-category 4b: Enemy X position differences

**Status**: OPEN — not conclusively identified.

Single-frame enemy X position differences with large deltas (7-11 pixels).

| Scenario | Frame | World | Address | Kotlin | FCEUX | Delta |
|---|---|---|---|---|---|---|
| warps-mario | 3589 | W1-3 | $0088 (enemy 1 X) | 0x02 | 0x0D | -11 |
| warps-luigi | 6978 | W4-1 | $008A (enemy 3 X) | 0x0D | 0x06 | +7 |

**Partial fix applied**: `offscreenBoundsCheck()` (processCannons.kt:262) used
`PiranhaPlant.id` ($0D) as the carry threshold for SMB2J, but SMB2J's last `cpy` before
`ExtendLB` is `#UpsideDownPiranhaP` ($04). Fixed to use variant-appropriate value. This
corrects a 1-pixel offscreen boundary but doesn't explain the 7-11px deltas.

### Sub-category 4c: Player X position (flagpole/climbing)

**Status**: OPEN — root cause not yet identified.

| Scenario | Frame | World | Addresses | Kotlin | FCEUX | Delta |
|---|---|---|---|---|---|---|
| warps-mario | 22374 | W8-2 | $0086 + $0755 | 0xF7/0x53 | 0xEF/0x4B | +8 |
| warps-mario | 22786 | W8-2 | $0086 + $0755 | 0xCA | 0xA9 | +0x21 (33) |

Both `playerXPosition` and `playerPosForScroll` diverge together. Frame 22786 has the
larger delta of 0x21 (33 pixels). Both frames are in W8-2 near the flagpole/end of level.

**Investigated and ruled out**: The first-half side check in `doPlayerSideCheck()` was
suspected — when `CheckForClimbMTiles` returns climbable, the agent proposed calling
`handleClimbing`. However, the NES assembly (smbdism.asm:12048, sm2main.asm:11079) shows
that climbable falls through to BHalf (not to HandleClimbing). HandleClimbing is only called
from `CheckSideMTiles` (the second `CheckForClimbMTiles` call). The proposed fix was reverted
after it introduced SMB1 regressions.

### Sub-category 4d: Enemy Y speed/position

**Status**: OPEN — likely NES memory aliasing, needs FCEUX dump enemy type.

| Scenario | Frame | World | Addresses | Pattern |
|---|---|---|---|---|
| allitems | 56767 | W6-3 | $00A3 (sprObjYSpeed[4]), $00D2 (sprObjYPos[4]) | speed 2 vs 0, pos off by 1 |

Both addresses are for the same enemy (slot 3, objectOffset=3). Kotlin=0x91/speed=2 (still
falling), FCEUX=0x90/speed=0 (stopped). The FCEUX position 0x90 is NOT the result of
`enemyLanding()` snap (which would give 0x98), so something other than the standard landing
path zeroed the Y speed on NES — likely an alias variable (PiranhaPlant_MoveFlag,
BlooperMoveCounter, or LakituMoveDirection sharing the $A0+x address).

---

## Category 5: Mixed/Other — RESOLVED

### Frame 18440 (warps-mario, W5-3): area parser phase shift

All 12 divergences are area parser phase shift artifacts. FCEUX processed 2 area parser
tasks while Kotlin processed 1, creating a 1-task offset in block buffer metatiles
(left vs right pipe column), attribute buffer, and area parser control state.

**Fix**: Improved `isAreaParserPhaseShift2` detection — uses wide address set (including
block buffers $0500-$069F) when attribute buffer diffs ($03F9-$03FF) are present. Added
$0730-$0732 (areaObjectLength) to narrow late-engine address set.

---

## Lag Frame Detection

The test detects and skips several categories of FCEUX dump artifacts:

1. **NES lag frames**: Frame counter didn't increment (NMI ran but game engine was still
   executing from the previous frame).
2. **VRAM overflow**: NMI ran but VRAM buffer was too full to process completely.
3. **Loading/transition frames**: OperMode != Game or OperModeTask < game engine task.
4. **Partial NMI lag** (`isPartialNmiLag2`): Frame counter incremented but impossible state
   detected (e.g., `areaParserTaskNum=8`, impossible `colorRotateOffset`). Uses a wide
   address set that includes block buffer ranges ($0500-$069F) for co-occurring diffs.
5. **Area parser phase-shift** (`isAreaParserPhaseShift2`): All diffs are in the late-game-engine
   address set. Uses the wide set (with block buffers) when attribute buffer diffs are present
   as a discriminator against Category 1 bugs.

---

## Priority for Fixing

1. **Category 1** (block buffer USED_BLOCK): Highest impact. Fixing would also fix Category 2,
   eliminating ~16-18 frames total. Requires 6502 interpreter tracing.

2. **Category 4c** (player X + scroll): 2 frames in warps-mario. Root cause unknown —
   related to flagpole/climbing area in W8-2. The `doPlayerSideCheck` first-half hypothesis
   was ruled out.

3. **Category 4b** (enemy X 7-11px): 2 frames. Large deltas suggest a significant logic
   difference, not just off-by-one. May be related to Category 1 block buffer differences
   affecting enemy collision behavior.

4. **Category 4d** (enemy Y speed aliasing): 1 frame. Needs FCEUX dump enemy type to confirm.
