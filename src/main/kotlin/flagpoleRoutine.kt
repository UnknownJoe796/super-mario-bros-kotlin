// by Claude - FlagpoleRoutine subroutine
// Translates FlagpoleRoutine: end-of-level flagpole flag movement and scoring.
package com.ivieleague.smbtranslation

//> FlagpoleScoreMods:
//> .db $05, $02, $08, $04, $01
private val flagpoleScoreMods = intArrayOf(0x05, 0x02, 0x08, 0x04, 0x01)

//> FlagpoleScoreDigits:
//> .db $03, $03, $04, $04, $04
private val flagpoleScoreDigits = intArrayOf(0x03, 0x03, 0x04, 0x04, 0x04)

/**
 * Processes the flagpole flag object at enemy slot 5.
 * Moves the flag downward while the player slides, then awards score.
 */
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

    //> lda Enemy_Y_Position,x    ;check flagpole flag's vertical coordinate
    //> cmp #$aa                  ;if flagpole flag down to a certain point,
    //> bcs GiveFPScr             ;branch to end the level
    val flagY = ram.sprObjYPos[1 + x].toInt() and 0xFF  // Enemy_Y_Position,x = sprObj[1+x]
    if (flagY >= 0xAA) {
        giveFlagpoleScore(x)
        return
    }

    //> lda Player_Y_Position     ;check player's vertical coordinate
    //> cmp #$a2                  ;if player down to a certain point,
    //> bcs GiveFPScr             ;branch to end the level
    val playerY = ram.playerYPosition.toInt() and 0xFF
    if (playerY >= 0xA2) {
        giveFlagpoleScore(x)
        return
    }

    //> lda Enemy_YMF_Dummy,x
    //> adc #$ff                  ;add movement amount to dummy variable
    //> sta Enemy_YMF_Dummy,x
    val dummyResult = (ram.sprObjYMFDummy[1 + x].toInt() and 0xFF) + 0xFF
    ram.sprObjYMFDummy[1 + x] = dummyResult.toByte()
    val carry = if (dummyResult > 0xFF) 1 else 0

    //> lda Enemy_Y_Position,x    ;get flag's vertical coordinate
    //> adc #$01                  ;add 1 plus carry to move flag
    //> sta Enemy_Y_Position,x
    ram.sprObjYPos[1 + x] = ((flagY + 1 + carry) and 0xFF).toByte()

    //> lda FlagpoleFNum_YMFDummy
    //> sec                       ;subtract movement amount from dummy variable
    //> sbc #$ff
    val fnumDummyResult = (ram.flagpoleFNumYMFDummy.toInt() and 0xFF) - 0xFF
    val fnumBorrow = if (fnumDummyResult < 0) 1 else 0
    ram.flagpoleFNumYMFDummy = fnumDummyResult.toByte()

    //> lda FlagpoleFNum_Y_Pos
    //> sbc #$01                  ;subtract one plus borrow to move floatey number
    //> sta FlagpoleFNum_Y_Pos
    ram.flagpoleFNumYPos = ((ram.flagpoleFNumYPos.toInt() and 0xFF) - 1 - fnumBorrow).toByte()

    //> SkipScore: jmp FPGfx
    flagpoleGfx(x)
}

private fun System.giveFlagpoleScore(x: Int) {
    //> GiveFPScr:
    //> ldy FlagpoleScore         ;get score offset from earlier
    val y = ram.flagpoleScore.toInt() and 0xFF
    //> lda FlagpoleScoreMods,y   ;get amount to award player points
    //> ldx FlagpoleScoreDigits,y ;get digit with which to award points
    //> sta DigitModifier,x       ;store in digit modifier
    val scoreMod = flagpoleScoreMods[y]
    val digitIdx = flagpoleScoreDigits[y]
    ram.digitModifier[digitIdx] = scoreMod.toByte()
    //> jsr AddToScore            ;do sub to award player points
    addToScore()
    //> lda #$05
    //> sta GameEngineSubroutine  ;set to run end-of-level subroutine on next frame
    ram.gameEngineSubroutine = GameEngineRoutine.PlayerEndLevel

    //> FPGfx:
    flagpoleGfx(x)
}

private fun System.flagpoleGfx(x: Int) {
    //> FPGfx:
    //> jsr GetEnemyOffscreenBits ;get offscreen information
    getEnemyOffscreenBits()
    //> jsr RelativeEnemyPosition ;get relative coordinates
    relativeEnemyPosition()
    //> jsr FlagpoleGfxHandler    ;draw flagpole flag and floatey number
    flagpoleGfxHandler()
    //> ExitFlagP: rts
}

// flagpoleGfxHandler() moved to drawRoutines.kt
