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
private fun System.renderAreaGraphics() {
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

val metatileGraphics = listOf(
    listOf(
        //> Palette0_MTiles:
        Metatile(0x24, 0x24, 0x24, 0x24),  // blank
        Metatile(0x27, 0x27, 0x27, 0x27),  // black metatile
        Metatile(0x24, 0x24, 0x24, 0x35),  // bush left
        Metatile(0x36, 0x25, 0x37, 0x25),  // bush middle
        Metatile(0x24, 0x38, 0x24, 0x24),  // bush right
        Metatile(0x24, 0x30, 0x30, 0x26),  // mountain left
        Metatile(0x26, 0x26, 0x34, 0x26),  // mountain left bottom/middle center
        Metatile(0x24, 0x31, 0x24, 0x32),  // mountain middle top
        Metatile(0x33, 0x26, 0x24, 0x33),  // mountain right
        Metatile(0x34, 0x26, 0x26, 0x26),  // mountain right bottom
        Metatile(0x26, 0x26, 0x26, 0x26),  // mountain middle bottom
        Metatile(0x24, 0xc0, 0x24, 0xc0),  // bridge guardrail
        Metatile(0x24, 0x7f, 0x7f, 0x24),  // chain
        Metatile(0xb8, 0xba, 0xb9, 0xbb),  // tall tree top, top half
        Metatile(0xb8, 0xbc, 0xb9, 0xbd),  // short tree top
        Metatile(0xba, 0xbc, 0xbb, 0xbd),  // tall tree top, bottom half
        Metatile(0x60, 0x64, 0x61, 0x65),  // warp pipe end left, points up
        Metatile(0x62, 0x66, 0x63, 0x67),  // warp pipe end right, points up
        Metatile(0x60, 0x64, 0x61, 0x65),  // decoration pipe end left, points up
        Metatile(0x62, 0x66, 0x63, 0x67),  // decoration pipe end right, points up
        Metatile(0x68, 0x68, 0x69, 0x69),  // pipe shaft left
        Metatile(0x26, 0x26, 0x6a, 0x6a),  // pipe shaft right
        Metatile(0x4b, 0x4c, 0x4d, 0x4e),  // tree ledge left edge
        Metatile(0x4d, 0x4f, 0x4d, 0x4f),  // tree ledge middle
        Metatile(0x4d, 0x4e, 0x50, 0x51),  // tree ledge right edge
        Metatile(0x6b, 0x70, 0x2c, 0x2d),  // mushroom left edge
        Metatile(0x6c, 0x71, 0x6d, 0x72),  // mushroom middle
        Metatile(0x6e, 0x73, 0x6f, 0x74),  // mushroom right edge
        Metatile(0x86, 0x8a, 0x87, 0x8b),  // sideways pipe end top
        Metatile(0x88, 0x8c, 0x88, 0x8c),  // sideways pipe shaft top
        Metatile(0x89, 0x8d, 0x69, 0x69),  // sideways pipe joint top
        Metatile(0x8e, 0x91, 0x8f, 0x92),  // sideways pipe end bottom
        Metatile(0x26, 0x93, 0x26, 0x93),  // sideways pipe shaft bottom
        Metatile(0x90, 0x94, 0x69, 0x69),  // sideways pipe joint bottom
        Metatile(0xa4, 0xe9, 0xea, 0xeb),  // seaplant
        Metatile(0x24, 0x24, 0x24, 0x24),  // blank, used on bricks or blocks that are hit
        Metatile(0x24, 0x2f, 0x24, 0x3d),  // flagpole ball
        Metatile(0xa2, 0xa2, 0xa3, 0xa3),  // flagpole shaft
        Metatile(0x24, 0x24, 0x24, 0x24),  // blank, used in conjunction with vines
    ),

    //> Palette1_MTiles:
    listOf(
        Metatile(0xa2, 0xa2, 0xa3, 0xa3),  // vertical rope
        Metatile(0x99, 0x24, 0x99, 0x24),  // horizontal rope
        Metatile(0x24, 0xa2, 0x3e, 0x3f),  // left pulley
        Metatile(0x5b, 0x5c, 0x24, 0xa3),  // right pulley
        Metatile(0x24, 0x24, 0x24, 0x24),  // blank used for balance rope
        Metatile(0x9d, 0x47, 0x9e, 0x47),  // castle top
        Metatile(0x47, 0x47, 0x27, 0x27),  // castle window left
        Metatile(0x47, 0x47, 0x47, 0x47),  // castle brick wall
        Metatile(0x27, 0x27, 0x47, 0x47),  // castle window right
        Metatile(0xa9, 0x47, 0xaa, 0x47),  // castle top w/ brick
        Metatile(0x9b, 0x27, 0x9c, 0x27),  // entrance top
        Metatile(0x27, 0x27, 0x27, 0x27),  // entrance bottom
        Metatile(0x52, 0x52, 0x52, 0x52),  // green ledge stump
        Metatile(0x80, 0xa0, 0x81, 0xa1),  // fence
        Metatile(0xbe, 0xbe, 0xbf, 0xbf),  // tree trunk
        Metatile(0x75, 0xba, 0x76, 0xbb),  // mushroom stump top
        Metatile(0xba, 0xba, 0xbb, 0xbb),  // mushroom stump bottom
        Metatile(0x45, 0x47, 0x45, 0x47),  // breakable brick w/ line
        Metatile(0x47, 0x47, 0x47, 0x47),  // breakable brick
        Metatile(0x45, 0x47, 0x45, 0x47),  // breakable brick (not used)
        Metatile(0xb4, 0xb6, 0xb5, 0xb7),  // cracked rock terrain
        Metatile(0x45, 0x47, 0x45, 0x47),  // brick with line (power-up)
        Metatile(0x45, 0x47, 0x45, 0x47),  // brick with line (vine)
        Metatile(0x45, 0x47, 0x45, 0x47),  // brick with line (star)
        Metatile(0x45, 0x47, 0x45, 0x47),  // brick with line (coins)
        Metatile(0x45, 0x47, 0x45, 0x47),  // brick with line (1-up)
        Metatile(0x47, 0x47, 0x47, 0x47),  // brick (power-up)
        Metatile(0x47, 0x47, 0x47, 0x47),  // brick (vine)
        Metatile(0x47, 0x47, 0x47, 0x47),  // brick (star)
        Metatile(0x47, 0x47, 0x47, 0x47),  // brick (coins)
        Metatile(0x47, 0x47, 0x47, 0x47),  // brick (1-up)
        Metatile(0x24, 0x24, 0x24, 0x24),  // hidden block (1 coin)
        Metatile(0x24, 0x24, 0x24, 0x24),  // hidden block (1-up)
        Metatile(0xab, 0xac, 0xad, 0xae),  // solid block (3-d block)
        Metatile(0x5d, 0x5e, 0x5d, 0x5e),  // solid block (white wall)
        Metatile(0xc1, 0x24, 0xc1, 0x24),  // bridge
        Metatile(0xc6, 0xc8, 0xc7, 0xc9),  // bullet bill cannon barrel
        Metatile(0xca, 0xcc, 0xcb, 0xcd),  // bullet bill cannon top
        Metatile(0x2a, 0x2a, 0x40, 0x40),  // bullet bill cannon bottom
        Metatile(0x24, 0x24, 0x24, 0x24),  // blank used for jumpspring
        Metatile(0x24, 0x47, 0x24, 0x47),  // half brick used for jumpspring
        Metatile(0x82, 0x83, 0x84, 0x85),  // solid block (water level, green rock)
        Metatile(0x24, 0x47, 0x24, 0x47),  // half brick (???)
        Metatile(0x86, 0x8a, 0x87, 0x8b),  // water pipe top
        Metatile(0x8e, 0x91, 0x8f, 0x92),  // water pipe bottom
        Metatile(0x24, 0x2f, 0x24, 0x3d),  // flag ball (residual object)
    ),

    //> Palette2_MTiles:
    listOf(
        Metatile(0x24, 0x24, 0x24, 0x35),  // cloud left
        Metatile(0x36, 0x25, 0x37, 0x25),  // cloud middle
        Metatile(0x24, 0x38, 0x24, 0x24),  // cloud right
        Metatile(0x24, 0x24, 0x39, 0x24),  // cloud bottom left
        Metatile(0x3a, 0x24, 0x3b, 0x24),  // cloud bottom middle
        Metatile(0x3c, 0x24, 0x24, 0x24),  // cloud bottom right
        Metatile(0x41, 0x26, 0x41, 0x26),  // water/lava top
        Metatile(0x26, 0x26, 0x26, 0x26),  // water/lava
        Metatile(0xb0, 0xb1, 0xb2, 0xb3),  // cloud level terrain
        Metatile(0x77, 0x79, 0x77, 0x79),  // bowser's bridge
    ),

    //> Palette3_MTiles:
    listOf(
        Metatile(0x53, 0x55, 0x54, 0x56),  // question block (coin)
        Metatile(0x53, 0x55, 0x54, 0x56),  // question block (power-up)
        Metatile(0xa5, 0xa7, 0xa6, 0xa8),  // coin
        Metatile(0xc2, 0xc4, 0xc3, 0xc5),  // underwater coin
        Metatile(0x57, 0x59, 0x58, 0x5a),  // empty block
        Metatile(0x7b, 0x7d, 0x7c, 0x7e),  // axe
    ),
)

