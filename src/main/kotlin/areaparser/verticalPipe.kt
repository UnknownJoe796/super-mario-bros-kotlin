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

    //> lda AreaNumber
    //> ora WorldNumber          ;if at world 1-1, do not add piranha plant ever
    //> beq DrawPipe
    if ((ram.areaNumber.toInt() or ram.worldNumber.toInt()) != 0) {
        //> ldy AreaObjectLength,x   ;if on second column of pipe, branch
        //> beq DrawPipe             ;(because we only need to do this once)
        if (ram.areaObjectLength[x.toInt()] != 0.toByte()) {
            //> jsr FindEmptyEnemySlot   ;check for an empty moving data buffer space
            //> bcs DrawPipe             ;if not found, too many enemies, thus skip
            val slot = findEmptyEnemySlot()
            if (slot != null) {
                //> jsr GetAreaObjXPosition  ;get horizontal pixel coordinate
                //> clc
                //> adc #$08                 ;add eight to put the piranha plant in the center
                val xPos = (getAreaObjXPosition().toInt() and 0xFF) + 8
                //> sta Enemy_X_Position,x   ;store as enemy's horizontal coordinate
                ram.sprObjXPos[1 + slot] = (xPos and 0xFF).toByte()
                //> lda CurrentPageLoc       ;add carry to current page number
                //> adc #$00
                //> sta Enemy_PageLoc,x      ;store as enemy's page coordinate
                ram.sprObjPageLoc[1 + slot] = ((ram.currentPageLoc.toInt() + (if (xPos > 0xFF) 1 else 0)) and 0xFF).toByte()
                //> lda #$01
                //> sta Enemy_Y_HighPos,x
                ram.sprObjYHighPos[1 + slot] = 1
                //> sta Enemy_Flag,x         ;activate enemy flag
                ram.enemyFlags[slot] = 1
                //> jsr GetAreaObjYPosition  ;get piranha plant's vertical coordinate and store here
                //> sta Enemy_Y_Position,x
                ram.sprObjYPos[1 + slot] = getAreaObjYPosition(row.toByte())
                //> lda #PiranhaPlant        ;write piranha plant's value into buffer
                //> sta Enemy_ID,x
                ram.enemyID[slot] = EnemyId.PiranhaPlant.byte
                //> jsr InitPiranhaPlant
                val savedOffset = ram.objectOffset
                ram.objectOffset = slot.toByte()
                initPiranhaPlant()
                ram.objectOffset = savedOffset
            }
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
private data class PipeHeightResult(
    val row: Int,             // $07 from GetLrgObjAttrib (starting row in metatile buffer)
    val verticalLength: Int,  // $06 (lower 3 bits of second byte's lower nybble)
    val horizLengthLeft: Int, // Y = AreaObjectLength,x (horizontal length remaining)
)

// by Claude - GetPipeHeight: gets pipe row, vertical length, and horizontal length counter
private fun System.getPipeHeight(counterIndex: Int): PipeHeightResult {
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
