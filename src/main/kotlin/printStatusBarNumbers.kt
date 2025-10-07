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