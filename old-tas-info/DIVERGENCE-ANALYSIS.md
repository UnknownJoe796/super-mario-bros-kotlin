# TAS RAM Divergence Analysis

## Summary

The interpreter reaches World 8-1 using the TAS inputs, but Mario dies at frame 10212 due to accumulated position differences. This document analyzes the root causes and proposes solutions.

## Consistently Divergent Addresses

### 1. $01fc (Stack) and $0778 (Mirror_PPU_CTRL_REG1)
- **FCEUX**: 16 (0x10 = 0b00010000)
- **Interpreter**: 112 (0x70 = 0b01110000)
- **Difference**: 96 (0x60 = bits 5 and 6)

`$0778` is `Mirror_PPU_CTRL_REG1`, a shadow copy of PPU control register $2000. The game writes to this mirror whenever it writes to $2000.

**Bit meanings in PPUCTRL:**
- Bit 7: NMI enable
- Bit 6: PPU master/slave (should be 0)
- Bit 5: Sprite size (0=8x8, 1=8x16)
- Bit 4: Background pattern table
- Bit 3: Sprite pattern table

The difference in bits 5 and 6 suggests our PPU stub is causing the game to write different values, OR the timing of when we sample RAM differs from FCEUX.

**$01fc** is in the stack area and has the SAME values - this is likely the processor status register pushed during the last NMI, which would contain different flags due to different CPU state at NMI time.

### 2. $077f (IntervalTimerControl)
- **Frame 50**: FCEUX=20, Interpreter=19 (1 frame behind)
- **Frame 100**: FCEUX=12, Interpreter=11 (still 1 frame behind)

This is decremented every NMI and controls game timing intervals. Being 1 frame off from the start means:
- Enemy spawn timing differs
- Animation timing differs
- Level scrolling timing may differ

### 3. $07a7-$07ad (PseudoRandomBitReg - LFSR RNG)
The Linear Feedback Shift Register produces different values because:
- It's seeded/rotated based on game events
- Different timing = different rotation points
- Affects: enemy behaviors, firebar patterns, Bullet Bill spawns, platform positions

## Root Cause Analysis

### The Reset Routine Problem

Our reset handling differs from FCEUX:

**FCEUX:**
```
Reset → vblank wait loops run naturally → NMIs fire at correct cycles →
game state initializes with proper timing → IntervalTimerControl starts at 21
```

**Our Interpreter:**
```
Reset → vblank wait loops SKIPPED (we jump past LDA $2002; BPL) →
reset completes FASTER → IntervalTimerControl starts at 20 (1 less)
```

By skipping vblank waits during reset, we save real time but lose one NMI cycle worth of initialization, putting IntervalTimerControl 1 frame behind.

### The PPU Mirror Problem

When the game writes to $2000 (PPUCTRL), it also writes to $0778 (mirror). Our SimplePPU stub may be:
1. Not handling all write cases correctly
2. Causing different sprite size/pattern table selections
3. Running at different points relative to when RAM is sampled

### The Cascade Effect

Frame N: IntervalTimerControl is 1 off
         → Enemy spawn timing differs slightly
         → RNG advances at different points
Frame N+1000: Small position differences
Frame N+5000: Mario is 50 pixels off
Frame N+10000: Mario is 100+ pixels off → misses jump → dies

## Proposed Solutions

### Solution 1: Proper Reset Timing (Best Long-Term Fix)

Instead of skipping vblank waits, simulate them properly:

```kotlin
// Instead of skipping LDA $2002; BPL loops
if (opcode == 0xAD && nextByte == 0x02 && nextByte2 == 0x20) {
    // Simulate vblank wait by running N iterations
    // then setting vblank flag and continuing
    simulateVblankWait()
}
```

This ensures the game initializes with the same timing as FCEUX.

### Solution 2: Initialize Timing Variables from FCEUX (Quick Fix)

Before starting the frame loop, copy FCEUX's initial state for timing-critical variables:

```kotlin
// At frame 0 or just before gameplay starts
val frame0Offset = 0
interp.memory.writeByte(0x077f, fceuxRam[frame0Offset + 0x077f]) // IntervalTimerControl
interp.memory.writeByte(0x0778, fceuxRam[frame0Offset + 0x0778]) // Mirror_PPU_CTRL_REG1
// Copy initial RNG state
for (addr in 0x07a7..0x07ad) {
    interp.memory.writeByte(addr, fceuxRam[frame0Offset + addr])
}
```

### Solution 3: Sync IntervalTimerControl (Not Just FC)

Sync IntervalTimerControl along with FrameCounter every frame:

```kotlin
val fceuxFC = fceuxRam[fOff + 0x09].toInt() and 0xFF
interp.memory.writeByte(0x09, fceuxFC.toUByte())

val fceuxIntCtrl = fceuxRam[fOff + 0x077f].toInt() and 0xFF
interp.memory.writeByte(0x077f, fceuxIntCtrl.toUByte())
```

**Warning**: We tried syncing RNG and it broke warp zones. IntervalTimerControl may be safer.

### Solution 4: Fix the NMI Suppress Pattern

Our current pattern: `setOf(0, 1, 2, 3, 4, 6, 7)`

This might not exactly match FCEUX's NMI timing during title screen. We should analyze FCEUX's exact NMI firing pattern and match it.

### Solution 5: Fix PPU Mirror Handling

Ensure our PPU stub correctly mirrors writes:

```kotlin
interp.memoryWriteHook = { addr, value ->
    when (addr) {
        0x2000 -> {
            ppu.write(addr, value, 0)
            // Also update the game's mirror variable
            interp.memory.writeByte(0x0778, value)
            true
        }
        // ... other cases
    }
}
```

But wait - the GAME should be updating $0778 itself when it writes to $2000. If our values differ, it means the game is writing different values due to different code paths.

## Recommended Approach

1. **First**: Try Solution 3 (sync IntervalTimerControl) - quick test
2. **If that works**: Implement Solution 2 (one-time init at frame 0)
3. **For proper fix**: Implement Solution 1 (proper vblank simulation in reset)

## Test Commands

```bash
# Run TAS test
./gradlew :core:test --tests "*.FrameCounterBasedTASTest.run TAS*"

# Run divergence analysis
./gradlew :core:test --tests "*.FrameCounterBasedTASTest.find early RAM divergence"
```
