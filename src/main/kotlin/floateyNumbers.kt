package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.SpriteFlags

// This file contains the Kotlin translation of the SMB "Floatey Numbers" data and routine.
// See smbdism.asm around the labels FloateyNumTileData, ScoreUpdateData, and FloateyNumbersRoutine.
// TODO: Check for correctness

// Data classes to express the meaning of the 6502 data bytes.
/** Pair of tile IDs (left, right) used to render a floating score number. */
data class FloateyNumTile(val left: Byte, val right: Byte)

/**
 * Score update item.
 * - digitIndex: which score digit to modify (0 = least significant digit)
 * - addAmount: value to add to that digit (0..9)
 */
data class ScoreUpdate(val digitIndex: Int, val addAmount: Int)

//> ;data is used as tiles for numbers
//> ;that appear when you defeat enemies
//> FloateyNumTileData:
val floateyNumTileData: List<FloateyNumTile> = listOf(
    //>       .db $ff, $ff ;dummy
    FloateyNumTile(0xff.toByte(), 0xff.toByte()),
    //>       .db $f6, $fb ; "100"
    FloateyNumTile(0xf6.toByte(), 0xfb.toByte()),
    //>       .db $f7, $fb ; "200"
    FloateyNumTile(0xf7.toByte(), 0xfb.toByte()),
    //>       .db $f8, $fb ; "400"
    FloateyNumTile(0xf8.toByte(), 0xfb.toByte()),
    //>       .db $f9, $fb ; "500"
    FloateyNumTile(0xf9.toByte(), 0xfb.toByte()),
    //>       .db $fa, $fb ; "800"
    FloateyNumTile(0xfa.toByte(), 0xfb.toByte()),
    //>       .db $f6, $50 ; "1000"
    FloateyNumTile(0xf6.toByte(), 0x50.toByte()),
    //>       .db $f7, $50 ; "2000"
    FloateyNumTile(0xf7.toByte(), 0x50.toByte()),
    //>       .db $f8, $50 ; "4000"
    FloateyNumTile(0xf8.toByte(), 0x50.toByte()),
    //>       .db $f9, $50 ; "5000"
    FloateyNumTile(0xf9.toByte(), 0x50.toByte()),
    //>       .db $fa, $50 ; "8000"
    FloateyNumTile(0xfa.toByte(), 0x50.toByte()),
    //>       .db $fd, $fe ; "1-UP"
    FloateyNumTile(0xfd.toByte(), 0xfe.toByte()),
)

//> ;high nybble is digit number, low nybble is number to
//> ;add to the digit of the player's score
//> ScoreUpdateData:
val scoreUpdateData: List<ScoreUpdate?> = listOf(
    //>       .db $ff ;dummy
    null,
    //>       .db $41, $42, $44, $45, $48
    ScoreUpdate(digitIndex = 4, addAmount = 1),
    ScoreUpdate(digitIndex = 4, addAmount = 2),
    ScoreUpdate(digitIndex = 4, addAmount = 4),
    ScoreUpdate(digitIndex = 4, addAmount = 5),
    ScoreUpdate(digitIndex = 4, addAmount = 8),
    //>       .db $31, $32, $34, $35, $38, $00
    ScoreUpdate(digitIndex = 3, addAmount = 1),
    ScoreUpdate(digitIndex = 3, addAmount = 2),
    ScoreUpdate(digitIndex = 3, addAmount = 4),
    ScoreUpdate(digitIndex = 3, addAmount = 5),
    ScoreUpdate(digitIndex = 3, addAmount = 8),
    ScoreUpdate(digitIndex = 0, addAmount = 0),
)

/**
 * Kotlin translation of FloateyNumbersRoutine.
 * This routine updates the floating score indicator control/timer and, when appropriate,
 * writes two sprite entries (left/right tiles) at the provided OAM base index.
 *
 * Notes:
 * - The original uses X as the object index, and fetches enemy/OAM offsets accordingly.
 *   Here we operate on the single floateyNum* fields in GameRam and take an explicit oamBaseIndex.
 * - Score update and sound effects are represented by placeholders; full systems are not yet wired.
 */
