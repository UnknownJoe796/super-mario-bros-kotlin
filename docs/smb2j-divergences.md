# SMB2J TAS Divergence Tracker

## Summary

| Scenario | Divergent Frames | Compared Frames | Rate |
|---|---|---|---|
| smb2j-warps-mario | 0 | ~29,138 | 0% |
| smb2j-warps-luigi | 0 | ~29,675 | 0% |
| smb2j-allitems-mario | 0 | ~84,927 | 0% |

SMB1 TAS: 0 divergent frames (warps, smb-0); 5 on warpless (all NES lag frame artifacts).

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

## Category 1: Block Buffer USED_BLOCK (kotlin=0x00, fceux=0x20) — RESOLVED

**Root cause**: `handleEToBGCollision()` in `enemyBGCollision.kt` unconditionally cleared the
block buffer when an enemy stood on USED_BLOCK. But **SMB2J omits the clearing instructions**
that SMB1 has.

**SMB1** (smbdism.asm:12439-12443):
```asm
cmp #$23              ; USED_BLOCK for SMB1
bne LandEnemyProperly
ldy $02               ; get vertical offset
lda #$00              ; zero
sta ($06),y           ; CLEAR block buffer position
lda Enemy_ID,x
```

**SMB2J** (sm2main.asm:11482-11484):
```asm
cmp #$20              ; USED_BLOCK for SMB2J
bne LandEnemyProperly
lda Enemy_ID,x        ; NO CLEARING — goes straight to enemy ID check
```

The three instructions `ldy $02; lda #$00; sta ($06),y` are present in SMB1 but entirely
absent in SMB2J. The Kotlin code cleared unconditionally for both variants.

**Fix**: Wrapped the block buffer clearing in `if (variant != GameVariant.SMB2J)`.
File: `src/main/kotlin/enemyBGCollision.kt`, `handleEToBGCollision()`.

**Impact**: Eliminated all 10 Category 1 divergent frames across 3 scenarios
(warps-mario -2, warps-luigi -3, allitems -5).

Note: Category 2 (fireball bounce) was originally hypothesized as downstream of Category 1,
but the fireball divergences remain after this fix. They are independent bugs.

### Previous Occurrences (now resolved)

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

---

## Category 2: Fireball Bounce Failure — RESOLVED

**Root cause**: Block buffer overflow for `blockBuffer2`. When a fireball is near the bottom
of the screen (Y ~0xE8), `BlockBufferChk_FBall` computes `vertOffset = 0xD0`, which overflows
the 0xD0-byte buffer. On NES, `lda ($06),y` reads past blockBuffer2 ($05D0-$069F) into the
MetatileBuffer ($06A1-$06AD), which contains terrain metatile IDs from the area parser —
typically ground/floor tiles (solid). The fireball detects these as solid and bounces.
Kotlin returned 0 for buffer 2 overflows, so the fireball saw "nothing" and fell through.

**Fix**: Added `readNesRamAt06A0()` to `GameRam.kt` that replicates the NES memory layout
after blockBuffer2. Updated `blockBufferCollision()` and `blockBufferCollisionEnemy()` to
call it for buffer 2 overflows instead of returning 0.

### Previous Occurrences (now resolved)

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

### Sub-category 4b: Enemy X position differences (VineObject) — RESOLVED

**Root cause**: `Setup_Vine` reads `Block_X_Position,Y` where Y=$60 (from JumpEngine dispatch
for VineObject ID $2F). This reads NES address $8F + $60 = $EF, which is zero-page scratch
RAM. On the NES, $EF is written by `EnemyGfxHandler` (`sta $ef` at sm2main.asm:12739) with
the enemy graphics code for the last-processed enemy. The Kotlin `enemyGfxHandler()` used a
local variable (`enemyCode`) instead of writing to `zeroPageScratch[4]`, so $EF was stale.

**Fix 1**: Added `ram.zeroPageScratch[4] = enemyCode.toByte()` at both NES $EF write points
in `enemyGfxHandler.kt` (lines ~252 and ~278, corresponding to `sta $ef` and Bowser `sty $ef`).

