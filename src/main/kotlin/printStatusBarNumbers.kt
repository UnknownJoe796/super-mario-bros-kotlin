package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.chr.OriginalRom

//> ;-------------------------------------------------------------------------------------
//> ;$00 - used to store status bar nybbles
//> ;$02 - used as temp vram offset
//> ;$03 - used to store length of status bar number

//> ;status bar name table offset and length data
//> StatusBarData:
// PPU Buffer partial data - lower byte of address AND control bits
//> StatusBarOffset:
//> .db $06, $0c, $12, $18, $1e, $24
data class StatusBarData(val xOffset: Int, val yOffset: Int, val length: Int, val fetcher: (GameRam)-> ByteArray)
private val statusBarData = listOf(
    //> .db $f0, $06 ; top score display on title screen
    StatusBarData(0xf0 % 0x20, 0xf0 / 0x20, 0x06, fetcher = GameRam::topScoreDisplay),
    //> .db $62, $06 ; player score
    StatusBarData(0x62 % 0x20, 0x62 / 0x20, 0x06, fetcher = GameRam::playerScoreDisplay),
    //> .db $62, $06
    StatusBarData(0x62 % 0x20, 0x62 / 0x20, 0x06, fetcher = GameRam::player2ScoreDisplay),
    //> .db $6d, $02 ; coin tally
    StatusBarData(0x6d % 0x20, 0x6d / 0x20, 0x02, fetcher = GameRam::coinDisplay),
    //> .db $6d, $02
    StatusBarData(0x6d % 0x20, 0x6d / 0x20, 0x02, fetcher = GameRam::coin2Display),
    //> .db $7a, $03 ; game timer
    StatusBarData(0x7a % 0x20, 0x7a / 0x20, 0x03, fetcher = GameRam::gameTimerDisplay),
)


fun System.printStatusBarNumbers(nybbleByte: Byte) {
    //> PrintStatusBarNumbers:
    //> sta $00            ;store player-specific offset
    //> jsr OutputNumbers  ;use first nybble to print the coin display
    outputNumbers((nybbleByte.toInt() and 0x0F).toByte())
    //> lda $00            ;move high nybble to low
    //> lsr                ;and print to score display
    //> lsr
    //> lsr
    //> lsr
    outputNumbers(((nybbleByte.toInt() ushr 4) and 0x0F).toByte())
}
fun System.printStatusBarNumbers(coinDisplayOffset: Byte = 0, scoreDisplayOffset: Byte = 0) {
    outputNumbers(coinDisplayOffset)
    outputNumbers(scoreDisplayOffset)
}
private fun System.outputNumbers(statusBarIndexMinusOne: Byte) {
    //> OutputNumbers:
    //> clc                      ;add 1 to low nybble
    //> adc #$01
    //> and #%00001111           ;mask out high nybble
    val statusBarIndex = statusBarIndexMinusOne.plus(1).and(0b1111)
    //> cmp #$06
    //> bcs ExitOutputN
    if (statusBarIndex >= 6) return
    //> pha                      ;save incremented value to stack for now and
    //> asl                      ;shift to left and use as offset
    //> tay
    // This would calculate the offset in the StatusBarData table.  We don't care, though, since we access it in a more convenient way anyways.

    //> ldx VRAM_Buffer1_Offset  ;get current buffer pointer
    //> lda #$20                 ;put at top of screen by default
    //> cpy #$00                 ;are we writing top score on title screen?
    //> bne SetupNums
    //> lda #$22                 ;if so, put further down on the screen
    var startY = if(statusBarIndex == 0) 0x200 / 0x20 else 0x0

    //> SetupNums:   sta VRAM_Buffer1,x
    //> lda StatusBarData,y      ;write low vram address and length of thing
    //> sta VRAM_Buffer1+1,x     ;we're printing to the buffer
    val data = statusBarData[statusBarIndex]
    val startX = data.xOffset
    startY += data.yOffset

    //> lda StatusBarData+1,y
    //> sta VRAM_Buffer1+2,x
    val length = data.length

    //> sta $03                  ;save length byte in counter
    //> stx $02                  ;and buffer pointer elsewhere for now
    //> pla                      ;pull original incremented value from stack
    //> tax
    //> lda StatusBarOffset,x    ;load offset to value we want to write
    //> sec
    //> sbc StatusBarData+1,y    ;subtract from length byte we read before
    val toDisplay = data.fetcher(ram)
    require(toDisplay.size == length) {
        "Status bar data length mismatch for index=$statusBarIndex: expected $length, got ${toDisplay.size}"
    }

    //> tay                      ;use value as offset to display digits
    //> ldx $02

    //> DigitPLoop:  lda DisplayDigits,y      ;write digits to the buffer
    //> sta VRAM_Buffer1+3,x
    //> inx
    //> iny
    //> dec $03                  ;do this until all the digits are written
    //> bne DigitPLoop
    val patterns = toDisplay.map { OriginalRom.backgrounds[it.toInt()] }

    //> lda #$00                 ;put null terminator at end
    //> sta VRAM_Buffer1+3,x
    //> inx                      ;increment buffer pointer by 3
    //> inx
    //> inx
    //> stx VRAM_Buffer1_Offset  ;store it in case we want to use it again
    ram.vRAMBuffer1.add(BufferedPpuUpdate.BackgroundPatternString(
        nametable = 0,
        x = startX.toByte(),
        y = startY.toByte(),
        drawVertically = false,
        patterns = patterns
    ))
    //> ExitOutputN: rts
}

