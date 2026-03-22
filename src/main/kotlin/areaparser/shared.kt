// by Claude - area parser shared helper functions
package com.ivieleague.smbtranslation.areaparser

import com.ivieleague.smbtranslation.*
import com.ivieleague.smbtranslation.utils.get
import com.ivieleague.smbtranslation.utils.set
import kotlin.experimental.and

// by Claude - fixed chkLrgObjLength to use length from GetLrgObjAttrib (not a separate param)
fun System.chkLrgObjLength(x: Byte): Pair<LrgObjAttribInfo, LrgObjFixedLength> {
    //> ChkLrgObjLength:
    //> jsr GetLrgObjAttrib     ;get row location and size (length if branched to from here)
    val a = getLrgObjAttrib(x)
    //> (falls through to ChkLrgObjFixedLength with Y from GetLrgObjAttrib)
    val l = chkLrgObjFixedLength(x, a.length)
    return a to l
}

fun System.chkLrgObjFixedLength(counterIndex: Byte, length: Byte): LrgObjFixedLength {
    //> ChkLrgObjFixedLength:
    //> lda AreaObjectLength,x  ;check for set length counter
    //> clc                     ;clear carry flag for not just starting
    var justStarting = false
    //> bpl LenSet              ;if counter not set, load it, otherwise leave alone
    if (ram.areaObjectLength[counterIndex] < 0.toByte()) {
        //> tya                     ;save length into length counter
        //> sta AreaObjectLength,x
        ram.areaObjectLength[counterIndex] = length
        //> sec                     ;set carry flag if just starting
        justStarting = true
    }
    //> LenSet: rts
    return LrgObjFixedLength(justStarting, length)
}

data class LrgObjFixedLength(val justStarting: Boolean, val length: Byte)


/**
 * Gets row location (stored in $07) and length/height (lower nybble of second byte, returned in Y).
 */
fun System.getLrgObjAttrib(x: Byte): LrgObjAttribInfo {
    //> GetLrgObjAttrib:
    //> ldy AreaObjOffsetBuffer,x ;get offset saved from area obj decoding routine
    val y = ram.areaObjOffsetBuffer[x].toInt() and 0xFF
    //> lda (AreaData),y          ;get first byte of level object
    val a = ram.areaData!![y]
    //> and #%00001111
    //> sta $07                   ;save row location
    val row = a and 0b1111
    //> iny
    //> lda (AreaData),y          ;get next byte, save lower nybble (length or height)
    //> and #%00001111            ;as Y, then leave
    //> tay
    val aOutput = ram.areaData!![y + 1] and 0b00001111
    //> rts
    return LrgObjAttribInfo(row, aOutput)
}

data class LrgObjAttribInfo(
    /**
     * Stored in the $07 memory location.
     */
    val row: Byte,
    /**
     * Stored in the Y register.
     */
    val length: Byte,
)

