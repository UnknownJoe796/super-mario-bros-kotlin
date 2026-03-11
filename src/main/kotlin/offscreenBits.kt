// by Claude - Offscreen detection chain
// Translates GetXOffscreenBits, GetYOffscreenBits, DividePDiff, RunOffscrBitsSubs,
// GetOffScreenBitsSet, and all Get*OffscreenBits wrapper routines.
package com.ivieleague.smbtranslation

//> XOffscreenBitsData:
//> .db $7f, $3f, $1f, $0f, $07, $03, $01, $00
//> .db $80, $c0, $e0, $f0, $f8, $fc, $fe, $ff
// Indices 0-7: right side, indices 8-15: left side
private val xOffscreenBitsData = intArrayOf(
    0x7f, 0x3f, 0x1f, 0x0f, 0x07, 0x03, 0x01, 0x00,
    0x80, 0xc0, 0xe0, 0xf0, 0xf8, 0xfc, 0xfe, 0xff
)

//> DefaultXOnscreenOfs:
//> .db $07, $0f, $07
private val defaultXOnscreenOfs = intArrayOf(0x07, 0x0f, 0x07)

//> YOffscreenBitsData:
//> .db $00, $08, $0c, $0e
//> .db $0f, $07, $03, $01
//> .db $00
private val yOffscreenBitsData = intArrayOf(
    0x00, 0x08, 0x0c, 0x0e, 0x0f, 0x07, 0x03, 0x01, 0x00
)

//> DefaultYOnscreenOfs:
//> .db $04, $00, $04
private val defaultYOnscreenOfs = intArrayOf(0x04, 0x00, 0x04)

//> HighPosUnitData:
//> .db $ff, $00
private val highPosUnitData = intArrayOf(0xff, 0x00)

/**
 * DividePDiff: Subdivides pixel difference to produce a graduated offset
 * into the offscreen bits data table.
 */
private fun dividePDiff(pixelDiff: Int, preset: Int, adder: Int, side: Int, currentOffset: Int): Int {
    //> DividePDiff:
    //> sta $05       ;store adder
    //> lda $07       ;get pixel difference
    //> cmp $06       ;compare to preset value
    //> bcs ExDivPD   ;if pixel_diff >= preset, return with current offset
    if ((pixelDiff and 0xFF) >= preset) return currentOffset
    //> lsr; lsr; lsr ;divide by eight
    //> and #$07      ;mask out all but 3 LSB
    var offset = ((pixelDiff and 0xFF) shr 3) and 0x07
    //> cpy #$01      ;right/top side?
    //> bcs SetOscrO  ;if so, use divided value directly
    if (side < 1) {
        //> adc $05   ;left/bottom side: add adder for second half of table
        offset += adder
    }
    //> SetOscrO: tax
    return offset
}

/**
 * Determines X-axis offscreen status by checking both right and left screen edges.
 * Returns a raw offscreen bits value from the XOffscreenBitsData table.
 */
fun System.getXOffscreenBits(sprObjOffset: Int): Int {
    //> GetXOffscreenBits:
    //> stx $04                     ;save position in buffer
    //> ldy #$01                    ;start with right side of screen
    for (side in 1 downTo 0) {
        //> XOfsLoop: lda ScreenEdge_X_Pos,y
        //> sec
        //> sbc SprObject_X_Position,x  ;edge pixel - object pixel
        //> sta $07
        val edgeX = if (side == 1) ram.screenRightXPos else ram.screenLeftXPos
        val objX = ram.sprObjXPos[sprObjOffset]
        val pixelSubtract = (edgeX.toInt() and 0xFF) - (objX.toInt() and 0xFF)
        val borrow = if (pixelSubtract < 0) 1 else 0
        val pixelDiff = pixelSubtract and 0xFF

        //> lda ScreenEdge_PageLoc,y
        //> sbc SprObject_PageLoc,x     ;edge page - object page (with borrow)
        val edgePage = if (side == 1) ram.screenRightPageLoc else ram.screenLeftPageLoc
        val objPage = ram.sprObjPageLoc[sprObjOffset]
        val pageDiff = ((edgePage.toInt() and 0xFF) - (objPage.toInt() and 0xFF) - borrow).toByte()

        //> ldx DefaultXOnscreenOfs,y
        var offset = defaultXOnscreenOfs[side]
        //> cmp #$00
        //> bmi XLdBData                ;if page diff negative, use default offset
        if (pageDiff >= 0) {
            //> ldx DefaultXOnscreenOfs+1,y
            offset = defaultXOnscreenOfs[side + 1]
            //> cmp #$01
            //> bpl XLdBData            ;if page diff >= 1, use alternate offset
            if (pageDiff < 1) {
                //> lda #$38; sta $06; lda #$08; jsr DividePDiff
                offset = dividePDiff(pixelDiff, 0x38, 0x08, side, offset)
            }
        }

        //> XLdBData: lda XOffscreenBitsData,x
        val bits = xOffscreenBitsData[offset]
        //> ldx $04
        //> cmp #$00; bne ExXOfsBS      ;if bits nonzero, done
        if (bits != 0) return bits
        //> dey; bpl XOfsLoop           ;otherwise check other side
    }
    //> ExXOfsBS: rts
    return 0
}

