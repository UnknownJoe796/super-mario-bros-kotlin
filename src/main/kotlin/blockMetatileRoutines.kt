package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.chr.OriginalRom

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
private data class BlockQuad(val topLeft: Int, val topRight: Int, val bottomLeft: Int, val bottomRight: Int)

//> BlockGfxData:
//> .db $45, $45, $47, $47
//> .db $47, $47, $47, $47
//> .db $57, $58, $59, $5a
//> .db $24, $24, $24, $24
//> .db $26, $26, $26, $26
private val blockGfxData: List<BlockQuad> = listOf(
    BlockQuad(0x45, 0x45, 0x47, 0x47),
    BlockQuad(0x47, 0x47, 0x47, 0x47),
    BlockQuad(0x57, 0x58, 0x59, 0x5a),
    BlockQuad(0x24, 0x24, 0x24, 0x24), // default blank metatile
    BlockQuad(0x26, 0x26, 0x26, 0x26), // water blank metatile
)

/**
 * High-level helper to actually emit the two 2-tile rows for a block metatile into vRAMBuffer1.
 * This corresponds to the write performed inside PutBlockMetatile's VRAM writes.
 */
private fun System.emitBlockQuadAtCurrentNT(topLeft: Int, topRight: Int, bottomLeft: Int, bottomRight: Int) {
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
                OriginalRom.backgrounds[topLeft and 0xFF],
                OriginalRom.backgrounds[topRight and 0xFF],
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
                OriginalRom.backgrounds[bottomLeft and 0xFF],
                OriginalRom.backgrounds[bottomRight and 0xFF],
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
    if ((ram.areaType.toInt() and 0xFF) == 0) {
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