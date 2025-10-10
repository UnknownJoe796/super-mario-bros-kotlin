package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.Constants.Silence
import kotlin.experimental.xor

//> HalfwayPageNybbles:
//> ;page numbers are in order from -1 to -4
//> .db $56, $40
//> .db $65, $70
//> .db $66, $40
//> .db $66, $40
//> .db $66, $40
//> .db $66, $60
//> .db $65, $70
//> .db $00, $00
/**
 * The halfway checkpoint page numbers for each level, organized by world then level.
 * For example, HalfwayPageNybbles[3][2] would get the halfway page of level 4-3.
 */
private val HalfwayPageNybbles = listOf(
    listOf<Byte>(0x5, 0x6, 0x4, 0x0),
    listOf<Byte>(0x6, 0x5, 0x7, 0x0),
    listOf<Byte>(0x6, 0x6, 0x4, 0x0),
    listOf<Byte>(0x6, 0x6, 0x4, 0x0),
    listOf<Byte>(0x6, 0x6, 0x4, 0x0),
    listOf<Byte>(0x6, 0x6, 0x6, 0x0),
    listOf<Byte>(0x6, 0x5, 0x7, 0x0),
    listOf<Byte>(0x0, 0x0, 0x0, 0x0),
)

/**
 * Kotlin translation of PlayerLoseLife.
 * Disables the screen, silences music, decrements lives, and either sets Game Over mode
 * or computes/stores the halfway page, transposes players for 2P, and continues the game.
 */
fun System.playerLoseLife() {
    //> PlayerLoseLife:
    //> inc DisableScreenFlag    ;disable screen and sprite 0 check
    ram.disableScreenFlag = true
    //> lda #$00
    //> sta Sprite0HitDetectFlag
    ram.sprite0HitDetectFlag = false
    //> lda #Silence             ;silence music
    //> sta EventMusicQueue
    ram.eventMusicQueue = Silence
    //> dec NumberofLives        ;take one life from player
    //> bpl StillInGame          ;if player still has lives, branch
    if (--ram.numberofLives < 0) {
        //> lda #$00
        //> sta OperMode_Task        ;initialize mode task,
        ram.operModeTask = 0x00
        //> lda #GameOverModeValue   ;switch to game over mode
        //> sta OperMode             ;and leave
        ram.operMode = OperMode.GameOver
        //> rts
        return
    }

    //> StillInGame: lda WorldNumber          ;multiply world number by 2 and use
    //> asl                      ;as offset
    //> tax
    //> lda LevelNumber          ;if in area -3 or -4, increment
    //> and #$02                 ;offset by one byte, otherwise
    //> beq GetHalfway           ;leave offset alone
    //> inx
    //> GetHalfway:  ldy HalfwayPageNybbles,x ;get halfway page number with offset
    //> lda LevelNumber          ;check area number's LSB
    //> lsr
    //> tya                      ;if in area -2 or -4, use lower nybble
    //> bcs MaskHPNyb
    //> lsr                      ;move higher nybble to lower if area
    //> lsr                      ;number is -1 or -3
    //> lsr
    //> lsr
    //> MaskHPNyb:   and #%00001111           ;mask out all but lower nybble
    val halfNyb = HalfwayPageNybbles[ram.worldNumber.toInt()][ram.levelNumber.toInt()]

    //> cmp ScreenLeft_PageLoc
    //> beq SetHalfway           ;left side of screen must be at the halfway page,
    //> bcc SetHalfway           ;otherwise player must start at the
    //> lda #$00                 ;beginning of the level
    //> SetHalfway:  sta HalfwayPage          ;store as halfway page for player
    ram.halfwayPage = if (halfNyb <= ram.screenLeftPageLoc) halfNyb else 0

    //> jsr TransposePlayers     ;switch players around if 2-player game
    transposePlayers()
    //> jmp ContinueGame         ;continue the game
    continueGame()
}

/**
 * @return true if the game should end.
 * The original code returns the value via the carry flag.
 */
fun System.transposePlayers(): Boolean {
    //> TransposePlayers:
    //> sec                       ;set carry flag by default to end game
    //> lda NumberOfPlayers       ;if only a 1 player game, leave
    //> beq ExTrans
    if (ram.numberOfPlayers == 0.toByte()) return true
    //> lda OffScr_NumberofLives  ;does offscreen player have any lives left?
    //> bmi ExTrans               ;branch if not
    if (ram.offScrNumberofLives < 0) return true
    //> lda CurrentPlayer         ;invert bit to update
    //> eor #%00000001            ;which player is on the screen
    //> sta CurrentPlayer
    ram.currentPlayer = ram.currentPlayer.xor(0x1)
    //> ldx #$06

    //> TransLoop: lda OnscreenPlayerInfo,x    ;transpose the information
    //> pha                         ;of the onscreen player
    //> lda OffscreenPlayerInfo,x   ;with that of the offscreen player
    //> sta OnscreenPlayerInfo,x
    //> pla
    //> sta OffscreenPlayerInfo,x
    //> dex
    //> bpl TransLoop
    //> clc            ;clear carry flag to get game going
    //> ExTrans:   rts
    // Equivalent is to swap all of the individual fields
    val numberofLivesTemp = ram.numberofLives
    ram.numberofLives = ram.offScrNumberofLives
    ram.offScrNumberofLives = numberofLivesTemp

    val halfwayPageTemp = ram.halfwayPage
    ram.halfwayPage = ram.offScrHalfwayPage
    ram.offScrHalfwayPage = halfwayPageTemp

    val levelNumberTemp = ram.levelNumber
    ram.levelNumber = ram.offScrLevelNumber
    ram.offScrLevelNumber = levelNumberTemp

    val hidden1UpFlagTemp = ram.hidden1UpFlag
    ram.hidden1UpFlag = ram.offScrHidden1UpFlag
    ram.offScrHidden1UpFlag = hidden1UpFlagTemp

    val coinTallyTemp = ram.coinTally
    ram.coinTally = ram.offScrCoinTally
    ram.offScrCoinTally = coinTallyTemp

    val worldNumberTemp = ram.worldNumber
    ram.worldNumber = ram.offScrWorldNumber
    ram.offScrWorldNumber = worldNumberTemp

    val areaNumberTemp = ram.areaNumber
    ram.areaNumber = ram.offScrAreaNumber
    ram.offScrAreaNumber = areaNumberTemp

    return false
}
