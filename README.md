# Super Mario Bros - Kotlin Translation

A complete, accurate translation of Super Mario Bros and Super Mario Bros 2J (The Lost Levels) from 6502 assembly into readable, modifiable Kotlin. Every line of the original disassembly is preserved as comments alongside its Kotlin equivalent.

Both games are fully playable and verified against the original ROMs via TAS replay across 250,000+ frames — **zero divergences** on all five primary TAS scenarios (see [Verification](#verification)).

## Running

```
./gradlew run                                      # SMB1, Mario
./gradlew run -Dsmb.variant=smb2j                  # SMB2J (The Lost Levels)
./gradlew run -Dsmb.variant=smb2j -Dsmb.character=luigi  # SMB2J, Luigi
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
| SMB1 TAS: happylee-warps | 17,868 frames, warp route to W8-4 victory | **0 divergent frames** |
| SMB1 TAS: happylee-warpless | 67,117 frames, full W1-1 through W8-4 | **5 divergent frames** (all NES lag artifacts) |
| SMB1 TAS: smb-0-full-playthrough | 18,315 frames | **0 divergent frames** |
| SMB2J TAS: warps-mario | ~29,138 frames, warp route to W8-4 victory | **0 divergent frames** |
| SMB2J TAS: warps-luigi | ~29,675 frames, warp route to W8-4 victory | **0 divergent frames** |
| SMB2J TAS: allitems-mario | ~84,927 frames, full playthrough | **0 divergent frames** |

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
- **Phase 3 - SMB2J**: Complete
  - Full Super Mario Bros 2J (The Lost Levels) support
  - Luigi physics, poison mushroom, upside-down piranha, wind mechanic
  - Character select, hard worlds, FDS disk image loading
  - Zero TAS divergences across 3 scenarios (~143,740 frames)
- **Phase 4 - Un-NES**: Future
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

The 5 remaining SMB1 warpless divergences are NES cycle-level timing artifacts. The NES CPU has a limited cycle budget per frame; when the NMI handler runs long (e.g. uploading a large VRAM buffer), execution timing shifts in ways that cannot be reproduced without a cycle-accurate CPU emulator. These affect area parser task scheduling (off by one rendering step) for a single frame each. None affect gameplay.

All real translation bugs have been fixed. SMB2J has zero divergences across all test scenarios.

## Design Decisions

- NES PPU limitations are not emulated; graphics update directly. This eliminates original visual glitches and enables rendering beyond the NES's capabilities.
- The original RAM layout is preserved in `GameRam` for validation compatibility, with typed property accessors layered on top.
- Assembly comments use the `//> ` prefix to distinguish them from regular Kotlin comments.