/**
 * Determines Y-axis offscreen status by checking both top and bottom screen edges.
 * Returns a raw offscreen bits value from the YOffscreenBitsData table.
 */
fun System.getYOffscreenBits(sprObjOffset: Int): Int {
    //> GetYOffscreenBits:
    //> stx $04
    //> ldy #$01                     ;start with top of screen
    for (side in 1 downTo 0) {
        //> YOfsLoop: lda HighPosUnitData,y
        //> sec
        //> sbc SprObject_Y_Position,x
        //> sta $07
        val edgeY = highPosUnitData[side]
        val objY = ram.sprObjYPos[sprObjOffset].toInt() and 0xFF
        val pixelSubtract = edgeY - objY
        val borrow = if (pixelSubtract < 0) 1 else 0
        val pixelDiff = pixelSubtract and 0xFF

        //> lda #$01
        //> sbc SprObject_Y_HighPos,x
        val objYHigh = ram.sprObjYHighPos[sprObjOffset].toInt() and 0xFF
        val pageDiff = (0x01 - objYHigh - borrow).toByte()

        //> ldx DefaultYOnscreenOfs,y
        var offset = defaultYOnscreenOfs[side]
        //> cmp #$00; bmi YLdBData
        if (pageDiff >= 0) {
            //> ldx DefaultYOnscreenOfs+1,y
            offset = defaultYOnscreenOfs[side + 1]
            //> cmp #$01; bpl YLdBData
            if (pageDiff < 1) {
                //> lda #$20; sta $06; lda #$04; jsr DividePDiff
                offset = dividePDiff(pixelDiff, 0x20, 0x04, side, offset)
            }
        }

        //> YLdBData: lda YOffscreenBitsData,x
        val bits = yOffscreenBitsData[offset]
        //> ldx $04; cmp #$00; bne ExYOfsBS
        if (bits != 0) return bits
        //> dey; bpl YOfsLoop
    }
    //> ExYOfsBS: rts
    return 0
}

/**
 * Gets combined X and Y offscreen bits for an object and stores in the condensed offscrBits array.
 * Result byte: high nybble = Y offscreen info, low nybble = X offscreen info.
 */
fun System.getOffScreenBitsSet(sprObjOffset: Int, condensedOffset: Int) {
    //> GetOffScreenBitsSet:
    //> tya; pha                     ;save condensed offset on stack
    //> jsr RunOffscrBitsSubs
    //> RunOffscrBitsSubs:
    //> jsr GetXOffscreenBits
    val xBits = getXOffscreenBits(sprObjOffset)
    //> lsr; lsr; lsr; lsr          ;move high nybble to low
    //> sta $00
    val xLowNybble = (xBits shr 4) and 0x0F
    //> jmp GetYOffscreenBits
    val yBits = getYOffscreenBits(sprObjOffset)
    //> (back in GetOffScreenBitsSet)
    //> asl; asl; asl; asl          ;move low nybble to high
    //> ora $00                     ;combine with X bits
    val combined = ((yBits shl 4) and 0xF0) or xLowNybble
    //> pla; tay                    ;restore condensed offset
    //> lda $00
    //> sta SprObject_OffscrBits,y
    ram.offscrBits[condensedOffset] = combined.toByte()
    //> ldx ObjectOffset; rts
}

// --- Wrapper functions for each object type ---

fun System.getPlayerOffscreenBits() {
    //> GetPlayerOffscreenBits:
    //> ldx #$00; ldy #$00; jmp GetOffScreenBitsSet
    getOffScreenBitsSet(sprObjOffset = 0, condensedOffset = 0)
}

fun System.getFireballOffscreenBits() {
    //> GetFireballOffscreenBits:
    //> ldy #$00; jsr GetProperObjOffset  ;X += 7
    //> ldy #$02; jmp GetOffScreenBitsSet
    getOffScreenBitsSet(sprObjOffset = ram.objectOffset.toInt() + 7, condensedOffset = 2)
}

fun System.getBubbleOffscreenBits() {
    //> GetBubbleOffscreenBits:
    //> ldy #$01; jsr GetProperObjOffset  ;X += 22
    //> ldy #$03; jmp GetOffScreenBitsSet
    getOffScreenBitsSet(sprObjOffset = ram.objectOffset.toInt() + 22, condensedOffset = 3)
}

fun System.getMiscOffscreenBits() {
    //> GetMiscOffscreenBits:
    //> ldy #$02; jsr GetProperObjOffset  ;X += 13
    //> ldy #$06; jmp GetOffScreenBitsSet
    getOffScreenBitsSet(sprObjOffset = ram.objectOffset.toInt() + 13, condensedOffset = 6)
}

fun System.getEnemyOffscreenBits() {
    //> GetEnemyOffscreenBits:
    //> lda #$01; ldy #$01; jmp SetOffscrBitsOffset  ;X = 1 + ObjectOffset
    getOffScreenBitsSet(sprObjOffset = 1 + ram.objectOffset.toInt(), condensedOffset = 1)
}

fun System.getBlockOffscreenBits() {
    //> GetBlockOffscreenBits:
    //> lda #$09; ldy #$04  ;X = 9 + ObjectOffset
    //> (falls into SetOffscrBitsOffset then GetOffScreenBitsSet)
    getOffScreenBitsSet(sprObjOffset = 9 + ram.objectOffset.toInt(), condensedOffset = 4)
}