fun System.floateyNumbersRoutine(oamBaseIndex: Int) {
    //> FloateyNumbersRoutine:
    //>               lda FloateyNum_Control,x     ;load control for floatey number
    //>               beq EndExitOne               ;if zero, branch to leave
    if (ram.floateyNumControl == 0.toByte()) return
    //>               cmp #$0b                     ;if less than $0b, branch
    //>               bcc ChkNumTimer
    if (ram.floateyNumControl.toInt() and 0xFF >= 0x0b) {
        //>               lda #$0b                     ;otherwise set to $0b, thus keeping
        //>               sta FloateyNum_Control,x     ;it in range
        ram.floateyNumControl = 0x0b.toByte()
    }
    //> ChkNumTimer:  tay                          ;use as Y
    val yIndex: Int = ram.floateyNumControl.toInt() and 0xFF
    //>               lda FloateyNum_Timer,x       ;check value here
    //>               bne DecNumTimer              ;if nonzero, branch ahead
    if (ram.floateyNumTimer == 0.toByte()) {
        //>               sta FloateyNum_Control,x     ;initialize floatey number control and leave
        // In 6502, A is zero here; we mirror the intent by clearing the control.
        ram.floateyNumControl = 0x00
        //>               rts
        return
    }
    //> DecNumTimer:  dec FloateyNum_Timer,x       ;decrement value here
    ram.floateyNumTimer = (ram.floateyNumTimer - 1).toByte()
    // For the following compare, A would hold the previous value; emulate the compare against 0x2b
    val timerAfterDec = ram.floateyNumTimer.toInt() and 0xFF
    //>               cmp #$2b                     ;if not reached a certain point, branch
    //>               bne ChkTallEnemy
    if (timerAfterDec == 0x2b) {
        //>               cpy #$0b                     ;check offset for $0b
        //>               bne LoadNumTiles             ;branch ahead if not found
        if (yIndex == 0x0b) {
            //>               inc NumberofLives            ;give player one extra life (1-up)
            ram.numberofLives = (ram.numberofLives + 1).toByte()
            //>               lda #Sfx_ExtraLife
            //>               sta Square2SoundQueue        ;and play the 1-up sound
            // Not wired: sound system queues. This is a placeholder location in APU.
            // apu.queueSquare2(Sfx.ExtraLife) // TODO when sound is available
        }
        //> LoadNumTiles: lda ScoreUpdateData,y        ;load point value here
        val scoreUpdate: ScoreUpdate? = scoreUpdateData.getOrNull(yIndex)
        if (scoreUpdate != null) {
            //>               lsr                          ;move high nybble to low
            //>               lsr
            //>               lsr
            //>               lsr
            //>               tax                          ;use as X offset, essentially the digit
            val digit = scoreUpdate.digitIndex
            //>               lda ScoreUpdateData,y        ;load again and this time
            //>               and #%00001111               ;mask out the high nybble
            val addAmount = scoreUpdate.addAmount
            //>               sta DigitModifier,x          ;store as amount to add to the digit
            //>               jsr AddToScore               ;update the score accordingly
            // We do not yet model score digits; store a simple stub of last update.
            ram.lastScoreDigitIndex = digit.toByte()
            ram.lastScoreDigitAdd = addAmount.toByte()
            // TODO: Implement AddToScore and real score digits when score system is modeled.
        }
    }
    //> ChkTallEnemy: ldy Enemy_SprDataOffset,x    ;get OAM data offset for enemy object
    // Enemy/OAM offset resolution depends on enemy systems; here we take oamBaseIndex
    var oamY = oamBaseIndex

    //> FloateyPart:  lda FloateyNum_Y_Pos,x       ;get vertical coordinate for
    //>               cmp #$18                     ;floatey number, if coordinate in the
    //>               bcc SetupNumSpr              ;status bar, branch
    val currentY = ram.floateyNumYPos.toInt() and 0xFF
    if (currentY >= 0x18) {
        //>               sbc #$01
        //>               sta FloateyNum_Y_Pos,x       ;otherwise subtract one and store as new
        ram.floateyNumYPos = (currentY - 1).toByte()
    }
    //> SetupNumSpr:  lda FloateyNum_Y_Pos,x       ;get vertical coordinate
    //>               sbc #$08                     ;subtract eight and dump into the
    val yTop = ((ram.floateyNumYPos.toInt() and 0xFF) - 0x08).coerceIn(0, 0xFF)
    //>               jsr DumpTwoSpr               ;left and right sprite's Y coordinates
    // Write Y to two sprites at oamBaseIndex and oamBaseIndex+1 (i.e., 2 sprite slots apart in OAM bytes)
    val leftSprite = ram.sprites.getOrNull(oamY)
    val rightSprite = ram.sprites.getOrNull(oamY + 1)
    leftSprite?.y = yTop.toUByte()
    rightSprite?.y = yTop.toUByte()

    //>               lda FloateyNum_X_Pos,x       ;get horizontal coordinate
    val baseX = ram.floateyNumXPos.toInt() and 0xFF
    //>               sta Sprite_X_Position,y      ;store into X coordinate of left sprite
    leftSprite?.x = baseX.toUByte()
    //>               clc
    //>               adc #$08                     ;add eight pixels and store into X
    //>               sta Sprite_X_Position+4,y    ;coordinate of right sprite
    rightSprite?.x = (baseX + 0x08).coerceIn(0, 0xFF).toUByte()

    //>               lda #$02
    //>               sta Sprite_Attributes,y      ;set palette control in attribute bytes
    //>               sta Sprite_Attributes+4,y    ;of left and right sprites
    val attr = SpriteFlags(palette = 0x02)
    leftSprite?.attributes = attr
    rightSprite?.attributes = attr

    //>               lda FloateyNum_Control,x
    //>               asl                          ;multiply our floatey number control by 2
    //>               tax                          ;and use as offset for look-up table
    val tileIndex = (ram.floateyNumControl.toInt() and 0xFF) * 2
    //>               lda FloateyNumTileData,x
    //>               sta Sprite_Tilenumber,y      ;display first half of number of points
    //>               lda FloateyNumTileData+1,x
    //>               sta Sprite_Tilenumber+4,y    ;display the second half
    if (tileIndex + 1 < floateyNumTileData.size * 2) {
        val pairIndex = tileIndex / 2
        val pair = floateyNumTileData[pairIndex]
        leftSprite?.tilenumber = pair.left
        rightSprite?.tilenumber = pair.right
    }

    //>               rts
}
