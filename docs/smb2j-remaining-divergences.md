# SMB2J TAS Remaining Divergences

Per-frame FCEUX comparison status as of 2026-03-29.

## Test Scenarios
- **smb2j-warps-mario**: 60/28,391 frames (0.2%), 133 divergent bytes, 0 NMI errors
- **smb2j-warps-luigi**: 56/28,928 frames (0.2%), 111 divergent bytes, 0 NMI errors
- **smb2j-allitems-mario**: 569/84,180 frames (0.7%), 718 divergent bytes, 0 NMI errors

## Category 1: Sound Engine State ($07B0/$07C1) — 15-35 frames per scenario

**Addresses**: `$07B0` (musicOffsetNoise/areaMusicBuffer_Alt), `$07C1` (noiseDataLoopbackOfs)

**Pattern**: Kotlin=$A4, FCEUX=$DB. Occurs every ~2000 frames across all scenarios.

**Root cause**: The sound engine's noise channel loopback offset diverges. Both values
are valid — they represent different positions in the noise beat data cycle. The difference
is likely from a single noise beat frame where Kotlin and FCEUX process the loopback at
slightly different timing, then the offset stays diverged until the next music load.

**Impact**: Cosmetic only. Affects noise percussion timing by a few beats.

**Fix direction**: Check the noise channel's `endOfMusicData` loopback path for
subtle differences in how the loopback offset is initialized or when the terminator
byte ($00) is detected.

## Category 2: Block Buffer Metatile Off-by-One — scattered, 2-5 frames each

**Addresses**: Various block buffer locations ($05xx, $06xx)

**Pattern**: Kotlin writes metatile N, FCEUX writes N+1 (e.g., $63/$64, $64/$65, $66/$67).

**Occurrences**:
- W4-2: 5 divergences in one frame ($0639-$0679), column of metatiles each off by 1
- W1-4 (allitems): $63/$64 in castle wall tiles ($05AE, $05AF, $0670, $0671)
- Scattered single-byte differences in other worlds

**Root cause**: There are still metatile constants that weren't updated for SMB2J's
shifted metatile numbering. The castle wall tile ($63 in SMB1 vs $64 in SMB2J) and
underground brick tiles ($64/$65/$66 vs $65/$66/$67) need variant-aware handling.
These are likely in `AreaStyleTileData` or castle-specific rendering paths.

**Impact**: Visual only — wrong tile graphics rendered, no gameplay effect.

**Fix direction**: Search for remaining hardcoded metatile values $61-$67 in area
parser and rendering code. Compare against sm2main.asm's metatile tables.

## Category 3: Enemy Slot Timing ($0014, $06D7) — 4-5 frames each

**Address $0014**: `enemyID[5]` — Kotlin=01, FCEUX=00 (or vice versa)

**Address $06D7**: `playerGfxOffset` or similar — Kotlin=$06, FCEUX=$FF

**Pattern**: Single-frame differences where an enemy spawns or despawns one frame
early/late compared to FCEUX.

**Root cause**: The area parser's enemy data stream processing has a subtle timing
difference. When the screen scrolls past an enemy spawn point, Kotlin and FCEUX
occasionally disagree by one frame on when the enemy enters the active zone. This
cascades into different enemy slot assignments for ~1 frame.

**Impact**: Minimal gameplay effect — enemy appears 1 frame early/late.

**Fix direction**: Check the horizontal boundary comparison in `processEnemyData`
(the `cmp ScreenRight_X_Pos` check). There may be an off-by-one in the page
location comparison or the X position threshold.

## Category 4: Area Parser Task Phase ($071F/$0721) — 6 frames

**Addresses**: `$071F` (areaParserTaskNum), `$0721` (currentNTAddrHigh),
`$0773` (vRAMBufferAddrCtrl), `$03F9-$03FF` (attributeBuffer)

**Pattern**: Same as the known SMB1 warpless W4-3 divergence — the area parser
runs 1 task out of phase for ~6 frames during a specific scroll transition.

**Root cause**: FCEUX skips `areaParserTaskHandler` on one frame where Kotlin runs
it, causing all 8-task cycle state to be 1 step ahead. Self-corrects after the cycle
completes. Unknown whether this is a lag frame detection issue or a genuine NMI
timing difference.

**Impact**: Cosmetic only — brief 1-tile seam at scroll edge for ~6 frames.

## Category 5: Player Position Edge Cases — 2-4 frames

**Addresses**: `$0086` (player X position), `$0755` (playerPosForScroll),
`$0057` (player X speed)

**Pattern**: Occurs in W4-3 and W8-x. Small position differences (~1-2 pixels)
that appear for 1-2 frames then self-correct.

**Root cause**: Likely related to the jumpspring landing calculation or wind
displacement timing. The position difference is small enough that it doesn't
affect subsequent game state (TAS per-frame sync corrects it).

**Impact**: None in practice — self-correcting within 1-2 frames.

## Category 6: Allitems-Specific ($00D1, $0787, $07EE) — 231+ frames

**Address $00D1**: `sprObjYPos[3]` — 231 frames starting at frame 25686

This is the largest remaining divergence in the allitems scenario, occurring in
the later worlds (W4+). An enemy's Y position diverges and persists, suggesting
a gravity or vertical movement calculation difference for a specific enemy type
in a specific context.

**Address $0787**: `timers[7]` — 52 frames starting at 68918 (W7-x)
**Address $07EE**: `coinDisplay[1]` or `gameTimerDisplay[2]` — 52 frames

These correlate and may be a display update timing issue in late worlds.

**Fix direction**: Trace frame 25686 in the allitems scenario to identify which
enemy type is at slot 3 and what vertical movement path differs.

## Summary

| Category | Frames (warps) | Fixable? |
|----------|---------------|----------|
| Sound engine noise | 15 | Medium — loopback timing |
| Block buffer metatiles | 7 | Easy — more constant updates |
| Enemy spawn timing | 5 | Hard — subtle boundary check |
| Area parser phase | 6 | Unknown — same as SMB1 issue |
| Player position edge | 4 | Hard — cascading position calc |
| Allitems-specific | 231 | Medium — needs tracing |
