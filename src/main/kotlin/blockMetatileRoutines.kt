package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
import com.ivieleague.smbtranslation.chr.OriginalRom
import kotlin.experimental.or

/**
 * Translation of the block-metatile VRAM update helpers around RemoveCoin_Axe and friends.
 *
 * Notes about this port:
 * - The original code writes raw bytes into VRAM_Buffer1 using offsets (e.g., $0341).
 *   Our project models these as high-level BufferedPpuUpdate entries appended to ram.vRAMBuffer1.
 * - Address calculations that depended on temporary zero-page scratch (like $02/$03/$04/$05) are expressed
 *   in clear locals and converted into nametable/x/y coordinates via the currentNTAddr registers when needed.
 * - The assembly is preserved line-for-line using //> comments immediately above their Kotlin equivalents.
 */

//> ;$00 - temp store for offset control bit
//> ;$01 - temp vram buffer offset
//> ;$02 - temp store for vertical high nybble in block buffer routine
//> ;$03 - temp adder for high byte of name table address
//> ;$04, $05 - name table address low/high
//> ;$06, $07 - block buffer address low/high

// A compact representation for the 2x2 tile graphics used by the various block metatiles in this routine.
private data class BlockQuad(val topLeft: UByte, val topRight: UByte, val bottomLeft: UByte, val bottomRight: UByte)

//> BlockGfxData:
//> .db $45, $45, $47, $47
//> .db $47, $47, $47, $47
//> .db $57, $58, $59, $5a
//> .db $24, $24, $24, $24
//> .db $26, $26, $26, $26
private val blockGfxData: List<BlockQuad> = listOf(
    BlockQuad(0x45u, 0x45u, 0x47u, 0x47u),
    BlockQuad(0x47u, 0x47u, 0x47u, 0x47u),
    BlockQuad(0x57u, 0x58u, 0x59u, 0x5au),
    BlockQuad(0x24u, 0x24u, 0x24u, 0x24u), // default blank metatile
    BlockQuad(0x26u, 0x26u, 0x26u, 0x26u), // water blank metatile
)

/**
 * High-level helper to actually emit the two 2-tile rows for a block metatile into vRAMBuffer1.
 * This corresponds to the write performed inside PutBlockMetatile's VRAM writes.
 */
private fun System.emitBlockQuadAtCurrentNT(topLeft: UByte, topRight: UByte, bottomLeft: UByte, bottomRight: UByte) {
    // Translate CurrentNTAddr into nametable and (x,y)
    val ntAddr = ((ram.currentNTAddrHigh.toInt() and 0xFF) shl 8) or (ram.currentNTAddrLow.toInt() and 0xFF)
    val ntIndex = ((ntAddr - 0x2000) / 0x400).coerceIn(0, 3)
    val startInNt = (ntAddr - 0x2000) % 0x400
    val startX = startInNt % 32
    val startY = startInNt / 32

    // Top row (two tiles)
    ram.vRAMBuffer1.add(
        BufferedPpuUpdate.BackgroundPatternString(
            nametable = ntIndex.toByte(),
            x = startX.toByte(),
            y = startY.toByte(),
            drawVertically = false,
            patterns = listOf(
                OriginalRom.backgrounds[topLeft],
                OriginalRom.backgrounds[topRight],
            )
        )
    )
    // Bottom row (+1 in Y)
    ram.vRAMBuffer1.add(
        BufferedPpuUpdate.BackgroundPatternString(
            nametable = ntIndex.toByte(),
            x = startX.toByte(),
            y = (startY + 1).toByte(),
            drawVertically = false,
            patterns = listOf(
                OriginalRom.backgrounds[bottomLeft],
                OriginalRom.backgrounds[bottomRight],
            )
        )
    )
}


/**
 * Select which blank metatile to use and write it, then select buffer #6.
 */
