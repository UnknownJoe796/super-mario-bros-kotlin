package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.chr.OriginalRom

//> ;-------------------------------------------------------------------------------------
//> ;$00 - temp vram buffer offset
//> ;$01 - temp metatile buffer offset
//> ;$02 - temp metatile graphics table offset
//> ;$03 - used to store attribute bits
//> ;$04 - used to determine attribute table row
//> ;$05 - used to determine attribute table column
//> ;$06 - metatile graphics table address low
//> ;$07 - metatile graphics table address high

// I won't pretend to have reviewed this one from the AI too well, but it looks right to me.
fun System.renderAreaGraphics() {
    //> RenderAreaGraphics:
    //> lda CurrentColumnPos         ;store LSB of where we're at
    //> and #$01
    //> sta $05
    var attribColumn = (ram.currentColumnPos.toInt() and 0x01) // $05

    //> ldy VRAM_Buffer2_Offset      ;store vram buffer offset
    //> sty $00
    // In our high-level model we append to vRAMBuffer2 rather than managing raw offsets.

    //> lda CurrentNTAddr_Low        ;get current name table address we're supposed to render
    //> sta VRAM_Buffer2+1,y
    //> lda CurrentNTAddr_High
    //> sta VRAM_Buffer2,y
    // Translate current NT address into nametable/x/y for high-level PPU API
    val ntAddr = ((ram.currentNTAddrHigh.toInt() and 0xFF) shl 8) or (ram.currentNTAddrLow.toInt() and 0xFF)
    val ntIndex = ((ntAddr - 0x2000) / 0x400) and 0x03
    val startInNt = (ntAddr - 0x2000) % 0x400
    val startX = startInNt % 32
    val startY = startInNt / 32

    //> lda #$9a                     ;store length byte of 26 here with d7 set
    //> sta VRAM_Buffer2+2,y         ;to increment by 32 (in columns)
    // Bit7 set means vertical stepping; length=0x1A bytes (13 rows * 2 bytes each)

    //> lda #$00                     ;init attribute row
    //> sta $04
    var attribRow = 0 // $04
    //> tax
    var metatileBufOffset = 0 // X

    // We will build a sequence of 26 tile IDs written vertically (increment by 32 between bytes).
    val patternIds = ArrayList<Int>(26)

    // DrawMTLoop:
    while (true) {
        //> DrawMTLoop: stx $01                      ;store init value of 0 or incremented offset for buffer
        val currentRowOffset = metatileBufOffset // $01
        //> lda MetatileBuffer,x         ;get first metatile number, and mask out all but 2 MSB
        val metatileVal = ram.metatileBuffer.getOrNull(metatileBufOffset)?.toInt()?.and(0xFF) ?: 0
        //> and #%11000000
        var attribBits = (metatileVal and 0xC0) // $03 stores raw bits before rotation
        //> sta $03                      ;store attribute table bits here
        var attribWork = attribBits // $03
        //> asl                          ;note that metatile format is:
        //> rol                          ;%xx000000 - attribute table bits,
        //> rol                          ;%00xxxxxx - metatile number
        //> tay                          ;rotate bits to d1-d0 and use as offset here
        val attributeGroup = ((attribBits shl 2) and 0xFF) ushr 6 // bring bits into d1-d0 -> 0..3

        //> lda MetatileGraphics_Low,y   ;get address to graphics table from here
        //> sta $06
        //> lda MetatileGraphics_High,y
        //> sta $07
        // High-level port uses resolveMetatileTilePair instead of raw address tables.

        //> lda MetatileBuffer,x         ;get metatile number again
        //> asl                          ;multiply by 4 and use as tile offset
        //> asl
        val tileBaseOffset = ((metatileVal and 0x3F) shl 2) and 0xFF
        //> sta $02
        var tileOffset = tileBaseOffset // $02
        //> lda AreaParserTaskNum        ;get current task number for level processing and
        //> and #%00000001               ;mask out all but LSB, then invert LSB, multiply by 2
        //> eor #%00000001               ;to get the correct column position in the metatile,
        //> asl                          ;then add to the tile offset so we can draw either side
        //> adc $02                      ;of the metatiles
        val columnSide = (((ram.areaParserTaskNum.toInt() and 0x01) xor 0x01) shl 1) and 0x02
        tileOffset = (tileOffset + columnSide) and 0xFF
        //> tay
        //> ldx $00                      ;use vram buffer offset from before as X
        // We keep vramBuf2Offset only conceptually.

        // Resolve tile numbers for this metatile half-column
        // mtVal layout: %aabbcccc where aa=attribute group, cccc=metatile index low bits
        val attributeBits = (metatileVal ushr 6) and 0x03
        val metatileIndex = metatileVal and 0x3F
        val metatile = metatileGraphics[attributeBits][metatileIndex]
        val metatileUsesLeft = columnSide ushr 1 == 0
        val tileTop = if(metatileUsesLeft) metatile.topLeft else metatile.topRight
        val tileBottom = if(metatileUsesLeft) metatile.bottomLeft else metatile.bottomRight
        //> lda ($06),y
        //> sta VRAM_Buffer2+3,x         ;get first tile number (top left or top right) and store
        patternIds.add(tileTop)
        //> iny
        //> lda ($06),y                  ;now get the second (bottom left or bottom right) and store
        //> sta VRAM_Buffer2+4,x
        patternIds.add(tileBottom)

        //> ldy $04                      ;get current attribute row
        // In our model, attribRow indexes a temporary attribute buffer row.
        //> lda $05                      ;get LSB of current column where we're at, and
        //> bne RightCheck               ;branch if set (clear = left attrib, set = right)
        if (attribColumn == 0) {
            // Left half of attribute byte
            //> lda $01                      ;get current row we're rendering
            //> lsr                          ;branch if LSB set (clear = top left, set = bottom left)
            //> bcs LLeft
            val isBottom = (currentRowOffset and 0x01) != 0
            if (!isBottom) {
                //> rol $03                      ;rotate attribute bits 3 to the left
                //> rol $03                      ;thus in d1-d0, for upper left square
                //> rol $03
                // Place attribute bits into bits 0-1
                val bits = (attributeGroup and 0x03)
                attribWork = bits
                //> jmp SetAttrib
            } else {
                //> LLeft:      lsr $03                      ;shift attribute bits 2 to the right
                //> lsr $03                      ;thus in d5-d4 for lower left square
                // Place into bits 4-5
                val bits = (attributeGroup and 0x03) shl 4
                attribWork = bits
                //> NextMTRow:  inc $04                      ;move onto next attribute row
                attribRow += 1
            }
        } else {
            //> RightCheck: lda $01                      ;get LSB of current row we're rendering
            //> lsr                          ;branch if set (clear = top right, set = bottom right)
            val isBottom = (currentRowOffset and 0x01) != 0
            //> bcs NextMTRow
            if (!isBottom) {
                //> lsr $03                      ;shift attribute bits 4 to the right
                //> lsr $03                      ;thus in d3-d2, for upper right square
                //> lsr $03
                //> lsr $03
                //> jmp SetAttrib
                // Place into bits 2-3
                val bits = (attributeGroup and 0x03) shl 2
                attribWork = bits
            } else {
                //> LLeft:      lsr $03                      ;shift attribute bits 2 to the right
                //> lsr $03                      ;thus in d5-d4 for lower left square
                // bottom-right -> bits 6-7
                val bits = (attributeGroup and 0x03) shl 6
                attribWork = bits
                //> NextMTRow:  inc $04                      ;move onto next attribute row
                attribRow += 1
            }
        }

        //> SetAttrib:  lda AttributeBuffer,y        ;get previously saved bits from before
        val prevAttrib = ram.attributeBuffer.getOrNull(attribRow) ?: 0
        //> ora $03                      ;if any, and put new bits, if any, onto
        val combined = (prevAttrib.toInt() or (attribWork and 0xFF)) and 0xFF
        //> sta AttributeBuffer,y        ;the old, and store
        if (attribRow in ram.attributeBuffer.indices) ram.attributeBuffer[attribRow] = combined.toByte()

        //> inc $00                      ;increment vram buffer offset by 2
        //> inc $00
        // Again, not relevant since we model the VRAM buffer at a higher level.

        //> ldx $01                      ;get current gfx buffer row, and check for
        //> inx                          ;the bottom of the screen
        //> cpx #$0d
        //> bcc DrawMTLoop               ;if not there yet, loop back
        metatileBufOffset = currentRowOffset + 1
        if (metatileBufOffset < 0x0d) continue
        // Break when 13 rows processed
        break
    }

    // Now emit the VRAM updates for the column: address ntAddr, vertical stepping, len=0x1A
    if (patternIds.isNotEmpty()) {
        val patterns = patternIds.map { id -> OriginalRom.backgrounds[id and 0xFF] }
        ram.vRAMBuffer2.add(
            BufferedPpuUpdate.BackgroundPatternString(
                nametable = ntIndex.toByte(),
                x = startX.toByte(),
                y = startY.toByte(),
                drawVertically = true,
                patterns = patterns
            )
        )
    }

    //> ldy $00                      ;get current vram buffer offset, increment by 3
    //> iny                          ;(for name table address and length bytes)
    //> iny
    //> iny
    //> lda #$00
    //> sta VRAM_Buffer2,y           ;put null terminator at end of data for name table
    //> sty VRAM_Buffer2_Offset      ;store new buffer offset
    // With high-level buffers we don’t track raw offset; UpdateScreen consumes ram.vRAMBuffer2 directly.

    //> inc CurrentNTAddr_Low        ;increment name table address low
    //> lda CurrentNTAddr_Low        ;check current low byte
    //> and #%00011111               ;if no wraparound, just skip this part
    //> bne ExitDrawM
    ram.currentNTAddrLow++
    if ((ram.currentNTAddrLow.toInt() and 0x1F) == 0) {
        //> lda #$80                     ;if wraparound occurs, make sure low byte stays
        //> sta CurrentNTAddr_Low        ;just under the status bar
        ram.currentNTAddrLow = 0x80.toByte()
        //> lda CurrentNTAddr_High       ;and then invert d2 of the name table address high
        //> eor #%00000100               ;to move onto the next appropriate name table
        //> sta CurrentNTAddr_High
        val hi = ram.currentNTAddrHigh.toInt() xor 0x04
        ram.currentNTAddrHigh = hi.toByte()
    }

    //> ExitDrawM:  jmp SetVRAMCtrl              ;jump to set buffer to $0341 and leave
    setVRAMCtrl()
}

// Minimal stub for the tail jump target to mirror flow; sets VRAM buffer selector to 6
private fun System.setVRAMCtrl() {
    //> SetVRAMCtrl: lda #$06
    //> sta VRAM_Buffer_AddrCtrl
    ram.vRAMBufferAddrCtrl = 0x06.toByte()
}

data class Metatile(
    val topLeft: Int,
    val bottomLeft: Int,
    val topRight: Int,
    val bottomRight: Int,
)
