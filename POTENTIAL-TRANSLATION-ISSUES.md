# Potential Translation Issues - Audit Results

This document tracks findings from comparing the Kotlin translation against the `smbdism.asm` disassembly.

## Verified and Fixed Issues

### 1. bowserRoutine.kt - bowserGfxHandler: Early return skips rear half
- **Assembly label**: BowserGfxHandler (line 10264)
- **Status**: FIXED
- **Issue**: The original Kotlin `if (!processBowserHalf()) return` incorrectly exited the entire handler if the front half had a non-normal state.
- **Fix**: Changed to call `processBowserHalf()` unconditionally for the front half, allowing the rear half to be processed.

### 2. drawRoutines.kt - drawFirebar: Flip animation bit check
- **Assembly label**: DrawFirebar (line 14236)
- **Status**: FIXED
- **Issue**: The original Kotlin `val flipCtrl = fc ushr 1` where `fc` was already `FrameCounter >> 2` resulted in checking `FrameCounter >> 3` for the flip bit. While this technically checked bit 3 (which is correct), the implementation was confusing and deviated from the assembly's direct shifts.
- **Fix**: Simplified the bit check to `if ((fc and 0x02) != 0)` where `fc` is `FrameCounter >> 2`. This directly checks bit 3 of `FrameCounter`.

## False Positives (Verified Correct)

The following issues were originally flagged as potential errors but were found to be correctly implemented according to the disassembly logic.

### 1. victoryMode.kt - playerVictoryWalk: Inverted page comparison
- **Label**: PlayerVictoryWalk (line 1144)
- **Status**: FALSE POSITIVE
- **Analysis**: The assembly `bne PerformWalk` branches if `Player_PageLoc != DestinationPageLoc`. If it *doesn't* branch, it checks `Player_X_Position < $60` and if so, reaches `PerformWalk`. The Kotlin logic `if(ram.playerPageLoc != ram.destinationPageLoc || ram.playerXPosition < 0x60u)` correctly combines these two paths for incrementing `VictoryWalkControl`.

### 2. enemyBGCollision.kt - chkToStunEnemiesLocal: Demote range inverted
- **Label**: ChkToStunEnemies (line 12455)
- **Status**: FALSE POSITIVE
- **Analysis**: The assembly demotes IDs {0x09, 0x0D, 0x0E, 0x0F, 0x10}. The Kotlin code correctly implements this by checking `if (enemyId < 0x0a || enemyId >= Constants.PiranhaPlant)` (where `PiranhaPlant` is 0x0D) within the range of 0x09 to 0x10.

### 3. drawRoutines.kt - drawExplosionFireball: Wrong index for Alt_SprDataOffset
- **Label**: DrawExplosion_Fireball (line 14258)
- **Status**: FALSE POSITIVE
- **Analysis**: The code already uses `val x = ram.objectOffset.toInt()` and then `ram.altSprDataOffset[x]`, which matches the assembly `ldy Alt_SprDataOffset,x`.

### 4. victoryMode.kt - thankPlayer: Missing return on incMsgCounter call
- **Label**: ThankPlayer (line 1198)
- **Status**: FALSE POSITIVE
- **Analysis**: The code already contains `return incMsgCounter()`, which correctly prevents fall-through as required by the assembly `bcs IncMsgCounter`.

### 5. processCannons.kt - offscreenBoundsCheck: Carry propagation
- **Label**: OffscreenBoundsCheck (line 11010)
- **Status**: FALSE POSITIVE
- **Analysis**: The Kotlin code models the carry flag from the initial comparisons and uses it in the subsequent `adc` (+1) and `sbc` (- (1 - carry)) operations. This correctly models 6502 carry propagation.

---
**Final Divergence Rate (HappyLee TAS): 0.07%** (13 divergent frames out of 17,859).
Game completion successfully reached in W8-4.