fun System.digitsMathRoutine() {
    //> DigitsMathRoutine:
    // This parameterless version cannot know which DisplayDigits buffer (score/coins/timer)
    // was selected by the original caller via the Y register. To preserve safety in our
    // Kotlin translation when no target is specified, we only perform the reset portion
    // (EraseDMods) that the routine always executes when in title mode, and provide an
    // overload below that accepts the concrete display digits buffer and starting index.
    //> lda OperMode              ;check mode of operation
    //> cmp #TitleScreenModeValue
    //> beq EraseDMods            ;if in title screen mode, branch to lock score
    // With no target buffer we cannot safely do the add/carry/borrow work here.
    // Fall through to EraseDMods behavior.
    //> EraseDMods: lda #$00                  ;store zero here
    //> ldx #$06                  ;start with the last digit
    //> EraseMLoop: sta DigitModifier-1,x     ;initialize the digit amounts to increment
    //> dex
    //> bpl EraseMLoop            ;do this until they're all reset, then leave
    //> rts
    for (i in 0..5) ram.digitModifier[i] = 0
}

fun System.digitsMathRoutine(displayDigits: ByteArray, startIndex: Int = displayDigits.lastIndex) {
    //> DigitsMathRoutine:
    // This overload models the original subroutine with Y pointing at the least significant
    // digit of the selected display buffer. We pass that as 'startIndex'. We then walk X
    // from 5 down to 0, applying the DigitModifier to each digit with proper borrow/carry.
    //> lda OperMode              ;check mode of operation
    //> cmp #TitleScreenModeValue
    //> beq EraseDMods            ;if in title screen mode, branch to lock score
    if (ram.operMode == OperMode.TitleScreen) {
        //> EraseDMods: lda #$00                  ;store zero here
        //> ldx #$06                  ;start with the last digit
        //> EraseMLoop: sta DigitModifier-1,x     ;initialize the digit amounts to increment
        //> dex
        //> bpl EraseMLoop            ;do this until they're all reset, then leave
        for (i in 0..5) ram.digitModifier[i] = 0
        //> rts
        return
    }

    //> ldx #$05
    var y = startIndex
    for (x in 5 downTo 0) {
        //> AddModLoop: lda DigitModifier,x       ;load digit amount to increment
        val addAmount = ram.digitModifier[x].toInt()
        //> clc
        //> adc DisplayDigits,y       ;add to current digit
        var result = (displayDigits[y].toInt() and 0xFF) + addAmount
        //> bmi BorrowOne             ;if result is a negative number, branch to subtract
        if (result < 0) {
            //> BorrowOne:  dec DigitModifier-1,x     ;decrement the previous digit, then put $09 in
            if (x > 0) ram.digitModifier[x - 1] = (ram.digitModifier[x - 1] - 1).toByte()
            //> lda #$09                  ;the game timer digit we're currently on to "borrow
            //> bne StoreNewD             ;the one", then do an unconditional branch back
            result = 9
        } else {
            //> cmp #10
            //> bcs CarryOne              ;if digit greater than $09, branch to add
            if (result >= 10) {
                //> CarryOne:   sec                       ;subtract ten from our digit to make it a
                //> sbc #10                   ;proper BCD number, then increment the digit
                result -= 10
                //> inc DigitModifier-1,x     ;preceding current digit to "carry the one" properly
                if (x > 0) ram.digitModifier[x - 1] = (ram.digitModifier[x - 1] + 1).toByte()
                //> jmp StoreNewD             ;go back to just after we branched here
            }
        }
        //> StoreNewD:  sta DisplayDigits,y       ;store as new score or game timer digit
        displayDigits[y] = result.toByte()
        //> dey                       ;move onto next digits in score or game timer
        y = (y - 1).coerceAtLeast(0)
        //> dex                       ;and digit amounts to increment
        // loop variable 'x' handled by for
        //> bpl AddModLoop            ;loop back if we're not done yet
    }

    // Regardless of branch, original code always performs EraseDMods at the end.
    //> EraseDMods: lda #$00                  ;store zero here
    //> ldx #$06                  ;start with the last digit
    //> EraseMLoop: sta DigitModifier-1,x     ;initialize the digit amounts to increment
    //> dex
    //> bpl EraseMLoop            ;do this until they're all reset, then leave
    for (i in 0..5) ram.digitModifier[i] = 0
}