// by Claude - RenderUnderPart: renders a metatile downward in the metatile buffer
//> RenderUnderPart:
fun System.renderUnderPart(metatile: UByte, startX: Int, lengthY: Int) {
    //> sty AreaObjectHeight  ;store vertical length to render
    ram.areaObjectHeight = lengthY.toByte()
    var x = startX
    var y = lengthY
    // NES overflow: MetatileBuffer index 13 maps to $06AE (hammerEnemyOffsets[0])
    fun readMT(idx: Int): UByte = if (idx < ram.metatileBuffer.size) ram.metatileBuffer[idx]
        else if (idx == 0x0d) ram.hammerEnemyOffsets[0].toUByte() else 0.toUByte()
    fun writeMT(idx: Int, v: UByte) { if (idx < ram.metatileBuffer.size) ram.metatileBuffer[idx] = v
        else if (idx == 0x0d) ram.hammerEnemyOffsets[0] = v.toByte() }
    while (true) {
        //> ldy MetatileBuffer,x  ;check current spot to see if there's something
        val current = readMT(x)
        //> beq DrawThisRow       ;we need to keep, if nothing, go ahead
        if (current == 0.toUByte()) {
            //> DrawThisRow: sta MetatileBuffer,x
            writeMT(x, metatile)
        } else {
            //> cpy #$17; beq WaitOneRow  ;if middle part (tree ledge), wait until next row
            //> cpy #$1a; beq WaitOneRow  ;if middle part (mushroom ledge), wait until next row
            //> cpy #$c0; beq DrawThisRow ;if question block w/ coin, overwrite
            //> cpy #$c0; bcs WaitOneRow  ;if any other metatile with palette 3, wait until next row
            //> cpy #$54; bne DrawThisRow ;if cracked rock terrain, overwrite
            //> cmp #$50; beq WaitOneRow  ;if stem top of mushroom, wait until next row
            val cv = current.toInt() and 0xFF
            when {
                cv == 0x17 -> { /* WaitOneRow - tree ledge middle */ }
                cv == 0x1a -> { /* WaitOneRow - mushroom ledge middle */ }
                cv == 0xc0 -> writeMT(x, metatile)
                cv > 0xc0 -> { /* WaitOneRow - palette 3 metatile */ }
                cv != 0x54 -> writeMT(x, metatile)
                // cv == 0x54 (cracked rock terrain):
                metatile.toInt() and 0xFF == 0x50 -> { /* WaitOneRow - stem top of mushroom */ }
                else -> writeMT(x, metatile)
            }
        }
        //> WaitOneRow: inx
        x++
        //> cpx #$0d; bcs ExitUPartR  ;stop rendering if we're at the bottom of the screen
        if (x >= 0x0d) return
        //> ldy AreaObjectHeight; dey; bpl RenderUnderPart
        y--
        if (y < 0) return
        //> ExitUPartR: rts
    }
}

// by Claude - GetAreaObjXPosition: get horizontal pixel coordinate from current column position
//> GetAreaObjXPosition:
fun System.getAreaObjXPosition(): Byte {
    //> lda CurrentColumnPos    ;multiply current offset where we're at by 16
    //> asl; asl; asl; asl     ;to obtain horizontal pixel coordinate
    //> rts
    return ((ram.currentColumnPos.toInt() and 0xFF) shl 4 and 0xFF).toByte()
}

// by Claude - GetAreaObjYPosition: get vertical pixel coordinate from row ($07)
//> GetAreaObjYPosition:
fun System.getAreaObjYPosition(row: Byte): Byte {
    //> lda $07  ;multiply value by 16
    //> asl; asl; asl; asl
    //> clc
    //> adc #32  ;add 32 pixels for the status bar
    //> rts
    return (((row.toInt() and 0xFF) shl 4) + 32 and 0xFF).toByte()
}

// by Claude - FindEmptyEnemySlot: finds an empty slot in enemy object buffer
// Returns the slot index (0-4), or null if all slots are full (carry set in assembly)
//> FindEmptyEnemySlot:
fun System.findEmptyEnemySlot(): Int? {
    //> ldx #$00          ;start at first enemy slot
    for (x in 0 until 5) {
        //> clc               ;clear carry flag by default
        //> lda Enemy_Flag,x  ;check enemy buffer for nonzero
        //> beq ExitEmptyChk  ;if zero, leave
        if (ram.enemyFlags[x] == 0.toByte()) return x
        //> inx; cpx #$05; bne EmptyChkLoop
    }
    //> ExitEmptyChk: rts  ;if all values nonzero, carry flag is set
    return null
}

// by Claude - KillEnemies: deactivate all enemies with the given identifier
//> KillEnemies:
fun System.killEnemies(identifier: Byte) {
    //> sta $00           ;store identifier here
    //> lda #$00
    //> ldx #$04          ;check for identifier in enemy object buffer
    for (x in 4 downTo 0) {
        //> ldy Enemy_ID,x; cpy $00; bne NoKillE
        if (ram.enemyID[x] == identifier) {
            //> sta Enemy_Flag,x  ;if found, deactivate enemy object flag
            ram.enemyFlags[x] = 0
        }
        //> dex; bpl KillELoop
    }
    //> rts
}

// by Claude - VerticalPipeData: shared between verticalPipe.kt and areaParser.kt (IntroPipe)
//> VerticalPipeData:
//>   .db $11, $10  ;used by pipes that lead somewhere
//>   .db $15, $14
//>   .db $13, $12  ;used by decoration pipes
//>   .db $15, $14
val VerticalPipeData = ubyteArrayOf(
    0x11u, 0x10u, 0x15u, 0x14u,
    0x13u, 0x12u, 0x15u, 0x14u,
)