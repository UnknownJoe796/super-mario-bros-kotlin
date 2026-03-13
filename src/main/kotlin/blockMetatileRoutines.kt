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
 * Emit the two 2-tile rows for a block metatile into vRAMBuffer1 at the given nametable address.
 * @param ntAddr 16-bit NES nametable address ($2000-$27FF) pointing to the top-left tile of the 2x2 block
 */
private fun System.emitBlockQuad(ntAddr: Int, topLeft: UByte, topRight: UByte, bottomLeft: UByte, bottomRight: UByte) {
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
 * Compute the NES nametable address from block buffer pointer low byte ($06) and vertical offset ($02).
 * This mirrors the $06/$02 → $05:$04 computation in PutBlockMetatile.
 */
fun computeBlockNTAddr(bbLow: Int, vertOfs: Int): Int {
    // NT high byte: $20 for buffer 1, $24 for buffer 2
    val ntBase = if (bbLow >= 0xD0) 0x24 else 0x20
    // Column: low nibble of $06 * 2
    val col2 = (bbLow and 0x0F) * 2
    // Vertical with status bar offset: V = vertOfs + $20
    val v = (vertOfs + 0x20) and 0xFF
    // Two ASL/ROL pairs: shift V left 2, propagating high bits into $05
    val aShifted = (v shl 2) and 0xFF
    val highBits = (v shr 6) and 0x03
    // Add column offset to low byte
    val lowSum = aShifted + col2
    val lowByte = lowSum and 0xFF
    val carry = if (lowSum > 0xFF) 1 else 0
    val highByte = highBits + carry + ntBase
    return (highByte shl 8) or lowByte
}


/**
 * Select which blank metatile to use and write it, then select buffer #6.
 * @param bbLow block buffer pointer low byte ($06) — determines nametable and column
 * @param vertOfs vertical offset into block buffer ($02) — determines row
 */
fun System.removeCoinOrAxe(bbLow: Int, vertOfs: Int) {
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
    putBlockMetatile(blockIndex, bbLow, vertOfs)
    //> lda #$06
    //> sta VRAM_Buffer_AddrCtrl ;set vram address controller to $0341 and leave
    ram.vRAMBufferAddrCtrl = 0x06
    //> rts
}

/**
 * Write a new metatile for a block object; increments a residual counter and decrements a flag.
 * @param bbLow block buffer pointer low byte ($06)
 * @param vertOfs vertical offset into block buffer ($02)
 */
fun System.replaceBlockMetatile(metatileId: Int, bbLow: Int, vertOfs: Int) {
    //> ReplaceBlockMetatile:
    //> jsr WriteBlockMetatile    ;write metatile to vram buffer to replace block object
    writeBlockMetatile(metatileId, bbLow, vertOfs)
    //> inc Block_ResidualCounter ;increment unused counter (residual code)
    ram.blockResidualCounter = (ram.blockResidualCounter + 1).toByte()
    //> dec Block_RepFlag,x       ;decrement flag indexed by objectOffset (residual code)
    // by Claude - fix: assembly uses indexed addressing, not scalar
    val x = ram.objectOffset.toInt()
    ram.blockRepFlags[x] = (ram.blockRepFlags[x] - 1).toByte()
    //> rts                       ;leave
}

/**
 * Forces blank metatile, then falls into WriteBlockMetatile logic.
 * @param bbLow block buffer pointer low byte ($06)
 * @param vertOfs vertical offset into block buffer ($02)
 */
fun System.destroyBlockMetatile(bbLow: Int, vertOfs: Int) {
    //> DestroyBlockMetatile:
    //> lda #$00       ;force blank metatile if branched/jumped to this point
    writeBlockMetatile(0x00, bbLow, vertOfs)
}

/**
 * Decide which block graphics entry to use based on the metatileId, then write it.
 * @param bbLow block buffer pointer low byte ($06)
 * @param vertOfs vertical offset into block buffer ($02)
 */
fun System.writeBlockMetatile(metatileId: Int, bbLow: Int, vertOfs: Int) {
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
    putBlockMetatile(index, bbLow, vertOfs)
    //> MoveVOffset: dey                     ;decrement vram buffer offset
    //> tya                     ;add 10 bytes to it
    //> clc
    //> adc #10
    //> jmp SetVRAMOffset       ;branch to store as new vram buffer offset
    // No explicit offset tracking in this port.
}

/**
 * Compute nametable address from block buffer pointer ($06) and vertical offset ($02),
 * then write the 2x2 block graphic into the VRAM buffer.
 */
private fun System.putBlockMetatile(blockGfxIndex: Int, bbLow: Int, vertOfs: Int) {
    //> PutBlockMetatile:
    // Compute nametable address from $06 (bbLow) and $02 (vertOfs), stored into $04/$05
    val ntAddr = computeBlockNTAddr(bbLow, vertOfs)

    // Write the 2x2 block graphic
    remBridge(blockGfxIndex, ntAddr)

    //> rts                   ;and leave
}

/**
 * Extracted routine corresponding to the RemBridge label in the original assembly.
 * Performs the actual VRAM buffer writes for the 2x2 block graphic.
 */
/**
 * Write a 2x2 block graphic into the VRAM buffer at the given nametable address.
 * @param ntAddr 16-bit NES nametable address ($2000-$27FF) for the top-left tile
 */
fun System.remBridge(blockGfxIndex: Int, ntAddr: Int) {
    //> RemBridge:  lda BlockGfxData,x    ;write top left and top right
    val quad = blockGfxData[blockGfxIndex.coerceIn(0, blockGfxData.lastIndex)]
    emitBlockQuad(
        ntAddr = ntAddr,
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

