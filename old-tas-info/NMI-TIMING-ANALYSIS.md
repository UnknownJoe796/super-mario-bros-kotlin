# NMI Timing Analysis for SMB TAS Accuracy

## Root Cause Discovery

The TAS divergence is caused by **NMI handler execution spanning multiple frames** during title screen initialization.

### The Mechanism

1. **NMI handler disables NMI at start** (line 740-742 in smbdism.asm):
   ```asm
   NonMaskableInterrupt:
   LDA Mirror_PPU_CTRL_REG1      ; disable NMIs in mirror reg
   AND #%01111111                ; save all other bits
   STA Mirror_PPU_CTRL_REG1
   AND #%01111110
   STA PPU_CTRL_REG1             ; disable in hardware too
   ```

2. **NMI handler calls game logic** (line 842):
   ```asm
   JSR OperModeExecutionTree     ; runs InitializeGame, ScreenRoutines, etc.
   ```

3. **During initialization, game logic takes many cycles**:
   - `InitializeGame`: Clears $0000-$076F (1903 bytes) → ~10,000 cycles
   - `ScreenRoutines`: Name table setup, VRAM writes → tens of thousands of cycles
   - One NES frame = ~29,780 CPU cycles

4. **If handler takes >1 frame, vblanks are missed**:
   - NMI disabled during handler execution
   - Vblank occurs but NMI doesn't fire
   - Handler eventually finishes and re-enables NMI
   - Next vblank triggers NMI normally

### FCEUX Evidence

| Frame | FC | IntCtrl | Analysis |
|-------|----|---------|---------------------------------|
| 5 | 1→0 | 0→20 | First NMI, InitializeGame clears FC |
| 6 | 0 | 20 | **No NMI** (IntCtrl unchanged) |
| 7 | 0 | 20 | **No NMI** (IntCtrl unchanged) |
| 8 | 0→1 | 20→19 | NMI fires again |
| 9 | 1→2 | 19→18 | Normal NMI |

IntCtrl staying at 20 for frames 5-7 proves no NMI decremented it during frames 6-7.

### Our Interpreter Behavior

| Frame | FC | IntCtrl | Analysis |
|-------|----|---------|---------------------------------|
| 5 | 0→0 | 0→20 | First NMI, InitializeGame clears FC ✓ |
| 6 | 0→1 | 20→19 | **NMI fires** (wrong!) |
| 7 | 1→2 | 19→18 | **NMI fires** (wrong!) |
| 8 | 2→3 | 18→17 | NMI fires |

We fire NMI every frame regardless of handler execution time.

### Impact on RNG

- RNG advances once per NMI (lines 803-815)
- By frame 8: FCEUX has 2 NMIs worth of RNG rotation, we have 4
- Different RNG → different enemy behaviors → TAS inputs become invalid

## Key Variables

| Variable | Address | Cleared by InitializeGame? |
|----------|---------|---------------------------|
| FrameCounter | $0009 | YES (below $076F) |
| IntervalTimerControl | $077F | NO (above $076F) |
| PseudoRandomBitReg | $07A7-$07AD | NO (above $076F) |
| TimerControl | $0787 | NO (above $076F) |

InitializeGame clears $0000-$076F, so FC is reset but timing variables survive.

## Solution Approach

Since the NMI handler duration during initialization is deterministic (same code path every time), we can:

1. **Measure cycles** our interpreter takes during specific NMI handlers
2. **Skip NMIs** when accumulated cycles exceed one frame's worth
3. **Match FCEUX's effective NMI count** at key synchronization points

This approximates cycle-accurate timing without full cycle counting.

## Implementation: Cycle Debt System

We implemented a "cycle debt" system in `FrameCounterBasedTASTest.kt`:

```kotlin
val cyclesPerFrame = 29780  // NES CPU cycles per frame
var cycleDebt = 0           // Cycles consumed beyond current frame
val avgCyclesPerStep = 3.5  // Average cycles per interpreter step

// After NMI handler completes:
val cyclesUsed = (nmiSteps * avgCyclesPerStep).toInt()
cycleDebt = cyclesUsed - cyclesPerFrame

// Before firing next NMI:
if (cycleDebt > 0) {
    // Still paying off cycle debt - skip this NMI
    cycleDebt -= cyclesPerFrame
} else {
    // Debt paid off - fire NMI normally
}
```

### Results with Cycle Debt