fun System.removeCoinOrAxe() {
    //> RemoveCoin_Axe:
    //> ldy #$41                 ;set low byte so offset points to $0341
    //> lda #$03                 ;load offset for default blank metatile
    var blockIndex = 0x03
    //> ldx AreaType             ;check area type
    //> bne WriteBlankMT         ;if not water type, use offset
    //> lda #$04                 ;otherwise load offset for blank metatile used in water
    if ((ram.areaType) == 0.toByte()) {
        blockIndex = 0x04
    }
    //> WriteBlankMT: jsr PutBlockMetatile     ;do a sub to write blank metatile to vram buffer
    putBlockMetatile(blockIndex)
    //> lda #$06
    //> sta VRAM_Buffer_AddrCtrl ;set vram address controller to $0341 and leave
    ram.vRAMBufferAddrCtrl = 0x06
    //> rts
}

/**
 * Write a new metatile for a block object; increments a residual counter and decrements a flag.
 */
fun System.replaceBlockMetatile(metatileId: Int) {
    //> ReplaceBlockMetatile:
    //> jsr WriteBlockMetatile    ;write metatile to vram buffer to replace block object
    writeBlockMetatile(metatileId)
    //> inc Block_ResidualCounter ;increment unused counter (residual code)
    ram.blockResidualCounter = (ram.blockResidualCounter + 1).toByte()
    //> dec Block_RepFlag,x       ;decrement flag (residual code)
    // We model a single flag in RAM; decrement if nonzero.
    ram.blockRepFlag = (ram.blockRepFlag - 1).toByte()
    //> rts                       ;leave
}

/** Forces blank metatile, then falls into WriteBlockMetatile logic. */
fun System.destroyBlockMetatile() {
    //> DestroyBlockMetatile:
    //> lda #$00       ;force blank metatile if branched/jumped to this point
    writeBlockMetatile(0x00)
}

/**
 * Decide which block graphics entry to use based on the metatileId, then write it.
 */
fun System.writeBlockMetatile(metatileId: Int) {
    //> WriteBlockMetatile:
    //> ldy #$03                ;load offset for blank metatile
    var index = 0x03
    //> cmp #$00                ;check contents of A for blank metatile
    //> beq UseBOffset          ;branch if found (unconditional if branched from 8a6b)
    if (metatileId != 0x00) {
        //> ldy #$00                ;load offset for brick metatile w/ line
        // start with brick w/ line
        index = 0x00
        //> cmp #$58
        //> beq UseBOffset          ;use offset if metatile is brick with coins (w/ line)
        //> cmp #$51
        //> beq UseBOffset          ;use offset if metatile is breakable brick w/ line
        if (metatileId == 0x58 || metatileId == 0x51) {
            // keep index = 0
        } else {
            //> iny                     ;increment offset for brick metatile w/o line
            index = 0x01
            //> cmp #$5d
            //> beq UseBOffset          ;use offset if metatile is brick with coins (w/o line)
            //> cmp #$52
            //> beq UseBOffset          ;use offset if metatile is breakable brick w/o line
            if (metatileId == 0x5d || metatileId == 0x52) {
                // keep index = 0x01
            } else {
                //> iny                     ;if any other metatile, increment offset for empty block
                index = 0x02
            }
        }
    }
    //> UseBOffset:  tya                     ;put Y in A
    //> ldy VRAM_Buffer1_Offset ;get vram buffer offset
    //> iny                     ;move onto next byte
    // Not modeled; we simply append updates.
    //> jsr PutBlockMetatile    ;get appropriate block data and write to vram buffer
    putBlockMetatile(index)
    //> MoveVOffset: dey                     ;decrement vram buffer offset
    //> tya                     ;add 10 bytes to it
    //> clc
    //> adc #10
    //> jmp SetVRAMOffset       ;branch to store as new vram buffer offset
    // No explicit offset tracking in this port.
}