**Fix 2**: Added zero-page wrapping for `setupVine()` address calculations. NES zero-page
indexed addressing wraps at $FF: `(base + Y) & 0xFF`. The `Block_Y_Position,Y` read at
$D7 + $60 wraps to $37 (not $137 as previously computed).

| Scenario | Frame | World | Address | Before | After |
|---|---|---|---|---|---|
| warps-mario | 3589 | W1-3 | $0088 (enemy 1 X) | Kotlin=0x02, FCEUX=0x0D | Match |
| warps-luigi | 6978 | W4-1 | $008A (enemy 3 X) | Kotlin=0x0D, FCEUX=0x06 | Match |

### Sub-category 4c: Player X position (flagpole/climbing) — RESOLVED

**Root cause**: `putPlayerOnVine()` (collisionDetection.kt) reads `ClimbXPosAdder-1,y`
where y = PlayerFacingDir. The NES `-1` indexing means out-of-range facingDir values
(0 or 3) read adjacent ROM bytes:

- **facingDir=3** (Direction.Both): NES reads `ClimbPLocAdder[0]` = `$FF`. Kotlin used
  `coerceIn` to clamp index 3 to 2, reading `$07` instead.
- **facingDir=0** (Direction.None): NES reads the high byte of the preceding
  `jmp RemoveCoin_Axe`. This differs by variant: SMB1 = `$8A`, SMB2J = `$69`.

Secondary fix: the `lda $06; bne ExPVne` check uses the full NES `$06` value (column for
buffer 1, column + $D0 for buffer 2), but Kotlin only used the column.

**Fix**: Extended `climbXPosAdder` and `climbPLocAdder` arrays with ROM overflow/underflow
bytes; made `climbXPosAdder` variant-aware; fixed `putPlayerOnVine` to reconstruct NES `$06`.

| Scenario | Frame | World | Addresses | Kotlin→Fixed | FCEUX | Delta |
|---|---|---|---|---|---|---|
| warps-mario | 22374 | W8-2 | $0086 + $0755 | 0xF7→0xEF | 0xEF | 0 |
| warps-mario | 22786 | W8-2 | $0086 + $0755 | 0xCA→0xA9 | 0xA9 | 0 |

### Sub-category 4d: Enemy Y speed/position (Bloober BlooperMoveCounter) — RESOLVED

**Root cause**: `ProcSwimmingB` (sm2main.asm:8573) uses `adc #$10` to compare the bloober's
Y position against the player's. The `adc` includes the 6502 carry flag, which is inherited
from the `MoveBloober` call chain — NOT cleared by any instruction between `BlooberSwim` and
`ChkNearPlayer`. The Kotlin code used a fixed `+ 0x10`, ignoring the carry.

The carry at `adc #$10` depends on the path through `MoveBloober`:
- **LSFR bypass** (direct to `BlooberSwim`): C=0 (from JumpEngine ASL of Bloober ID $07)
- **Odd enemy slot** (through `txa; lsr`): C=1 (bit 0 of odd x)
- **Even slot direction-set** (through `PlayerEnemyDiff`): C = result of `sbc Player_PageLoc`

When C=1, the NES effectively adds $11 instead of $10. This 1-pixel difference can flip
the `bcc Floatdown` comparison, determining whether `BlooperMoveCounter` gets zeroed.

**Fix**: Added `nesCarry` parameter to `procSwimmingB()`, computed from the `moveBloober()`
call chain. The carry is tracked through JumpEngine ASL, LSFR bypass, `txa; lsr`, and
`PlayerEnemyDiff` paths.

| Scenario | Frame | World | Addresses | Before | After |
|---|---|---|---|---|---|
| warps-mario | 26328 | W8-4 | $00A2 | Kotlin=2, FCEUX=0 | Match |
| allitems | 56767 | W6-3 | $00A3, $00D2 | Kotlin=2, FCEUX=0 | Match |
| allitems | 82022 | W8-4 | $00A2 | Kotlin=2, FCEUX=0 | Match |

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

## Current Status

**All SMB2J divergences resolved.** 0 divergent bytes across all 3 scenarios (~143,740 frames).

SMB1 warpless has 5 frames (18 bytes) remaining — all NES lag frame / area parser
phase shift artifacts in a cluster at frames 28155-28167 (W4-3).
