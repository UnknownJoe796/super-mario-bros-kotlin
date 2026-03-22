# Super Mario Bros - Kotlin Translation

A complete, accurate translation of Super Mario Bros from 6502 assembly into readable, modifiable Kotlin. Every line of the original disassembly is preserved as comments alongside its Kotlin equivalent.

The game is fully playable and verified against the original ROM via TAS replay across 103,000+ frames (zero divergences on two TAS scenarios; 12 remaining on the full warpless playthrough — see [Known Inaccuracies](#known-inaccuracies)).

## Running

```
./gradlew run
```

Controls: Arrow keys = D-pad, X = A, Z = B, Enter = Start, Shift = Select

Requires a JDK and an `smb.nes` ROM file in the project root for shadow validation (optional; runs without it).

## Code Sample

Every function is a direct translation of the original assembly, with the source preserved as `//>` comments:

```kotlin
fun System.flagpoleRoutine() {
    //> FlagpoleRoutine:
    //> ldx #$05                  ;set enemy object offset to special use slot
    //> stx ObjectOffset
    val x = 5
    ram.objectOffset = x.toByte()

    //> lda Enemy_ID,x
    //> cmp #FlagpoleFlagObject   ;if flagpole flag not found,
    //> bne ExitFlagP             ;branch to leave
    if (ram.enemyID[x] != EnemyId.FlagpoleFlagObject.byte) return

    //> lda GameEngineSubroutine
    //> cmp #$04                  ;if flagpole slide routine not running,
    //> bne SkipScore             ;branch to near the end of code
    if (ram.gameEngineSubroutine != GameEngineRoutine.FlagpoleSlide) {
        flagpoleGfx(x)
        return
    }

    //> lda Player_State
    //> cmp #$03                  ;if player state not climbing,
    //> bne SkipScore             ;branch to near the end of code
    if (ram.playerState != PlayerState.Climbing) {
        flagpoleGfx(x)
        return
    }
    // ...
}
```

Raw bytes and magic numbers from the original are replaced with typed enums (`EnemyId`, `PlayerState`, `GameEngineRoutine`, `AreaType`, `Direction`, etc.) and named constants (`MetatileId`) while preserving identical behavior.

## Architecture

- **`System`** - Top-level container holding `GameRam`, `PictureProcessingUnit`, `AudioProcessingUnit`, and `Inputs`
- **`GameRam`** - All game state as typed Kotlin properties backed by a flat byte array matching NES RAM layout
- **Game logic** - Extension functions on `System`, one file per major subsystem (~70k lines of Kotlin across 80+ files)
- **`nes/`** - PPU renderer, APU audio synthesis, input handling
- **`interpreter/`** - 6502 binary interpreter used for validation, not gameplay

## Verification

The translation was validated at multiple levels:

| Level | Scope | Result |
|-------|-------|--------|
| Tier 1 | 29 leaf subroutines compared against interpreter | All passing |
| Tier 2 | 17 composite subroutines | All passing |
| Tier 3 | 12 frame-level scenarios | Zero diffs |
| TAS: happylee-warps | 17,859 frames, warp route to W8-4 victory | **0 divergent frames** |
| TAS: happylee-warpless | 67,108 frames, full W1-1 through W8-4 | **12 divergent frames** (0.018%) |
| TAS: smb-0-full-playthrough | 18,307 frames | **0 divergent frames** |

Each TAS frame starts from FCEUX-synced state, so divergences are independent per-frame issues — they do not cascade.

The `GameRamMapper` provides bidirectional mapping between Kotlin properties and flat NES RAM for automated comparison testing. A `ShadowValidator` can run alongside gameplay to catch divergences in real time.

## Project Status

- **Phase 1 - Readable and Running**: Complete
  - All game logic translated (0 TODO stubs remaining)
  - PPU rendering, APU audio synthesis, joypad input
  - Frame-perfect TAS validation
- **Phase 2 - Organized**: In progress
  - Replacing magic numbers with typed enums and value classes
  - Boolean conversions for flag fields
  - Named constants for metatile IDs
- **Phase 3 - Un-NES**: Future
  - Remove NES-specific complications (PPU update juggling, sprite limits, etc.)

## Purpose

- Create an accurate, readable reference implementation of SMB's game logic
- Enable deep modding beyond NES hardware limitations
- Preserve frame-perfect physics for speedrunning accuracy
- Study the relationship between 6502 assembly and modern programming languages

## Mod Ideas

- **Super Mario Bros 4K** - Render entire levels on a single 4K display without scrolling
- **SMB1+2J Flow** - Run through both games without interruption
- **Procedural levels** - Generate new levels using techniques impossible on the NES
- **Speedrun multiplayer** - Players race through levels; fall behind and you lose

## Known Inaccuracies

The 12 remaining warpless divergences fall into two categories:

**NES timing artifacts (7 frames):** The NES CPU has limited cycles per frame. When the NMI handler runs long (e.g. uploading a large VRAM buffer), the game logic may not execute at all — a "partial lag frame" where only the NMI preamble runs. Kotlin has no cycle budget and always runs game logic. These frames produce correct game logic output; the NES simply skipped it. Confirmed by running the same input state through the 6502 interpreter, which matches Kotlin's output.

**Cycle-level edge cases (5 frames):** A handful of single-frame divergences in enemy positions, collision bits, or palette offsets where the NES behavior depends on cycle-level timing that cannot be reproduced without a cycle-accurate CPU emulator. These affect individual bytes for one frame each and self-correct on the next frame.

None of these inaccuracies affect gameplay — they involve rendering timing (area parser task scheduling), cosmetic state (color rotation offset), or transient enemy state that resets within a frame.

## Design Decisions

- NES PPU limitations are not emulated; graphics update directly. This eliminates original visual glitches and enables rendering beyond the NES's capabilities.
- The original RAM layout is preserved in `GameRam` for validation compatibility, with typed property accessors layered on top.
- Assembly comments use the `//> ` prefix to distinguish them from regular Kotlin comments.