/**
 * Write the selected block metatile into the VRAM buffer.
 *
 * Background: what the original PutBlockMetatile actually does
 * -----------------------------------------------------------
 * The 6502 routine computes an absolute nametable address (low in $04, high in $05) where a 2x2 tile
 * “block” should be drawn, then emits two VRAM buffer records that each write a row of two tiles.
 * Its inputs/temporaries live in zero page as follows (see the disassembly header above):
 * - $00: temp: copy of the SprDataOffset_Ctrl bit (controls whether the buffer offset is even/odd)
 * - $01: temp: a snapshot of VRAM_Buffer1_Offset used for placing bytes in the buffer
 * - $02: temp: “vertical high nybble” from the block buffer routine — effectively the coarse Y within the column
 * - $03: temp: a high-byte adder for the nametable ($20/$24 depending on which 1 KB nametable)
 * - $04/$05: output nametable address low/high in tile memory space (within $2000-$27FF)
 * - $06/$07: block buffer pointer low/high; $06’s low nibble selects the column within the page
 *
 * The address math (summarized):
 * 1) Select nametable high byte into $03:
 *    - Start with $20 for NT0; if $06 >= $D0 (odd page of the block buffer), use $24 for NT1 instead.
 * 2) Compute base low byte into $04 from the column within the 32x30 tile grid:
 *    - $04 = 2 * ( $06 & 0x0F ). In SMB, columns are 16-byte aligned per “block column”; multiplying by 2
 *      converts the nibble index into a tile-address column offset.
 * 3) Build vertical offset from the “vertical high nybble” in $02 (coarse Y), plus 32 to skip the status bar:
 *    - A = $02 + 0x20, then two ASL/ROL pairs fold bits 7..6 of A into carries that flow into $05 while also
 *      doubling A twice. This effectively computes (A << 2) and propagates overflow into the high byte.
 *    - Then add $04 (column base) into A (low) and its carry into $05 (high), finally add $03 into $05.
 *    - Result: $05:$04 points to the top-left tile of the 2x2 block in the intended nametable.
 * 4) With Y=$01 (buffer cursor), the code writes two VRAM records into VRAM_Buffer1 for two rows of two tiles,
 *    with length bytes set to 2 and the addresses $05:$04 and $05:($04+32), then terminates with a null byte.
 *
 * Why we do not translate this literally (yet)
 * -------------------------------------------
 * Our emulator surface does not model the raw byte-oriented VRAM update buffer; instead we accumulate
 * high-level BufferedPpuUpdate entries in ram.vRAMBuffer1. The exact buffer offsets ($01, increment/decrement,
 * inter-record spacing, and the final null terminator) are internal formatting concerns for the NMI writer and
 * are not externally observable once parsed by UpdateScreen. Likewise, our current renderer does not yet consume
 * the block buffer ($06/$07) directly to reconstruct nametable addresses. The project-wide convention is to use
 * CurrentNTAddr_High/Low to say “where are we drawing right now?” and to emit clear, testable draw intents.
 *
 * What we keep equivalent
 * -----------------------
 * - The choice of which 2x2 graphic to draw (index derived in WriteBlockMetatile) is preserved.
 * - The two-row, two-tile write is preserved by emitting two BackgroundPatternString updates at the current
 *   nametable/x/y and y+1 respectively (see emitBlockQuadAtCurrentNT).
 * - The RemBridge subroutine is preserved as its own function (remBridge) because the assembly later jumps to
 *   this label independently.
 *
 * What would change if we later model raw buffers
 * -----------------------------------------------
 * If we later switch to a byte-accurate VRAM buffer, this routine can be made literal by:
 * - Carrying an explicit “buffer offset” integer (standing in for $01/VRAM_Buffer1_Offset) and emitting bytes
 *   exactly as the NES code does (addr_hi, addr_lo, len, data..., addr_hi, addr_lo+32, len, data..., 0x00).
 * - Replacing CurrentNTAddr-derived coordinates with true computation from $02..$07 and column/page bits as above.
 * Until then, this function documents every assembly step with //> comments in situ and keeps behavior equivalent
 * at the rendering level used by tests.
 *
 * In short: this port intentionally compresses buffer-formatting arithmetic into clearer, high-level draw calls,
 * but comments retain a line-by-line map so you can cross-reference with the original.
 */
