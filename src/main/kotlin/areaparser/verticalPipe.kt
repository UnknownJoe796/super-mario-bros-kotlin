package com.ivieleague.smbtranslation.areaparser

import com.ivieleague.smbtranslation.System


//> VerticalPipeData:
//> .db $11, $10 ;used by pipes that lead somewhere
//> .db $15, $14
//> .db $13, $12 ;used by decoration pipes
//> .db $15, $14

fun System.verticalPipe(): Unit {

    //> VerticalPipe:
    //> jsr GetPipeHeight
    //> lda $00                  ;check to see if value was nullified earlier
    //> beq WarpPipe             ;(if d3, the usage control bit of second byte, was set)
    //> iny
    //> iny
    //> iny
    //> iny                      ;add four if usage control bit was not set
    //> WarpPipe: tya                      ;save value in stack
    //> pha
    //> lda AreaNumber
    //> ora WorldNumber          ;if at world 1-1, do not add piranha plant ever
    //> beq DrawPipe
    //> ldy AreaObjectLength,x   ;if on second column of pipe, branch
    //> beq DrawPipe             ;(because we only need to do this once)
    //> jsr FindEmptyEnemySlot   ;check for an empty moving data buffer space
    //> bcs DrawPipe             ;if not found, too many enemies, thus skip
    //> jsr GetAreaObjXPosition  ;get horizontal pixel coordinate
    //> clc
    //> adc #$08                 ;add eight to put the piranha plant in the center
    //> sta Enemy_X_Position,x   ;store as enemy's horizontal coordinate
    //> lda CurrentPageLoc       ;add carry to current page number
    //> adc #$00
    //> sta Enemy_PageLoc,x      ;store as enemy's page coordinate
    //> lda #$01
    //> sta Enemy_Y_HighPos,x
    //> sta Enemy_Flag,x         ;activate enemy flag
    //> jsr GetAreaObjYPosition  ;get piranha plant's vertical coordinate and store here
    //> sta Enemy_Y_Position,x
    //> lda #PiranhaPlant        ;write piranha plant's value into buffer
    //> sta Enemy_ID,x
    //> jsr InitPiranhaPlant
    //> DrawPipe: pla                      ;get value saved earlier and use as Y
    //> tay
    //> ldx $07                  ;get buffer offset
    //> lda VerticalPipeData,y   ;draw the appropriate pipe with the Y we loaded earlier
    //> sta MetatileBuffer,x     ;render the top of the pipe
    //> inx
    //> lda VerticalPipeData+2,y ;render the rest of the pipe
    //> ldy $06                  ;subtract one from length and render the part underneath
    //> dey
    //> jmp RenderUnderPart
}

/**
 * @return The pipe's length
 */
private data class PipeHeight(val height: Byte, val length: Byte)
private fun System.getPipeHeight(): PipeHeight {
    //> GetPipeHeight:
    //> ldy #$01       ;check for length loaded, if not, load
    //> jsr ChkLrgObjFixedLength ;pipe length of 2 (horizontal)
    //> jsr GetLrgObjAttrib
    //> tya            ;get saved lower nybble as height
    //> and #$07       ;save only the three lower bits as
    //> sta $06        ;vertical length, then load Y with
    //> ldy AreaObjectLength,x    ;length left over
    //> rts
    return PipeHeight(0, 0)
}
private fun System.findEmptyEnemySlot(): Unit {
    //> FindEmptyEnemySlot:
    //> ldx #$00          ;start at first enemy slot
    //> EmptyChkLoop: clc               ;clear carry flag by default
    //> lda Enemy_Flag,x  ;check enemy buffer for nonzero
    //> beq ExitEmptyChk  ;if zero, leave
    //> inx
    //> cpx #$05          ;if nonzero, check next value
    //> bne EmptyChkLoop
    //> ExitEmptyChk: rts               ;if all values nonzero, carry flag is set
}