// This routine compares each player's 6-digit score (BCD nybbles stored as digits 0..9)
// against the current top score from least-significant digit to most. If the player's
// score is greater than or equal to the top score, it overwrites the top score display.

fun System.updateTopScore() {
    //> UpdateTopScore:
    //> ldx #$05          ;start with mario's score
    //> jsr TopScoreCheck
    topScoreCheck(ram.playerScoreDisplay)
    //> ldx #$0b          ;now do luigi's score
    topScoreCheck(ram.player2ScoreDisplay)
}

private fun System.topScoreCheck(playerDigits: ByteArray) {
    //> TopScoreCheck:
    //> ldy #$05                 ;start with the lowest digit
    //> sec
    //> GetScoreDiff: lda PlayerScoreDisplay,x ;subtract each player digit from each high score digit
    //> sbc TopScoreDisplay,y    ;from lowest to highest, if any top score digit exceeds
    //> dex                      ;any player digit, borrow will be set until a subsequent
    //> dey                      ;subtraction clears it (player digit is higher than top)
    //> bpl GetScoreDiff

    // In 6502 terms we keep an SBC borrow that propagates across digits.
    // Start with carry set (i.e., borrow = 0), scan from least-significant (index 5) to most (index 0).
    var borrow = 0
    for (i in 5 downTo 0) {
        val player = playerDigits[i].toInt() and 0xFF
        val top = ram.topScoreDisplay[i].toInt() and 0xFF
        val diff = player - top - borrow
        borrow = if (diff < 0) 1 else 0
    }

    //> bcc NoTopSc              ;check to see if borrow is still set, if so, no new high score
    if (borrow == 1) return

    //> inx                      ;increment X and Y once to the start of the score
    //> iny
    //> CopyScore:    lda PlayerScoreDisplay,x ;store player's score digits into high score memory area
    //> sta TopScoreDisplay,y
    //> inx
    //> iny
    //> cpy #$06                 ;do this until we have stored them all
    //> bcc CopyScore
    //> NoTopSc:      rts

    // Copy all six digits from the player into the top score display (most- to least-significant).
    for (i in 0..5) {
        ram.topScoreDisplay[i] = playerDigits[i]
    }
}