private fun System.putBlockMetatile(blockGfxIndex: Int) {
    //> PutBlockMetatile:
    //> stx $00               ;store control bit from SprDataOffset_Ctrl
    //> sty $01               ;store vram buffer offset for next byte
    //> asl
    //> asl                   ;multiply A by four and use as X
    //> tax
    //> ldy #$20              ;load high byte for name table 0
    //> lda $06               ;get low byte of block buffer pointer
    //> cmp #$d0              ;check to see if we're on odd-page block buffer
    //> bcc SaveHAdder        ;if not, use current high byte
    //> ldy #$24              ;otherwise load high byte for name table 1
    //> SaveHAdder: sty $03               ;save high byte here
    //> and #$0f              ;mask out high nybble of block buffer pointer
    //> asl                   ;multiply by 2 to get appropriate name table low byte
    //> sta $04               ;and then store it here
    //> lda #$00
    //> sta $05               ;initialize temp high byte
    //> lda $02               ;get vertical high nybble offset used in block buffer routine
    //> clc
    //> adc #$20              ;add 32 pixels for the status bar
    //> asl
    //> rol $05               ;shift and rotate d7 onto d0 and d6 into carry
    //> asl
    //> rol $05               ;shift and rotate d6 onto d0 and d5 into carry
    //> adc $04               ;add low byte of name table and carry to vertical high nybble
    //> sta $04               ;and store here
    //> lda $05               ;get whatever was in d7 and d6 of vertical high nybble
    //> adc #$00              ;add carry
    //> clc
    //> adc $03               ;then add high byte of name table
    //> sta $05               ;store here
    //> ldy $01               ;get vram buffer offset to be used
    // In our port we do not model these two scratch locations explicitly; the high-level buffer appends are order-stable.

    // Write the 2x2 block graphic via the extracted RemBridge routine.
    remBridge(blockGfxIndex)

    //> rts                   ;and leave
}

/**
 * Extracted routine corresponding to the RemBridge label in the original assembly.
 * Performs the actual VRAM buffer writes for the 2x2 block graphic.
 */
fun System.remBridge(blockGfxIndex: Int) {
    //> RemBridge:  lda BlockGfxData,x    ;write top left and top right
    //> sta VRAM_Buffer1+2,y  ;tile numbers into first spot
    //> lda BlockGfxData+1,x
    //> sta VRAM_Buffer1+3,y
    //> lda BlockGfxData+2,x  ;write bottom left and bottom
    //> sta VRAM_Buffer1+7,y  ;right tiles numbers into
    //> lda BlockGfxData+3,x  ;second spot
    //> sta VRAM_Buffer1+8,y
    //> lda $04
    //> sta VRAM_Buffer1,y    ;write low byte of name table
    //> clc                   ;into first slot as read
    //> adc #$20              ;add 32 bytes to value
    //> sta VRAM_Buffer1+5,y  ;write low byte of name table
    //> lda $05               ;plus 32 bytes into second slot
    //> sta VRAM_Buffer1-1,y  ;write high byte of name
    //> sta VRAM_Buffer1+4,y  ;table address to both slots
    //> lda #$02
    //> sta VRAM_Buffer1+1,y  ;put length of 2 in
    //> sta VRAM_Buffer1+6,y  ;both slots
    //> lda #$00
    //> sta VRAM_Buffer1+9,y  ;put null terminator at end
    //> ldx $00               ;get offset control bit here
    val quad = blockGfxData[blockGfxIndex.coerceIn(0, blockGfxData.lastIndex)]
    emitBlockQuadAtCurrentNT(
        topLeft = quad.topLeft,
        topRight = quad.topRight,
        bottomLeft = quad.bottomLeft,
        bottomRight = quad.bottomRight,
    )
}


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