| Frame | FC | IntCtrl | Analysis |
|-------|----|---------|---------------------------------|
| 5 | 0→0 | 0→20 | First NMI (24375 steps, debt=55532) |
| 6 | 0→0 | 20→20 | **Skipped** (debt=25752) ✓ |
| 7 | 0→0 | 20→20 | **Skipped** (debt=-4028) ✓ |
| 8 | 0→1 | 20→19 | NMI fires (7184 steps) ✓ |
| 9 | 1→2 | 19→18 | Normal NMI (652 steps) ✓ |

This matches FCEUX behavior!

## PPU Sprite 0 Timing

SMB uses sprite 0 hit detection for split-screen scrolling (status bar vs playfield).
The NMI handler contains two busy-wait loops:

```asm
Sprite0Clr:         ; $813D
    BIT PPU_STATUS  ; Read $2002, V flag = bit 6 (sprite 0 hit)
    BVS Sprite0Clr  ; Loop while sprite 0 hit IS SET

Sprite0Hit:         ; $8149
    BIT PPU_STATUS
    BVC Sprite0Hit  ; Loop while sprite 0 hit IS NOT SET
```

### The Problem

When NMI handlers take multiple frames' worth of cycles (50000+ steps), the PPU
timing must cycle properly or these loops never terminate.

### Solution: Cyclical PPU Timing

```kotlin
val stepsPerFrame = 8509    // ~29780 cycles / 3.5 cycles/step
val vblankEndSteps = 800    // ~2800 cycles
val sprite0HitSteps = 1200  // ~4200 cycles

fun read(addr: Int): UByte {
    when (addr and 0x07) {
        0x02 -> {  // PPU_STATUS
            // Use modular timing - PPU cycles even during long NMI handlers
            val phaseStep = frameSteps % stepsPerFrame

            val sprite0Hit = phaseStep >= sprite0HitSteps
            val inVblank = phaseStep < vblankEndSteps
            // ...
        }
    }
}
```

This ensures sprite 0 clears every ~8509 steps (one frame), preventing infinite loops.

## TAS Accuracy Results

With cycle debt + cyclical PPU timing:

| Milestone | Frame | Status |
|-----------|-------|--------|
| W1-1 start | 0 | ✓ |
| W1-2 (via warp) | 1943 | ✓ |
| W1-3 (via warp) | 2442 | ✓ |
| W4-1 (via warp) | 3765 | ✓ |
| W4-2 | 6042 | ✓ |
| W4-3 | 6541 | ✓ |
| Death | 8282 | TAS expected to survive |

The TAS successfully uses the W1-1 → W4-1 warp but diverges in W4-3, dying where
the TAS expects to make the W4-2 → W8-1 warp.

## Why Full Accuracy Requires Cycle-Accurate Timing

Even with these approximations, the TAS eventually diverges because:

1. **RNG advances once per NMI** - Any difference in NMI count accumulates
2. **RNG affects enemy behavior** - Different RNG → different enemy positions
3. **TAS inputs are frame-perfect** - Even small timing differences break the run
4. **Cycle count varies by code path** - Our 3.5 cycles/step average is an approximation

### The Fundamental Issue

The NES's NMI can interrupt the CPU at any instruction. Without cycle-accurate
emulation, we cannot know exactly when NMI fires relative to game logic. This
uncertainty compounds over thousands of frames.

For SMB specifically:
- ~4000 frames to reach W8-4
- Each NMI advances RNG by ~1-7 bits
- Different RNG patterns affect enemy spawns, item drops, platform timing

## Recommendations

### For Speedrunner Testing
To achieve TAS-accurate timing, the interpreter would need:
1. Per-instruction cycle counting (not averaged)
2. PPU cycle synchronization (3 PPU cycles per CPU cycle)
3. Accurate NMI firing point (during vblank, instruction-boundary aligned)

### For General Gameplay
The current approximation is sufficient for:
- Manual playthroughs (human reaction time >> frame timing differences)
- Testing game logic (most logic is deterministic)
- Debugging decompiled code (behavior is close enough to verify correctness)

The cycle debt system provides a reasonable middle ground between simple
NMI-per-frame timing and full cycle accuracy.

## Path-Based Frame Skipping (Alternative Approach)

For the decompiled Kotlin code, we can use **path-based frame skipping** that detects
game state transitions instead of counting cycles:

### Detectable Expensive Paths

| Event | Condition | Frames to Skip |
|-------|-----------|----------------|
| InitializeGame | Mode=0, Task 0→1 | 2 |
| GameMode start | Mode 0→1 | 1 |
| World/Level change | WorldNumber or LevelNumber changes | 1 |
| Area type change | AreaType changes (underground→overworld, etc.) | 1 |
| Return to title | Mode→0 after game over | 2 |

