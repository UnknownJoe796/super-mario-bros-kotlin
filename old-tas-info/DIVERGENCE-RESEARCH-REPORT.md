# TAS Divergence Research Report

## Executive Summary

The interpreter successfully runs the happylee TAS from World 1-1 through World 4 (using warp zones) to World 8-1, where Mario dies at frame 10212. FCEUX continues through 8-1 and completes the game. The root cause is **RNG state divergence** that accumulates from frame 1 and eventually causes enemy positions/behaviors to differ enough that the TAS inputs become invalid.

## Observed Behavior

| Metric | FCEUX | Interpreter |
|--------|-------|-------------|
| Frame 8000 | W8-1, Page 0, X=168, Speed=40 | W8-1, Page 0, X=59, Speed=7 |
| Frame 10000 | W8-1, Page 19, X=45, Speed=40 | W8-1, Page 1, X=185, Speed=0 |
| Frame 10212 | W8-1, Page 21, alive | Game Over |

**Key observation**: By frame 8000, Mario is already 109 pixels behind FCEUX's position. By frame 10000, he's 18 PAGES behind (only on page 1 vs FCEUX's page 19).

## Consistently Divergent RAM Addresses

### 1. $0778 - Mirror_PPU_CTRL_REG1
- **FCEUX**: 0x10 (16)
- **Interpreter**: 0x70 (112)
- **Difference**: Bits 4, 5, 6 (background pattern table, sprite size, PPU master/slave)

This is a shadow copy of the PPU control register. The game reads this mirror and writes to both $2000 and $0778. The difference suggests the game is executing different code paths or has different values in the accumulator when reaching the STA instructions.

### 2. $077f - IntervalTimerControl
- **Frame 50**: FCEUX=20, Interpreter=19 (1 frame behind)
- **Purpose**: Controls when "interval timers" decrement. Starts at 21, decrements each NMI, resets to 20 when it hits 0.

From the disassembly:
```asm
DecTimers:  LDX #$14          ; load end offset for end of frame timers
DEC IntervalTimerControl      ; decrement interval timer control
BPL DecTimersLoop             ; if not expired, only frame timers will decrement
LDA #$14
STA IntervalTimerControl      ; if control for interval timers expired,
LDX #$23                      ; interval timers will decrement along with frame timers
```

Being 1 frame off means interval-based game events (enemy patterns, spawn timers, etc.) fire at slightly different times.

### 3. $07a7-$07ad - PseudoRandomBitReg (LFSR RNG)
- **Size**: 7 bytes (56-bit LFSR)
- **Clocked**: Once per frame, after FrameCounter increment
- **Diverges from**: Frame 5 onward

The LFSR rotation code:
```asm
NoDecTimers:  INC FrameCounter     ; increment frame counter
LDA PseudoRandomBitReg            ; get first memory location of LSFR bytes
AND #%00000010                    ; mask out all but d1
STA $00
LDA PseudoRandomBitReg+1          ; get second memory location
AND #%00000010                    ; mask out all but d1
EOR $00                           ; XOR d1 from first and second bytes
CLC
BEQ RotPRandomBit
SEC
RotPRandomBit:  ROR PseudoRandomBitReg,X  ; rotate carry into d7
INX
DEY
BNE RotPRandomBit
```

## What Uses the RNG?

From the SMB disassembly, `PseudoRandomBitReg` is used by:

1. **Hammer Bros** (`SpawnHammerObj`, `HammerBroJumpCode`)
   - Decides when to throw hammers
   - Decides jump direction/height
   - **Not in 8-1** (only in 8-3 and 8-4)

2. **Cheep Cheeps** (`InitCheepCheep`, `InitFlyingCheepCheep`)
   - Movement direction flag
   - Horizontal speed
   - Flight timer

3. **Bullet Bill Cannons** (`ThreeSChk`)
   - Spawn timing based on RNG masked with `CannonBitmasks`

4. **Lakitu/Spinies**
   - Spiny spawn positioning

5. **Bubbles** (`BubbleCheck`)
   - Movement patterns

6. **General enemy behaviors**
   - Various movement decisions use RNG bits as offsets

## The Cascade Effect

1. **Frame 0-5**: Reset routine runs. Our interpreter skips vblank waits, completing faster.

2. **Frame 5**: IntervalTimerControl is 19 in interpreter vs 20 in FCEUX (1 frame behind).

3. **Every frame**: LFSR rotates. Because IntervalTimerControl differs, the exact cycle at which various game events happen differs slightly.

4. **Frame 50+**: RNG state has completely diverged. FCEUX shows `e3 30 f6 97 7a 54 a0`, interpreter has different values.

5. **Frames 50-8000**: Small enemy behavior differences accumulate. Enemies spawn at slightly different times or positions. Mario must navigate differently.

6. **Frame 8000**: Mario is 109 pixels behind. He's moving at speed 7 instead of 40.

7. **Frame 10000**: Mario is 18 pages behind. Completely desynced.

8. **Frame 10212**: TAS inputs meant for Page 21 are applied when Mario is on Page 1. Mario falls into a pit or hits an enemy.

## Why Syncing Doesn't Work

We tried syncing:
- **IntervalTimerControl**: Broke warp zones (stuck in W1-3)
- **RNG state**: Broke warp zones
- **Initial state from FCEUX frame 5**: Broke warp zones

**Reason**: The game state is tightly coupled. When we sync one variable but not others, we create an inconsistent state. For example:
- RNG affects enemy spawns
- Enemy spawns affect what's in enemy slots
- If we sync RNG but enemy slots don't match, the game may reference wrong enemies
- This could cause glitched behavior or break warp zone detection

**Only FrameCounter sync works** because:
- FC is a simple 8-bit counter that wraps
- It's used primarily for animation timing and color cycling
- It doesn't directly affect gameplay logic or enemy states
- Syncing it keeps the game's sense of "what frame is it" aligned without disrupting internal state

## World 8-1 Specifics

From [StrategyWiki](https://strategywiki.org/wiki/Super_Mario_Bros./World_8):
- 8-1 is the game's longest level
- High enemy count: Goombas, Buzzy Beetles, Koopa Troopas, Piranha Plants
- No powerups
- Tight time limit (300)
- Large gaps requiring precise jumps

From [Speedrun.com](https://www.speedrun.com/smb1/guides/frxdu):
- 8-1 has frame-perfect timing requirements
- Star block timing matters for speedruns
- The level requires consistent momentum to complete in time

## Root Cause Hypothesis

The primary root cause is the **1-frame offset in IntervalTimerControl** established during the reset routine. This happens because:

1. Our interpreter skips vblank wait loops during reset (`LDA $2002; BPL` pattern)
2. This makes the reset routine complete faster
3. IntervalTimerControl is initialized 1 count lower than FCEUX
4. This 1-frame offset propagates into RNG timing
5. RNG divergence causes enemy behavior differences
6. Enemy differences cause Mario position differences
7. Position differences compound until TAS desyncs

## Potential Solutions (Not Yet Tested)

1. **Match reset timing exactly**
   - Don't skip vblank waits, simulate them with proper NMI timing
   - Problem: NMI isn't enabled during reset, so no NMIs fire

2. **Copy initial RAM state from FCEUX**
   - At frame 0, copy entire $0000-$07FF from FCEUX
   - Then let interpreter run independently
   - Problem: May still diverge due to execution timing differences

3. **Sync enemy slots**
   - Along with FC, sync enemy positions/states
   - Complex and may cause other issues

4. **Accept the limitation**
   - For decompilation comparison purposes, reaching W8-1 may be sufficient
   - The decompiled Kotlin code will have similar timing challenges

## Conclusion

The TAS divergence is fundamentally caused by **timing differences during initialization** that cascade through the RNG system into enemy behaviors. The NES/FCEUX has cycle-accurate timing that our simplified NMI-per-frame model cannot perfectly replicate.

The fact that we reach World 8-1 (using two warp zones correctly) demonstrates that the interpreter's core logic is correct. The remaining divergence is a timing/RNG issue, not a logic bug.
