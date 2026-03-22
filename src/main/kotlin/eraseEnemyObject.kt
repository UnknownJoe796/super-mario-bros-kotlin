// by Claude - EraseEnemyObject subroutine
// Translates EraseEnemyObject: clears all fields for an enemy slot.
package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.EnemyState

/**
 * Clears all enemy object variables for the enemy at offset [x].
 * Called when an enemy needs to be removed from the game.
 */
fun System.eraseEnemyObject(x: Int = ram.objectOffset.toInt()) {
    //> EraseEnemyObject:
    //> lda #$00                 ;clear all enemy object variables
    //> sta Enemy_Flag,x
    ram.enemyFlags[x] = 0
    //> sta Enemy_ID,x
    ram.enemyID[x] = 0
    //> sta Enemy_State,x
    ram.enemyState[x] = EnemyState.INACTIVE.byte
    //> sta FloateyNum_Control,x
    ram.floateyNumControl[x] = 0
    //> sta EnemyIntervalTimer,x
    ram.timers[0x16 + x] = 0  // EnemyIntervalTimer at $796, timers base $780, offset $16
    //> sta ShellChainCounter,x
    ram.shellChainCounters[x] = 0
    //> sta Enemy_SprAttrib,x
    ram.sprAttrib[1 + x] = 0  // Enemy_SprAttrib=$03C5, sprAttrib base=$03C4, so index = 1+x
    //> sta EnemyFrameTimer,x
    ram.timers[0x0a + x] = 0  // EnemyFrameTimer at $78a, timers base $780, offset $0a
    //> rts
}