### Implementation in Decompiled Code

```kotlin
// In the decompiled game code:
fun initializeGame() {
    // ... clear memory, set up game state ...
    runtime.consumeExtraFrames(2)  // Mark this path as multi-frame
}

fun initializeArea() {
    // ... load area data, clear memory ...
    runtime.consumeExtraFrames(1)
}

// In the runtime:
class SMBRuntime {
    private var framesToSkip = 0

    fun consumeExtraFrames(count: Int) {
        framesToSkip += count
    }

    fun shouldFireNmi(): Boolean {
        if (framesToSkip > 0) {
            framesToSkip--
            return false
        }
        return true
    }
}
```

### Path-Based Results

| Milestone | Cycle-Based | Path-Based |
|-----------|-------------|------------|
| W1-1 start | Frame 0 | Frame 0 ✓ |
| W1-2 (warp) | Frame 1943 | Frame 1943 ✓ |
| W1-3 (warp) | Frame 2442 | Frame 2442 ✓ |
| W4-1 (warp) | Frame 3765 | Frame 3765 ✓ |
| W4-2 | Frame 6042 | Not reached |
| W4-3 | Frame 6541 | Not reached |
| Death | Frame 8282 | Frame 8042 |

The path-based approach reaches W4-1 successfully but diverges earlier because it
misses some mid-level expensive operations that don't have detectable state transitions.

### Limitations

Path-based skipping cannot detect:
- Player respawn after death (same level, no state change)
- Internal area transitions (going through a pipe within same level)
- Screen scrolling heavy operations

These would require either:
1. More granular state tracking (track internal flags)
2. Explicit `consumeExtraFrames()` calls in specific code paths
3. Cycle-accurate timing as a fallback

## Critical Fix: LDA $2002 Branch Detection

### The Bug

When testing the interpreter, we detected `LDA $2002` instructions to simulate vblank polling
and skipped 5 bytes (the LDA + the BPL branch instruction) to exit the wait loop:

```kotlin
// OLD (buggy):
if (opcode == 0xAD && nextByte == 0x02 && nextByte2 == 0x20) {
    // Simulate vblank
    interp.cpu.PC = (pc + 5).toUShort()  // Skip LDA $2002 + BPL
}
```

This worked for vblank wait loops (`LDA $2002; BPL loop`) but **corrupted execution**
when `LDA $2002` was used for regular PPU status reads (like in `InitializeNameTables`).

### The Problem

In `InitializeNameTables`:
```asm
InitializeNameTables:
    LDA PPU_STATUS           ; 3 bytes: AD 02 20
    LDA Mirror_PPU_CTRL_REG1 ; 3 bytes: AD 78 07 ← We landed HERE
    ORA #%00010000
    AND #%11110000
    JSR WritePPUReg1
```

Skipping 5 bytes from the first `LDA` landed us in the MIDDLE of the second instruction,
causing garbage execution. This resulted in:
- $0778 (Mirror_PPU_CTRL_REG1) = 0xF0 instead of 0x10
- After NMI handler: $0778 = 0x70 (AND 0x7F of 0xF0) instead of 0x10

### The Fix

Check if the instruction following `LDA $2002` is a BPL (opcode 0x10). Only skip 5 bytes
if it's a vblank wait loop. Otherwise, let the instruction execute normally:

```kotlin
// NEW (fixed):
if (opcode == 0xAD && nextByte == 0x02 && nextByte2 == 0x20) {
    val nextOpcode = interp.memory.readByte(pc + 3).toInt()
    val isBranchLoop = nextOpcode == 0x10  // BPL

    if (isBranchLoop) {
        // Vblank wait loop - skip the loop
        ppu.startFrame()
        ppu.beginVBlank()
        if (ppu.isNmiEnabled()) {
            // Fire NMI...
        }
        ppu.endVBlank()
        interp.cpu.PC = (pc + 5).toUShort()  // Skip LDA $2002 + BPL
    } else {
        // Regular PPU status read - execute normally
        ppu.startFrame()
        ppu.beginVBlank()
        // Let the instruction execute
    }
}
```

### Results After Fix

| Metric | Before Fix | After Fix |
|--------|------------|-----------|
| $0778 at frame 7 | 0x70 (wrong) | 0x10 (correct) |
| Frames 7-10 RAM divergence | ~2 addresses | 0 addresses |
| Overall divergence | 100% | 62% |

The fix dramatically reduced RAM divergence and ensures the PPU mirror register
tracks correctly throughout the reset and initialization sequence.
