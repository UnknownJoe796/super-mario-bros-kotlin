// by Claude - VerticalPipe and GetPipeHeight: renders vertical pipes (warp and decoration)
package com.ivieleague.smbtranslation.areaparser

import com.ivieleague.smbtranslation.*

fun System.verticalPipe() {
    val x = ram.objectOffset

    //> VerticalPipe:
    //> jsr GetPipeHeight
    val pipeInfo = getPipeHeight(x.toInt())
    val row = pipeInfo.row
    val verticalLength = pipeInfo.verticalLength
    var horizOffset = pipeInfo.horizLengthLeft

    //> lda $00                  ;check to see if value was nullified earlier
    //> beq WarpPipe             ;(if d3, the usage control bit of second byte, was set)
    if (areaParserObjId != 0) {
        //> iny; iny; iny; iny   ;add four if usage control bit was not set (decoration pipe)
        horizOffset += 4
    }

    //> WarpPipe: tya            ;save value in stack
    //> pha
    val savedY = horizOffset

    // SMB1: skip piranha plants in W1-1 (AreaNumber|WorldNumber == 0)
    // SMB2J: removed this check — piranha plants spawn in all worlds including W1-1
    val allowPiranha = if (variant == GameVariant.SMB2J) true
        else (ram.areaNumber.toInt() or ram.worldNumber.toInt()) != 0
    //> ldy AreaObjectLength,x   ;if on second column of pipe, branch
    //> beq DrawPipe             ;(because we only need to do this once)
    if (allowPiranha && ram.areaObjectLength[x.toInt()] != 0.toByte()) {
        //> jsr FindEmptyEnemySlot   ;check for an empty moving data buffer space
        //> bcs DrawPipe             ;if not found, too many enemies, thus skip
        val slot = findEmptyEnemySlot()
        if (slot != null) {
            //> lda #PiranhaPlant        ;write piranha plant's value into buffer
            //> jsr SetupPiranhaPlant    ;(SMB2J) / inline setup (SMB1)
            val xPos = (getAreaObjXPosition().toInt() and 0xFF) + 8
            ram.sprObjXPos[1 + slot] = (xPos and 0xFF).toByte()
            ram.sprObjPageLoc[1 + slot] = ((ram.currentPageLoc.toInt() + (if (xPos > 0xFF) 1 else 0)) and 0xFF).toByte()
            ram.sprObjYHighPos[1 + slot] = 1
            ram.enemyFlags[slot] = 1
            ram.sprObjYPos[1 + slot] = getAreaObjYPosition(row.toByte())
            ram.enemyID[slot] = EnemyId.PiranhaPlant.byte
            val savedOffset = ram.objectOffset
            ram.objectOffset = slot.toByte()
            initPiranhaPlant()
            ram.objectOffset = savedOffset
        }
    }

    //> DrawPipe: pla            ;get value saved earlier and use as Y
    //> tay
    val y = savedY
    //> ldx $07                  ;get buffer offset (row)
    val bufX = row
    //> lda VerticalPipeData,y   ;draw the appropriate pipe with the Y we loaded earlier
    //> sta MetatileBuffer,x     ;render the top of the pipe
    ram.metatileBuffer[bufX] = VerticalPipeData.getOrElse(y) { 0x15u }
    //> inx
    //> lda VerticalPipeData+2,y ;render the rest of the pipe
    val bodyMetatile = VerticalPipeData.getOrElse(y + 2) { 0x15u }
    //> ldy $06                  ;subtract one from length and render the part underneath
    //> dey
    //> jmp RenderUnderPart
    renderUnderPart(bodyMetatile, bufX + 1, verticalLength - 1)
}

// by Claude - data class for getPipeHeight results
data class PipeHeightResult(
    val row: Int,             // $07 from GetLrgObjAttrib (starting row in metatile buffer)
    val verticalLength: Int,  // $06 (lower 3 bits of second byte's lower nybble)
    val horizLengthLeft: Int, // Y = AreaObjectLength,x (horizontal length remaining)
)

// by Claude - GetPipeHeight: gets pipe row, vertical length, and horizontal length counter
fun System.getPipeHeight(counterIndex: Int): PipeHeightResult {
    val x = counterIndex.toByte()
    //> GetPipeHeight:
    //> ldy #$01       ;check for length loaded, if not, load
    //> jsr ChkLrgObjFixedLength ;pipe length of 2 (horizontal)
    chkLrgObjFixedLength(counterIndex = x, length = 0x01)
    //> jsr GetLrgObjAttrib
    val attrib = getLrgObjAttrib(x)
    //> tya            ;get saved lower nybble as height
    //> and #$07       ;save only the three lower bits as
    //> sta $06        ;vertical length, then load Y with
    val verticalLength = attrib.length.toInt() and 0x07
    //> ldy AreaObjectLength,x    ;length left over
    val horizLeft = ram.areaObjectLength[x.toInt()].toInt() and 0xFF
    //> rts
    return PipeHeightResult(
        row = attrib.row.toInt() and 0xFF,
        verticalLength = verticalLength,
        horizLengthLeft = horizLeft,
    )
}
