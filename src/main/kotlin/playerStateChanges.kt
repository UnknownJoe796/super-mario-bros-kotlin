// by Claude - Player state change routines: grow/shrink, injury blink, death, fire flower
package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor

/**
 * Handles the player's grow/shrink animation during a size change (mushroom pickup or damage).
 * Checks the master timer for two specific moments:
 * - At $f8: triggers the actual size change via [initChangeSize].
 * - At $c4: finishes the task and returns control to normal player routine.
 *
 * Dispatch index 9 from GameRoutines.
 */
fun System.playerChangeSize() {
    //> PlayerChangeSize:
    //> lda TimerControl    ;check master timer control
    //> cmp #$f8            ;for specific moment in time
    //> bne EndChgSize      ;branch if before or after that point
    val tc = ram.timerControl.toInt() and 0xFF
    if (tc == 0xF8) {
        //> jmp InitChangeSize  ;otherwise run code to get growing/shrinking going
        initChangeSize()
        return
    }
    //> EndChgSize:  cmp #$c4            ;check again for another specific moment
    //> bne ExitChgSize     ;and branch to leave if before or after that point
    if (tc == 0xC4) {
        //> jsr DonePlayerTask  ;otherwise do sub to init timer control and set routine
        donePlayerTask()
    }
    //> ExitChgSize: rts                 ;and then leave
}

/**
 * Handles the player's post-injury invincibility blinking.
 * Timer-driven: while the master timer is >= $f0, does nothing (the player blinks
 * via the renderer). At $c8, the task is finished. Between those values, normal
 * player control runs so the player can still move.
 *
 * Dispatch index 10 from GameRoutines.
 */
fun System.playerInjuryBlink() {
    //> PlayerInjuryBlink:
    //> lda TimerControl       ;check master timer control
    //> cmp #$f0               ;for specific moment in time
    //> bcs ExitBlink          ;branch if before that point
    val tc = ram.timerControl.toInt() and 0xFF
    if (tc >= 0xF0) {
        //> ExitBlink: bne ExitBoth           ;do unconditional branch to leave
        // When tc > $F0: bne is taken → exits (ExitBoth)
        // When tc == $F0: bne falls through → InitChangeSize (size change happens!)
        if (tc != 0xF0) return
        initChangeSize()
        return
    }
    //> cmp #$c8               ;check again for another specific point
    //> beq DonePlayerTask     ;branch if at that point, and not before or after
    if (tc == 0xC8) {
        donePlayerTask()
        return
    }
    //> jmp PlayerCtrlRoutine  ;otherwise run player control routine
    playerCtrlRoutine()
}

/**
 * Handles the player's death animation sequence.
 * While the master timer is >= $f0, does nothing (death jump animation plays).
 * Once the timer drops below $f0, normal player control resumes (which runs
 * the death bounce physics).
 *
 * Dispatch index 11 from GameRoutines.
 */
fun System.playerDeath() {
    //> PlayerDeath:
    //> lda TimerControl       ;check master timer control
    //> cmp #$f0               ;for specific moment in time
    //> bcs ExitDeath          ;branch to leave if before that point
    val tc = ram.timerControl.toInt() and 0xFF
    if (tc >= 0xF0) {
        //> ExitDeath: rts          ;leave from death routine
        return
    }
    //> jmp PlayerCtrlRoutine  ;otherwise run player control routine
    playerCtrlRoutine()
}

/**
 * Handles the fire flower power-up animation. Cycles the player's palette every
 * four frames to create the flashing effect. When the master timer reaches $c0,
 * resets the palette and returns control to the normal player routine.
 *
 * Dispatch index 12 from GameRoutines.
 */
fun System.playerFireFlower() {
    //> PlayerFireFlower:
    //> lda TimerControl       ;check master timer control
    //> cmp #$c0               ;for specific moment in time
    //> beq ResetPalFireFlower ;branch if at moment, not before or after
    val tc = ram.timerControl.toInt() and 0xFF
    if (tc == 0xC0) {
        //> ResetPalFireFlower:
        //> jsr DonePlayerTask    ;do sub to init timer control and run player control routine
        donePlayerTask()
        //> (falls through to ResetPalStar)
        resetPalStar()
        return
    }
    //> lda FrameCounter       ;get frame counter
    //> lsr
    //> lsr                    ;divide by four to change every four frames
    val paletteBits = (ram.frameCounter.toInt() and 0xFF) ushr 2
    //> (falls through to CyclePlayerPalette)
    cyclePlayerPalette(paletteBits)
}

/**
 * Initializes the growing/shrinking animation. Sets the size-change flag,
 * zeroes the animation frame control, and inverts the player's size (big <-> small).
 * Called from [playerChangeSize] at the $f8 timer moment.
 */
private fun System.initChangeSize() {
    //> InitChangeSize:
    //> ldy PlayerChangeSizeFlag  ;if growing/shrinking flag already set
    //> bne ExitBoth              ;then branch to leave
    if (ram.playerChangeSizeFlag != 0.toByte()) return
    //> sty PlayerAnimCtrl        ;otherwise initialize player's animation frame control
    ram.playerAnimCtrl = 0
    //> inc PlayerChangeSizeFlag  ;set growing/shrinking flag
    ram.playerChangeSizeFlag++
    //> lda PlayerSize
    //> eor #$01                  ;invert player's size
    //> sta PlayerSize
    ram.playerSize = if (ram.playerSize == PlayerSize.Big) PlayerSize.Small else PlayerSize.Big
    //> ExitBoth: rts                       ;leave
}

/**
 * Finishes a player state task by clearing the master timer control
 * and setting the game engine subroutine to the normal player control routine (index 8).
 */
fun System.donePlayerTask() {
    //> DonePlayerTask:
    //> lda #$00
    //> sta TimerControl          ;initialize master timer control to continue timers
    ram.timerControl = 0
    //> lda #$08
    //> sta GameEngineSubroutine  ;set player control routine to run next frame
    ram.gameEngineSubroutine = GameEngineRoutine.PlayerCtrlRoutine
    //> rts                       ;leave
}

// cyclePlayerPalette() and resetPalStar() are in gameMode.kt
// playerCtrlRoutine() is in playerCtrlRoutine.kt
