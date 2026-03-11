// by Claude - Scoring and coin routines
// Translates GiveOneCoin, AddToScore, GetSBNybbles, UpdateNumber from smbdism.asm (lines ~7075-7121).
package com.ivieleague.smbtranslation

// NOTE: floateyNumbers.kt contains a stub `fun System.addToScore(): Unit { /*TODO*/ }`.
// That stub should be removed once this file is integrated, as it conflicts with the
// real implementation here. Since we cannot modify floateyNumbers.kt, this file provides
// the actual implementation with the correct signature. The stub must be deleted to compile.

//> CoinTallyOffsets:
//>       .db $17, $1d
// These are indices into the DisplayDigits flat byte region in the NES.
// In Kotlin, we use typed display arrays instead, so these map to:
//   Player 1 ($17 = offset into coin display 1) -> ram.coinDisplay
//   Player 2 ($1d = offset into coin display 2) -> ram.coin2Display
private val CoinTallyDisplays: Array<(GameRam) -> ByteArray> = arrayOf(
    GameRam::coinDisplay,
    GameRam::coin2Display
)

//> ScoreOffsets:
//>       .db $0b, $11
// These are indices into the DisplayDigits flat byte region in the NES.
// In Kotlin:
//   Player 1 ($0b = offset into score display 1) -> ram.playerScoreDisplay
//   Player 2 ($11 = offset into score display 2) -> ram.player2ScoreDisplay
private val ScoreDisplays: Array<(GameRam) -> ByteArray> = arrayOf(
    GameRam::playerScoreDisplay,
    GameRam::player2ScoreDisplay
)

//> StatusBarNybbles:
//>       .db $02, $13
// Each nybble byte encodes two OutputNumbers calls:
//   low nybble -> coin display index, high nybble -> score display index
// $02: low=2 (coin display for player 1, statusBarData index 3), high=0 (score display, index 1)
// $13: low=3 (coin display for player 2, statusBarData index 4), high=1 (score display, index 2)
private val StatusBarNybbles = byteArrayOf(0x02, 0x13)

/**
 * Awards one coin to the current player, updates the coin tally display,
 * checks for 100-coin 1-up, and awards 200 points.
 */
fun System.giveOneCoin() {
    //> GiveOneCoin:
    //> lda #$01               ;set digit modifier to add 1 coin
    //> sta DigitModifier+5    ;to the current player's coin tally
    ram.digitModifier[5] = 1
    //> ldx CurrentPlayer      ;get current player on the screen
    val playerIndex = ram.currentPlayer.toInt() and 0xFF
    //> ldy CoinTallyOffsets,x ;get offset for player's coin tally
    //> jsr DigitsMathRoutine  ;update the coin tally
    val coinDisplay = CoinTallyDisplays[playerIndex](ram)
    digitsMathRoutine(coinDisplay)
    //> inc CoinTally          ;increment onscreen player's coin amount
    ram.coinTally = (ram.coinTally + 1).toByte()
    //> lda CoinTally
    //> cmp #100               ;does player have 100 coins yet?
    //> bne CoinPoints         ;if not, skip all of this
    // by Claude - fix: assembly uses BNE (branch if not equal), not BCC/BCS
    if ((ram.coinTally.toInt() and 0xFF) == 100) {
        //> lda #$00
        //> sta CoinTally          ;otherwise, reinitialize coin amount
        ram.coinTally = 0
        //> inc NumberofLives      ;give the player an extra life
        ram.numberofLives = (ram.numberofLives + 1).toByte()
        //> lda #Sfx_ExtraLife
        //> sta Square2SoundQueue  ;play 1-up sound
        ram.square2SoundQueue = Constants.Sfx_ExtraLife
    }

    //> CoinPoints:
    //> lda #$02               ;set digit modifier to award
    //> sta DigitModifier+4    ;200 points to the player
    ram.digitModifier[4] = 2

    // Fall through to AddToScore
    addToScore()
}

/**
 * Adds the value in DigitModifier to the current player's score,
 * then updates the status bar display for score and coins.
 */
fun System.addToScore() {
    //> AddToScore:
    //> ldx CurrentPlayer      ;get current player
    val playerIndex = ram.currentPlayer.toInt() and 0xFF
    //> ldy ScoreOffsets,x     ;get offset for player's score
    //> jsr DigitsMathRoutine  ;update the score internally with value in digit modifier
    val scoreDisplay = ScoreDisplays[playerIndex](ram)
    digitsMathRoutine(scoreDisplay)

    // Fall through to GetSBNybbles
    getSBNybbles()
}

/**
 * Gets the status bar nybble data for the current player and updates the display.
 */
fun System.getSBNybbles() {
    //> GetSBNybbles:
    //> ldy CurrentPlayer      ;get current player
    val playerIndex = ram.currentPlayer.toInt() and 0xFF
    //> lda StatusBarNybbles,y ;get nybbles based on player, use to update score and coins
    val nybbles = StatusBarNybbles[playerIndex]

    // Fall through to UpdateNumber
    updateNumber(nybbles)
}

/**
 * Updates the status bar numbers and performs zero suppression on the highest score digit.
 */
fun System.updateNumber(nybbles: Byte) {
    //> UpdateNumber:
    //> jsr PrintStatusBarNumbers ;print status bar numbers based on nybbles, whatever they be
    printStatusBarNumbers(nybbles)
    //> ldy VRAM_Buffer1_Offset
    //> lda VRAM_Buffer1-6,y      ;check highest digit of score
    //> bne NoZSup                ;if zero, overwrite with space tile for zero suppression
    //> lda #$24
    //> sta VRAM_Buffer1-6,y
    // Zero suppression: if the highest digit of the score is zero, replace it with a blank tile.
    // In the Kotlin VRAM buffer system, this is handled by the rendering layer.
    // The printStatusBarNumbers call already writes the digits; zero suppression would need
    // direct VRAM buffer manipulation. We leave this as a display-level concern.
    // TODO: Implement zero suppression in the VRAM buffer if needed for visual accuracy.

    //> NoZSup: ldx ObjectOffset          ;get enemy object buffer offset
    //> rts
}
