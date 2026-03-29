@file:OptIn(ExperimentalUnsignedTypes::class)

package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
import com.ivieleague.smbtranslation.Constants.World8
import com.ivieleague.smbtranslation.areaparser.*
import com.ivieleague.smbtranslation.utils.bytePlus
import kotlin.experimental.and

fun System.areaParserTaskHandler() {
    //> AreaParserTaskHandler:
    //> ldy AreaParserTaskNum     ;check number of tasks here
    //> bne DoAPTasks             ;if already set, go ahead
    if (ram.areaParserTaskNum == 0.toByte()) {
        //> ldy #$08
        //> sty AreaParserTaskNum     ;otherwise, set eight by default
        ram.areaParserTaskNum = 0x08
    }
    //> DoAPTasks:    dey
    //> tya
    //> jsr AreaParserTasks
    areaParserTasks(taskToRun = ram.areaParserTaskNum.minus(1))
    //> dec AreaParserTaskNum     ;if all tasks not complete do not
    //> bne SkipATRender          ;render attribute table yet
    if (--ram.areaParserTaskNum != 0.toByte()) return
    //> jsr RenderAttributeTables
    renderAttributeTables()
    //> SkipATRender: rts
}

fun System.areaParserTasks(taskToRun: Int) {
    //> AreaParserTasks:
    //> jsr JumpEngine

    when (taskToRun) {
        //> .dw IncrementColumnPos
        0 -> incrementColumnPos()
        //> .dw RenderAreaGraphics
        1 -> renderAreaGraphics()
        //> .dw RenderAreaGraphics
        2 -> renderAreaGraphics()
        //> .dw AreaParserCore
        3 -> areaParserCore()
        //> .dw IncrementColumnPos
        4 -> incrementColumnPos()
        //> .dw RenderAreaGraphics
        5 -> renderAreaGraphics()
        //> .dw RenderAreaGraphics
        6 -> renderAreaGraphics()
        //> .dw AreaParserCore
        7 -> areaParserCore()
    }
}

private fun System.incrementColumnPos() {
    //> IncrementColumnPos:
    //> inc CurrentColumnPos     ;increment column where we're at
    //> lda CurrentColumnPos
    //> and #%00001111           ;mask out higher nybble
    //> bne NoColWrap
    val newColumn = ++ram.currentColumnPos and 0b1111.toUByte()
    if (newColumn == 0.toUByte()) {
        //> sta CurrentColumnPos     ;if no bits left set, wrap back to zero (0-f)
        ram.currentColumnPos = newColumn
        //> inc CurrentPageLoc       ;and increment page number where we're at
        ram.currentPageLoc++
    }
    //> NoColWrap: inc BlockBufferColumnPos ;increment column offset where we're at
    //> lda BlockBufferColumnPos
    //> and #%00011111           ;mask out all but 5 LSB (0-1f)
    //> sta BlockBufferColumnPos ;and save
    ram.blockBufferColumnPos = ram.blockBufferColumnPos.inc() and 0b11111
    //> rts
}

//> ;$00 - used as counter, store for low nybble for background, ceiling byte for terrain
//> ;$01 - used to store floor byte for terrain
//> ;$07 - used to store terrain metatile
//> ;$06-$07 - used to store block buffer address

//> BSceneDataOffsets:
//> .db $00, $30, $60
val BSceneDataOffsets = ubyteArrayOf(0x0u, 0x30u, 0x60u)

//> BackSceneryData:
val BackSceneryData = ubyteArrayOf(
//> .db $93, $00, $00, $11, $12, $12, $13, $00 ;clouds
    0x93u, 0x00u, 0x00u, 0x11u, 0x12u, 0x12u, 0x13u, 0x00u,
//> .db $00, $51, $52, $53, $00, $00, $00, $00
    0x00u, 0x51u, 0x52u, 0x53u, 0x00u, 0x00u, 0x00u, 0x00u,
//> .db $00, $00, $01, $02, $02, $03, $00, $00
    0x00u, 0x00u, 0x01u, 0x02u, 0x02u, 0x03u, 0x00u, 0x00u,
//> .db $00, $00, $00, $00, $91, $92, $93, $00
    0x00u, 0x00u, 0x00u, 0x00u, 0x91u, 0x92u, 0x93u, 0x00u,
//> .db $00, $00, $00, $51, $52, $53, $41, $42
    0x00u, 0x00u, 0x00u, 0x51u, 0x52u, 0x53u, 0x41u, 0x42u,
//> .db $43, $00, $00, $00, $00, $00, $91, $92
    0x43u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x91u, 0x92u,

//> .db $97, $87, $88, $89, $99, $00, $00, $00 ;mountains and bushes
    0x97u, 0x87u, 0x88u, 0x89u, 0x99u, 0x00u, 0x00u, 0x00u,
//> .db $11, $12, $13, $a4, $a5, $a5, $a5, $a6
    0x11u, 0x12u, 0x13u, 0xa4u, 0xa5u, 0xa5u, 0xa5u, 0xa6u,
//> .db $97, $98, $99, $01, $02, $03, $00, $a4
    0x97u, 0x98u, 0x99u, 0x01u, 0x02u, 0x03u, 0x00u, 0xa4u,
//> .db $a5, $a6, $00, $11, $12, $12, $12, $13
    0xa5u, 0xa6u, 0x00u, 0x11u, 0x12u, 0x12u, 0x12u, 0x13u,
//> .db $00, $00, $00, $00, $01, $02, $02, $03
    0x00u, 0x00u, 0x00u, 0x00u, 0x01u, 0x02u, 0x02u, 0x03u,
//> .db $00, $a4, $a5, $a5, $a6, $00, $00, $00
    0x00u, 0xa4u, 0xa5u, 0xa5u, 0xa6u, 0x00u, 0x00u, 0x00u,

//> .db $11, $12, $12, $13, $00, $00, $00, $00 ;trees and fences
    0x11u, 0x12u, 0x12u, 0x13u, 0x00u, 0x00u, 0x00u, 0x00u,
//> .db $00, $00, $00, $9c, $00, $8b, $aa, $aa
    0x00u, 0x00u, 0x00u, 0x9cu, 0x00u, 0x8bu, 0xaau, 0xaau,
//> .db $aa, $aa, $11, $12, $13, $8b, $00, $9c
    0xaau, 0xaau, 0x11u, 0x12u, 0x13u, 0x8bu, 0x00u, 0x9cu,
//> .db $9c, $00, $00, $01, $02, $03, $11, $12
    0x9cu, 0x00u, 0x00u, 0x01u, 0x02u, 0x03u, 0x11u, 0x12u,
//> .db $12, $13, $00, $00, $00, $00, $aa, $aa
    0x12u, 0x13u, 0x00u, 0x00u, 0x00u, 0x00u, 0xaau, 0xaau,
//> .db $9c, $aa, $00, $8b, $00, $01, $02, $03
    0x9cu, 0xaau, 0x00u, 0x8bu, 0x00u, 0x01u, 0x02u, 0x03u,
)

//> BackSceneryMetatiles:
val BackSceneryMetatiles = ubyteArrayOf(
//> .db $80, $83, $00 ;cloud left
    0x80u, 0x83u, 0x00u,
//> .db $81, $84, $00 ;cloud middle
    0x81u, 0x84u, 0x00u,
//> .db $82, $85, $00 ;cloud right
    0x82u, 0x85u, 0x00u,
//> .db $02, $00, $00 ;bush left
    0x02u, 0x00u, 0x00u,
//> .db $03, $00, $00 ;bush middle
    0x03u, 0x00u, 0x00u,
//> .db $04, $00, $00 ;bush right
    0x04u, 0x00u, 0x00u,
//> .db $00, $05, $06 ;mountain left
    0x00u, 0x05u, 0x06u,
//> .db $07, $06, $0a ;mountain middle
    0x07u, 0x06u, 0x0au,
//> .db $00, $08, $09 ;mountain right
    0x00u, 0x08u, 0x09u,
//> .db $4d, $00, $00 ;fence
    0x4du, 0x00u, 0x00u,
//> .db $0d, $0f, $4e ;tall tree
    0x0du, 0x0fu, 0x4eu,
//> .db $0e, $4e, $4e ;short tree
    0x0eu, 0x4eu, 0x4eu,
)

//> FSceneDataOffsets:
//> .db $00, $0d, $1a
val FSceneDataOffsets = ubyteArrayOf(0x00u, 0x0du, 0x1au)

//> ForeSceneryData: (SMB1)
val ForeSceneryData_SMB1 = ubyteArrayOf(
//> .db $86, $87, $87, $87, $87, $87, $87   ;in water
    0x86u, 0x87u, 0x87u, 0x87u, 0x87u, 0x87u, 0x87u,
//> .db $87, $87, $87, $87, $69, $69
    0x87u, 0x87u, 0x87u, 0x87u, 0x69u, 0x69u,

//> .db $00, $00, $00, $00, $00, $45, $47   ;wall
    0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x45u, 0x47u,
//> .db $47, $47, $47, $47, $00, $00
    0x47u, 0x47u, 0x47u, 0x47u, 0x00u, 0x00u,

//> .db $00, $00, $00, $00, $00, $00, $00   ;over water
    0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u,
//> .db $00, $00, $00, $00, $86, $87
    0x00u, 0x00u, 0x00u, 0x00u, 0x86u, 0x87u,
)

//> ForeSceneryData: (SMB2J - uses $6a instead of $69 for water terrain)
val ForeSceneryData_SMB2J = ubyteArrayOf(
    0x86u, 0x87u, 0x87u, 0x87u, 0x87u, 0x87u, 0x87u,
    0x87u, 0x87u, 0x87u, 0x87u, 0x6au, 0x6au,
    0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x45u, 0x47u,
    0x47u, 0x47u, 0x47u, 0x47u, 0x00u, 0x00u,
    0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u,
    0x00u, 0x00u, 0x00u, 0x00u, 0x86u, 0x87u,
)

//> TerrainMetatiles: (SMB1)
//> .db $69, $54, $52, $62
val TerrainMetatiles_SMB1 = ubyteArrayOf(0x69u, 0x54u, 0x52u, 0x62u)

//> TerrainMetatiles: (SMB2J)
//> .db $6a, $6b, $50, $63
val TerrainMetatiles_SMB2J = ubyteArrayOf(0x6au, 0x6bu, 0x50u, 0x63u)

//> TerrainRenderBits:
val TerrainRenderBits = ubyteArrayOf(
    //> .db %00000000, %00000000 ;no ceiling or floor
    0b00000000u, 0b00000000u,
    //> .db %00000000, %00011000 ;no ceiling, floor 2
    0b00000000u, 0b00011000u,
    //> .db %00000001, %00011000 ;ceiling 1, floor 2
    0b00000001u, 0b00011000u,
    //> .db %00000111, %00011000 ;ceiling 3, floor 2
    0b00000111u, 0b00011000u,
    //> .db %00001111, %00011000 ;ceiling 4, floor 2
    0b00001111u, 0b00011000u,
    //> .db %11111111, %00011000 ;ceiling 8, floor 2
    0b11111111u, 0b00011000u,
    //> .db %00000001, %00011111 ;ceiling 1, floor 5
    0b00000001u, 0b00011111u,
    //> .db %00000111, %00011111 ;ceiling 3, floor 5
    0b00000111u, 0b00011111u,
    //> .db %00001111, %00011111 ;ceiling 4, floor 5
    0b00001111u, 0b00011111u,
    //> .db %10000001, %00011111 ;ceiling 1, floor 6
    0b10000001u, 0b00011111u,
    //> .db %00000001, %00000000 ;ceiling 1, no floor
    0b00000001u, 0b00000000u,
    //> .db %10001111, %00011111 ;ceiling 4, floor 6
    0b10001111u, 0b00011111u,
    //> .db %11110001, %00011111 ;ceiling 1, floor 9
    0b11110001u, 0b00011111u,
    //> .db %11111001, %00011000 ;ceiling 1, middle 5, floor 2
    0b11111001u, 0b00011000u,
    //> .db %11110001, %00011000 ;ceiling 1, middle 4, floor 2
    0b11110001u, 0b00011000u,
    //> .db %11111111, %00011111 ;completely solid top to bottom
    0b11111111u, 0b00011111u,
)

// TODO: Everything below here is considered untrusted.

private fun System.areaParserCore() {
    //> AreaParserCore:
    //> lda BackloadingFlag       ;check to see if we are starting right of start
    //> beq RenderSceneryTerrain  ;if not, go ahead and render background, foreground and terrain
    if (ram.backloadingFlag) {
        //> jsr ProcessAreaData       ;otherwise skip ahead and load level data
        processAreaData()
        // fall through to render anyway per structure (next part renders after ProcessAreaData too)
    }

    //> RenderSceneryTerrain:
    //> ldx #$0c
    //> lda #$00
    //> ClrMTBuf: sta MetatileBuffer,x       ;clear out metatile buffer
    //> dex
    //> bpl ClrMTBuf
    ram.metatileBuffer.fill(0x00u)

    //> ldy BackgroundScenery      ;do we need to render the background scenery?
    //> beq RendFore               ;if not, skip to check the foreground
    if (ram.backgroundScenery != 0.toByte()) {
        //> lda CurrentPageLoc         ;otherwise check for every third page
        var a: IndexingByte = ram.currentPageLoc
        //> ThirdP:   cmp #$03
        //> bmi RendBack               ;if less than three we're there
        //> sec
        //> sbc #$03                   ;if 3 or more, subtract 3 and
        //> bpl ThirdP                 ;do an unconditional branch
        a = (a % 3u).toUByte()

        //> RendBack: asl                        ;move results to higher nybble
        //> asl
        //> asl
        //> asl
        a = a shl 4
        // Carry guaranteed clear
        //> adc BSceneDataOffsets-1,y  ;add to it offset loaded from here
        a = a bytePlus BSceneDataOffsets[ram.backgroundScenery - 1]
        //> adc CurrentColumnPos       ;add to the result our current column position
        a = a bytePlus ram.currentColumnPos
        //> tax
        var x = a
        //> lda BackSceneryData,x      ;load data from sum of offsets
        a = BackSceneryData[x]
        //> beq RendFore               ;if zero, no scenery for that part
        if (a != 0.toUByte()) {
            //> pha
            val stacked = a
            //> and #$0f                   ;save to stack and clear high nybble
            a = a and 0x0fu
            //> sec
            //> sbc #$01                   ;subtract one (because low nybble is $01-$0c)
            a--
            //> sta $00                    ;save low nybble
            //> asl                        ;multiply by three (shift to left and add result to old one)
            //> adc $00                    ;note that since d7 was nulled, the carry flag is always clear
            a = (a * 3u).toUByte()
            //> tax                        ;save as offset for background scenery metatile data
            x = a
            //> pla                        ;get high nybble from stack, move low
            a = stacked
            //> lsr
            //> lsr
            //> lsr
            //> lsr
            a = (a shr 4) and 0x0Fu
            //> tay                        ;use as second offset (used to determine height)
            var y = a
            //> lda #$03                   ;use previously saved memory location for counter
            //> sta $00
            var zp00 = 0x03.toUByte()

            while (true) {
                //> SceLoop1: lda BackSceneryMetatiles,x ;load metatile data from offset of (lsb - 1) * 3
                //> sta MetatileBuffer,y       ;store into buffer from offset of (msb / 16)
                ram.metatileBuffer[y] = BackSceneryMetatiles[x]
                //> inx
                x++
                //> iny
                y++
                //> cpy #$0b                   ;if at this location, leave loop
                //> beq RendFore
                if (y == 0x0B.toUByte()) break
                //> dec $00                    ;decrement until counter expires, barring exception
                zp00--
                //> bne SceLoop1
                if (zp00 == 0.toUByte()) break
            }
        }
    }

    //> RendFore: ldx ForegroundScenery      ;check for foreground data needed or not
    //> beq RendTerr               ;if not, skip this part
    if (ram.foregroundScenery != 0.toByte()) {
        //> ldy FSceneDataOffsets-1,x  ;load offset from location offset by header value, then
        var y = FSceneDataOffsets[ram.foregroundScenery - 1]
        //> ldx #$00                   ;reinit X
        var x = 0
        do {
            //> SceLoop2: lda ForeSceneryData,y      ;load data until counter expires
            //> beq NoFore                 ;do not store if zero found
            val foreSceneryData = if (variant == GameVariant.SMB2J) ForeSceneryData_SMB2J else ForeSceneryData_SMB1
            val v = foreSceneryData[y]
            if (v != 0.toUByte()) {
                //> sta MetatileBuffer,x
                ram.metatileBuffer[x] = v
            }
            //> NoFore:   iny
            y++
            //> inx
            x++
            //> cpx #$0d                   ;store up to end of metatile buffer
            //> bne SceLoop2
        } while (x != 0x0d)
    }

    var aTerrain: UByte
    //> RendTerr: ldy AreaType               ;check world type for water level
    //> bne TerMTile               ;if not water level, skip this part
    //> lda WorldNumber            ;check world number, if not world number eight
    //> cmp #World8                ;then skip this part
    //> bne TerMTile
    if (ram.areaType == AreaType.Water && ram.worldNumber == World8) {
        //> lda #$62 / #$63            ;if set as water level and world number eight,
        aTerrain = if (variant == GameVariant.SMB2J) 0x63u else 0x62u
        //> jmp StoreMT                ;use castle wall metatile as terrain type
    } else {
        //> TerMTile: lda TerrainMetatiles,y     ;otherwise get appropriate metatile for area type
        val terrainMetatiles = if (variant == GameVariant.SMB2J) TerrainMetatiles_SMB2J else TerrainMetatiles_SMB1
        aTerrain = terrainMetatiles[ram.areaType.ordinal]
        //> ldy CloudTypeOverride      ;check for cloud type override
        //> beq StoreMT                ;if not set, keep value otherwise
        if (ram.cloudTypeOverride) {
            //> lda #$88                   ;use cloud block terrain
            aTerrain = 0x88u
        }
    }
    //> StoreMT:  sta $07                    ;store value here
    var zp07 = aTerrain
    //> ldx #$00                   ;initialize X, use as metatile buffer offset
    var x = 0.toByte()
    //> lda TerrainControl         ;use yet another value from the header
    //> asl                        ;multiply by 2 and use as yet another offset
    //> tay
    var y = ((ram.terrainControl) shl 1)
    // We will emulate two passes: ceiling byte then floor byte
    repeat(2) { passIndex ->
        //> TerrLoop: lda TerrainRenderBits,y    ;get one of the terrain rendering bit data
        //> sta $00
        var zp00 = TerrainRenderBits[y]
        //> iny                        ;increment Y and use as offset next time around
        y++
        //> sty $01
        var zp01 = y
        //> lda CloudTypeOverride      ;skip if value here is zero
        //> beq NoCloud2
        //> cpx #$00                   ;otherwise, check if we're doing the ceiling byte
        //> beq NoCloud2
        if (ram.cloudTypeOverride && passIndex != 0) {
            //> lda $00                    ;if not, mask out all but d3
            //> and #%00001000
            //> sta $00
            zp00 = zp00 and 0b00001000.toUByte()
        }
        //> NoCloud2: ldy #$00                   ;start at beginning of bitmasks
        var bitY = 0.toByte()
        do {
            //> TerrBChk: lda Bitmasks,y             ;load bitmask, then perform AND on contents of first byte
            //> bit $00
            //> beq NextTBit               ;if not set, skip this part (do not write terrain to buffer)
            if (Bitmasks[bitY] and zp00 != 0.toUByte()) {
                //> lda $07
                //> sta MetatileBuffer,x       ;load terrain type metatile number and store into buffer here
                ram.metatileBuffer[x] = zp07
            }
            //> NextTBit: inx                        ;continue until end of buffer
            x++
            //> cpx #$0d
            //> beq RendBBuf               ;if we're at the end, break out of this loop
            if (x == 0x0d.toByte()) break
            //> lda AreaType               ;check world type for underground area
            //> cmp #$02
            //> bne EndUChk                ;if not underground, skip this part
            //> cpx #$0b
            //> bne EndUChk                ;if we're at the bottom of the screen, override
            if (ram.areaType == AreaType.Underground && x == 0x0B.toByte()) {
                //> lda #$54                   ;old terrain type with ground level terrain type
                //> sta $07
                zp07 = 0x54.toUByte()
            }
            //> EndUChk:  iny                        ;increment bitmasks offset in Y
            bitY++
            //> cpy #$08
            //> bne TerrBChk               ;if not all bits checked, loop back

        } while (bitY != 0x08.toByte())
        //> ldy $01
        //> bne TerrLoop               ;unconditional branch, use Y to load next byte
        y = zp01
        // loop repeats due to repeat(2)
    }

    //> RendBBuf: jsr ProcessAreaData        ;do the area data loading routine now
    processAreaData()
    //> lda BlockBufferColumnPos
    //> jsr GetBlockBufferAddr     ;get block buffer address from where we're at
    val (buffer, offset) = getBlockBufferAddr(ram.blockBufferColumnPos)
    //> ldx #$00
    //> ldy #$00                   ;init index regs and start at beginning of smaller buffer
    x = 0
    var yStore: Byte = 0
    // loop over 13 entries and write low bytes to block buffer based on thresholds
    do {
        //> ChkMTLow: sty $00
        val zp00 = yStore
        //> lda MetatileBuffer,x       ;load stored metatile number
        //> and #%11000000             ;mask out all but 2 MSB
        //> asl
        //> rol                        ;make %xx000000 into %000000xx
        //> rol
        val attrIndex = ram.metatileBuffer[x] shr 6 and 0x3.toUByte() // 0..3
        //> tay                        ;use as offset in Y
        val yAttr = attrIndex
        //> lda MetatileBuffer,x       ;reload original unmasked value here
        var a: UByte = ram.metatileBuffer[x]
        //> cmp BlockBuffLowBounds,y   ;check for certain values depending on bits set
        //> bcs StrBlock               ;if equal or greater, branch
        if (a < BlockBuffLowBounds[yAttr]) {
            //> lda #$00                   ;if less, init value before storing
            a = 0u
        }
        //> StrBlock: ldy $00                    ;get offset for block buffer
        yStore = zp00
        //> sta ($06),y                ;store value into block buffer
        buffer[offset + (yStore.toInt() and 0xFF)] = a.toByte() // by Claude - unsigned index
        //> tya
        //> clc                        ;add 16 (move down one row) to offset
        //> adc #$10
        //> tay
        yStore = yStore bytePlus 0x10
        //> inx                        ;increment column value
        x++
        //> cpx #$0d
        //> bcc ChkMTLow               ;continue until we pass last row, then leave
    } while(x < 0x0d.toByte())
    //> rts
}

/**
 * @return Block buffer and offset
 */
private fun System.getBlockBufferAddr(a: Byte): Pair<ByteArray, Byte> {

    //> BlockBufferAddr:
    //> .db <Block_Buffer_1, <Block_Buffer_2
    //> .db >Block_Buffer_1, >Block_Buffer_2
    //> GetBlockBufferAddr:
    //> pha                      ;take value of A, save
    //> lsr                      ;move high nybble to low
    //> lsr
    //> lsr
    //> lsr
    //> tay                      ;use nybble as pointer to high byte
    //> lda BlockBufferAddr+2,y  ;of indirect here
    //> sta $07
    val buffer = if(a shr 4 == 1.toByte()) ram.blockBuffer2 else ram.blockBuffer1
    //> pla
    //> and #%00001111           ;pull from stack, mask out high nybble
    //> clc
    //> adc BlockBufferAddr,y    ;add to low byte
    val offset = a and 0x0F
    //> sta $06                  ;store here and leave
    //> rts
    return buffer to offset
}

private fun System.processAreaData() {
    //> ;$00 - used to store area object identifier
    //> ;$07 - used as adder to find proper area object code

    // by Claude - guard: areaData pointer not loaded yet (title screen pre-init)
    if (ram.areaData == null) return

    //> ProcessAreaData:
    //> ldx #$02                 ;start at the end of area object buffer
    var x = 2.toByte()
    do {
        //> ProcADLoop: stx ObjectOffset
        ram.objectOffset = x
        //> lda #$00                 ;reset flag
        //> sta BehindAreaParserFlag
        ram.behindAreaParserFlag = false

        //> ldy AreaDataOffset       ;get offset of area data pointer
        var y = ram.areaDataOffset.toInt() and 0xFF
        //> lda (AreaData),y         ;get first byte of area object
        val firstObjByte = AreaObjByte1(ram.areaData!![y])
        //> cmp #$fd                 ;if end-of-area, skip all this crap
        //> beq RdyDecode
        //> lda AreaObjectLength,x   ;check area object buffer flag
        //> bpl RdyDecode            ;if buffer not negative, branch, otherwise
        var behind: Boolean? = false
        if (!firstObjByte.isEndOfData && ram.areaObjectLength[x] < 0.toByte()) {
            //> iny
            y++
            //> lda (AreaData),y         ;get second byte of area object
            //> asl                      ;check for page select bit (d7), branch if not set
            //> bcc Chk1Row13
            //> lda AreaObjectPageSel    ;check page select
            //> bne Chk1Row13
            if(ram.areaData!![y] < 0.toByte() && ram.areaObjectPageSel == 0.toByte()) {
                //> inc AreaObjectPageSel    ;if not already set, set it now
                ram.areaObjectPageSel++
                //> inc AreaObjectPageLoc    ;and increment page location
                ram.areaObjectPageLoc++
            }
            //> Chk1Row13:  dey
            y--
            //> lda (AreaData),y         ;reread first byte of level object
            val rereadByte1 = AreaObjByte1(ram.areaData!![y])
            //> cmp #$0d                 ;row 13?
            //> bne Chk1Row14
            if(rereadByte1.isSpecialRow13) {
                //> iny                      ;if so, reread second byte of level object
                //> lda (AreaData),y
                //> dey                      ;decrement to get ready to read first byte
                val tmp = ram.areaData!![y + 1]

                //> and #%01000000           ;check for d6 set (if not, object is page control)
                //> bne CheckRear
                //> lda AreaObjectPageSel    ;if page select is set, do not reread
                //> bne CheckRear
                if(tmp and 0b01000000.toByte() != 0.toByte() || ram.areaObjectPageSel != 0.toByte()) {
                    // See CheckRear further down.
                    if (ram.areaObjectPageLoc.toUByte() < ram.currentPageLoc) behind = true
                } else {
                    //> iny                      ;if d6 not set, reread second byte
                    y++
                    //> lda (AreaData),y
                    //> and #%00011111           ;mask out all but 5 LSB and store in page control
                    //> sta AreaObjectPageLoc
                    ram.areaObjectPageLoc = ram.areaData!![y] and 0b11111
                    //> inc AreaObjectPageSel    ;increment page select
                    ram.areaObjectPageSel++
                    //> jmp NextAObj
                    behind = null
                }
            } else {
                //> Chk1Row14:  cmp #$0e                 ;row 14?
                //> bne CheckRear
                //> lda BackloadingFlag      ;check flag for saved page number and branch if set
                //> bne RdyDecode            ;to render the object (otherwise bg might not look right)
                if(!rereadByte1.isAlterAttributes || !ram.backloadingFlag) {
                    //> CheckRear:  lda AreaObjectPageLoc    ;check to see if current page of level object is
                    //> cmp CurrentPageLoc       ;behind current page of renderer
                    //> bcc SetBehind            ;if so branch
                    if (ram.areaObjectPageLoc.toUByte() < ram.currentPageLoc) behind = true
                }
            }
        }
        if(behind == false) {
            //> RdyDecode:  jsr DecodeAreaData       ;do sub and do not turn on flag
            decodeAreaData(x, y.toByte())
            //> jmp ChkLength
        } else {
            if (behind == true) {
                //> SetBehind:  inc BehindAreaParserFlag ;turn on flag if object is behind renderer
                ram.behindAreaParserFlag = true
            }
            //> NextAObj:   jsr IncAreaObjOffset     ;increment buffer offset and move on
            incAreaObjOffset()
        }
        //> ChkLength:  ldx ObjectOffset         ;get buffer offset
        // NES reloads X from ObjectOffset here — decodeAreaData may have changed it
        x = ram.objectOffset
        //> lda AreaObjectLength,x   ;check object length for anything stored here
        //> bmi ProcLoopb            ;if not, branch to handle loopback
        if(ram.areaObjectLength[x] >= 0.toByte()) {
            //> dec AreaObjectLength,x   ;otherwise decrement length or get rid of it
            ram.areaObjectLength[x]--
        }
        //> ProcLoopb:  dex                      ;decrement buffer offset
        x--
        //> bpl ProcADLoop           ;and loopback unless exceeded buffer
    } while(x >= 0.toByte())

    //> lda BehindAreaParserFlag ;check for flag set if objects were behind renderer
    //> bne ProcessAreaData      ;branch if true to load more level data, otherwise
    if(ram.behindAreaParserFlag) return processAreaData()
    //> lda BackloadingFlag      ;check for flag set if starting right of page $00
    //> bne ProcessAreaData      ;branch if true to load more level data, otherwise leave
    if(ram.backloadingFlag) return processAreaData()
    //> EndAParse:  rts
}

private fun System.decodeAreaData(objectOffset: Byte, areaDataOffset: Byte): Unit  {
    //> DecodeAreaData:
    val x = objectOffset
    var y = areaDataOffset
    //> lda AreaObjectLength,x     ;check current buffer flag
    //> bmi Chk1stB
    if (ram.areaObjectLength[x] >= 0.toByte()) {
        //> ldy AreaObjOffsetBuffer,x  ;if not, get offset from buffer
        y = ram.areaObjOffsetBuffer[x]
    }
    //> lda (AreaData),y           ;get first byte of level object again
    var a: Byte = ram.areaData!![y]
    val objByte1 = AreaObjByte1(a)
    //> cmp #$fd
    //> beq EndAParse              ;if end of level, leave this routine
    if (objByte1.isEndOfData) return
    //> Determine jump table offset based on row:
    //> row 15 → offset 0x10, row 12 → offset 0x08, rows 0-11/13/14 → offset 0x00
    // SMB2J inserts UpsideDownPipe_High/Low at indices $16-$17, shifting all subsequent
    // dispatch entries (small objects, row 13, row 14) by 2 in the jump table.
    val smb2jShift: Byte = if (variant == GameVariant.SMB2J) 2 else 0
    var temp07: Byte = when {
        objByte1.isSpecialRow15 -> 0x10
        objByte1.isSpecialRow12 -> 0x08
        else -> 0x00
    }
    //> cmp #$0e                   ;row 14?
    //> bne ChkRow13
    if (objByte1.isAlterAttributes) {
        //> lda #$00                   ;if so, load offset with $00
        //> sta $07
        temp07 = 0x00
        //> lda #$2e                   ;and load A with another value (SMB2J: #$36)
        a = (0x2E + smb2jShift).toByte()
        //> bne NormObj                ;unconditional branch
        // jump to NormObj
    } else if (objByte1.isSpecialRow13) {
        //> ChkRow13: cmp #$0d                   ;row 13?
        //> bne ChkSRows
        //> lda #$22                   ;if so, load offset with 34 (SMB2J: 40/$28)
        //> sta $07
        temp07 = (0x22 + smb2jShift).toByte()
        //> iny                        ;get next byte
        y++
        //> lda (AreaData),y
        a = ram.areaData!![y]
        //> and #%01000000             ;mask out all but d6 (page control obj bit)
        //> beq LeavePar               ;if d6 clear, branch to leave (we handled this earlier)
        if (a and 0b0100_0000.toByte() == 0.toByte()) return
        //> lda (AreaData),y           ;otherwise, get byte again
        a = ram.areaData!![y]
        //> and #%01111111             ;mask out d7
        a = a and 0b0111_1111.toByte()
        //> cmp #$4b                   ;check for loop command in low nybble
        //> bne Mask2MSB               ;(plus d6 set for object other than page control)
        if (a == 0x4B.toByte()) {
            //> inc LoopCommand            ;if loop command, set loop command flag
            ram.loopCommand++
        }
        //> Mask2MSB: and #%00111111             ;mask out d7 and d6
        a = a and 0b0011_1111.toByte()
        //> jmp NormObj                ;and jump
    } else {
        //> ChkSRows: cmp #$0c                   ;row 12-15?
        //> bcs SpecObj
        if (objByte1.isNormalRow) {
            //> iny                        ;if not, get second byte of level object
            y++
            //> lda (AreaData),y
            a = ram.areaData!![y]
            //> and #%01110000             ;mask out all but d6-d4
            val highBits = a and 0b0111_0000.toByte()
            //> bne LrgObj                 ;if any bits set, branch to handle large object
            if (highBits == 0.toByte()) {
                // small object
                //> lda #$16
                //> sta $07                    ;otherwise set offset of 24 for small object (SMB2J: #$18)
                temp07 = (0x16 + smb2jShift).toByte()
                //> lda (AreaData),y           ;reload second byte of level object
                a = ram.areaData!![y]
                //> and #%00001111             ;mask out higher nybble and jump
                a = a and 0x0F
                //> jmp NormObj                ;skip MoveAOId — small objects already have ID in low nibble
            } else {
                //> LrgObj:   sta $00                    ;store value here (branch for large objects)
                var temp00: Byte = highBits
                //> cmp #$70                   ;check for vertical pipe object
                //> bne NotWPipe
                if (temp00 == 0x70.toByte()) {
                    //> lda (AreaData),y           ;if not, reload second byte
                    val second = ram.areaData!![y]
                    //> and #%00001000             ;mask out all but d3 (usage control bit)
                    //> beq NotWPipe               ;if d3 clear, branch to get original value
                    if (second and 0b0000_1000.toByte() != 0.toByte()) {
                        //> lda #$00                   ;otherwise, nullify value for warp pipe
                        //> sta $00
                        temp00 = 0x00
                    }
                }
                //> NotWPipe: lda $00                    ;get value and jump ahead
                a = temp00
                //> jmp MoveAOId
                // MoveAOId: shift d6-d4 to lower nybble
                a = (a.toUByte() shr 4.toUByte()).toByte()
            }
        } else {
            //> SpecObj:  iny                        ;branch here for rows 12-15
            y++
            //> lda (AreaData),y
            a = ram.areaData!![y]
            //> and #%01110000             ;get next byte and mask out all but d6-d4
            a = a and 0b0111_0000.toByte()
            // MoveAOId: shift d6-d4 to lower nybble
            a = (a.toUByte() shr 4.toUByte()).toByte()
        }
    }
    //> NormObj:  sta $00                    ;store value here (branch for small objects and rows 13 and 14)
    val objId: Byte = a
    //> lda AreaObjectLength,x     ;is there something stored here already?
    //> bpl RunAObj                ;if so, branch to do its particular sub
    if (ram.areaObjectLength[x] < 0.toByte()) {
        //> lda AreaObjectPageLoc      ;otherwise check to see if the object we've loaded is on the
        //> cmp CurrentPageLoc         ;same page as the renderer, and if so, branch
        //> beq InitRear
        if (ram.areaObjectPageLoc.toUByte() != ram.currentPageLoc) {
            //> ldy AreaDataOffset         ;if not, get old offset of level pointer
            y = ram.areaDataOffset
            //> lda (AreaData),y           ;and reload first byte
            val reloadByte1 = AreaObjByte1(ram.areaData!![y])
            //> and #%00001111 / cmp #$0e  ;row 14?
            //> bne LeavePar
            if (!reloadByte1.isAlterAttributes) return
            //> lda BackloadingFlag        ;if so, check backloading flag
            //> bne StrAObj                ;if set, branch to render object, else leave
            if (!ram.backloadingFlag) return
            //> LeavePar: rts
            // else fall through to StrAObj
        } else {
            //> InitRear: lda BackloadingFlag        ;check backloading flag to see if it's been initialized
            //> beq BackColC               ;branch to column-wise check
            if (ram.backloadingFlag) {
                //> lda #$00                   ;if not, initialize both backloading and
                //> sta BackloadingFlag        ;behind-renderer flags and leave
                //> sta BehindAreaParserFlag
                //> sta ObjectOffset
                ram.backloadingFlag = false
                ram.behindAreaParserFlag = false
                ram.objectOffset = 0
                //> LoopCmdE: rts
                return
            }
            //> BackColC: ldy AreaDataOffset         ;get first byte again
            y = ram.areaDataOffset
            val backColByte1 = AreaObjByte1(ram.areaData!![y])
            //> and #%11110000 / lsr*4     ;get column position
            //> cmp CurrentColumnPos       ;is this where we're at?
            //> bne LeavePar               ;if not, branch to leave
            if (backColByte1.column.toByte() != ram.currentColumnPos.toByte()) return
            // else fall through to StrAObj
        }
        //> StrAObj:  lda AreaDataOffset         ;if so, load area obj offset and store in buffer
        //> sta AreaObjOffsetBuffer,x
        ram.areaObjOffsetBuffer[x] = ram.areaDataOffset
        //> jsr IncAreaObjOffset       ;do sub to increment to next object data
        incAreaObjOffset()
    }
    //> RunAObj:  lda $00                    ;get stored value and add offset to it
    //> clc                        ;then use the jump engine with current contents of A
    //> adc $07
    //> jsr JumpEngine
    areaParserObjId = objId.toInt() // by Claude - save $00 for routines that need it
    when (objId + temp07) {
        //> ;large objects (rows $00-$0b or 00-11, d6-d4 set)
        //> .dw VerticalPipe         ;used by warp pipes
        0x00 -> verticalPipe()
        //> .dw AreaStyleObject
        0x01 -> areaStyleObject()
        //> .dw RowOfBricks
        0x02 -> rowOfBricks()
        //> .dw RowOfSolidBlocks
        0x03 -> rowOfSolidBlocks()
        //> .dw RowOfCoins
        0x04 -> rowOfCoins()
        //> .dw ColumnOfBricks
        0x05 -> columnOfBricks()
        //> .dw ColumnOfSolidBlocks
        0x06 -> columnOfSolidBlocks()
        //> .dw VerticalPipe         ;used by decoration pipes
        0x07 -> verticalPipe()

        //> ;objects for special row $0c or 12
        //> .dw Hole_Empty
        0x08 -> hole_Empty()
        //> .dw PulleyRopeObject
        0x09 -> pulleyRopeObject()
        //> .dw Bridge_High
        0x0A -> bridge_High()
        //> .dw Bridge_Middle
        0x0B -> bridge_Middle()
        //> .dw Bridge_Low
        0x0C -> bridge_Low()
        //> .dw Hole_Water
        0x0D -> hole_Water()
        //> .dw QuestionBlockRow_High
        0x0E -> questionBlockRow_High()
        //> .dw QuestionBlockRow_Low
        0x0F -> questionBlockRow_Low()

        //> ;objects for special row $0f or 15
        //> .dw EndlessRope
        0x10 -> endlessRope()
        //> .dw BalancePlatRope
        0x11 -> balancePlatRope()
        //> .dw CastleObject
        0x12 -> castleObject()
        //> .dw StaircaseObject
        0x13 -> staircaseObject()
        //> .dw ExitPipe
        0x14 -> exitPipe()
        //> .dw FlagBalls_Residual
        0x15 -> flagBalls_Residual()

        else -> if (variant == GameVariant.SMB2J) {
            // SMB2J dispatch: 2 extra entries at 0x16-0x17 shift all small objects by 2
            smb2jAreaObjectDispatch(objId + temp07)
        } else {
            smb1AreaObjectDispatch(objId + temp07)
        }
    }
}
private fun System.incAreaObjOffset() {
    ram.areaDataOffset++
    ram.areaDataOffset++
    ram.areaObjectPageSel = 0
}


// Bitmasks used for terrain rendering (1<<0 .. 1<<7)
private val Bitmasks = ubyteArrayOf(
    0x01u, 0x02u, 0x04u, 0x08u,
    0x10u, 0x20u, 0x40u, 0x80u,
)

// Thresholds for block buffer low byte based on attribute bits (top 2 bits of metatile)
private val BlockBuffLowBounds = ubyteArrayOf(
    0x10u, 0x51u, 0x88u, 0xC0u
)

/** SMB1 area object dispatch for indices >= 0x16. */
private fun System.smb1AreaObjectDispatch(index: Int) {
    when (index) {
        //> ;small objects (rows $00-$0b, d6-d4 all clear)
        0x16 -> questionBlock()          //> .dw QuestionBlock     ;power-up
        0x17 -> questionBlock()          //> .dw QuestionBlock     ;coin
        0x18 -> questionBlock()          //> .dw QuestionBlock     ;hidden, coin
        0x19 -> hidden1UpBlock()         //> .dw Hidden1UpBlock    ;hidden, 1-up
        0x1A -> brickWithItem()          //> .dw BrickWithItem     ;brick, power-up
        0x1B -> brickWithItem()          //> .dw BrickWithItem     ;brick, vine
        0x1C -> brickWithItem()          //> .dw BrickWithItem     ;brick, star
        0x1D -> brickWithCoins()         //> .dw BrickWithCoins    ;brick, coins
        0x1E -> brickWithItem()          //> .dw BrickWithItem     ;brick, 1-up
        0x1F -> waterPipe()              //> .dw WaterPipe
        0x20 -> emptyBlock()             //> .dw EmptyBlock
        0x21 -> jumpspring()             //> .dw Jumpspring
        //> ;objects for special row $0d (d6 set)
        0x22 -> introPipe()              //> .dw IntroPipe
        0x23 -> flagpoleObject()         //> .dw FlagpoleObject
        0x24 -> { axeObj(); chainObjWithIndex(0) } //> .dw AxeObj
        0x25 -> chainObjWithIndex(1)     //> .dw ChainObj
        0x26 -> castleBridgeObj()        //> .dw CastleBridgeObj
        0x27 -> scrollLockObject_Warp()  //> .dw ScrollLockObject_Warp
        0x28 -> scrollLockObject()       //> .dw ScrollLockObject
        0x29 -> scrollLockObject()       //> .dw ScrollLockObject
        0x2A -> areaFrenzy(0)            //> .dw AreaFrenzy
        0x2B -> areaFrenzy(1)            //> .dw AreaFrenzy
        0x2C -> areaFrenzy(2)            //> .dw AreaFrenzy
        0x2D -> loopCmdE()               //> .dw LoopCmdE
        //> ;object for special row $0e
        0x2E -> alterAreaAttributes()    //> .dw AlterAreaAttributes
    }
}

/** SMB2J area object dispatch for indices >= 0x16. Two extra entries shift small objects by 2. */
private fun System.smb2jAreaObjectDispatch(index: Int) {
    when (index) {
        //> .dw UpsideDownPipe_High   ;(sm2main) NEW in SMB2J
        0x16 -> upsideDownPipe(high = true)
        //> .dw UpsideDownPipe_Low    ;(sm2main) NEW in SMB2J
        0x17 -> upsideDownPipe(high = false)
        //> ;small objects (shifted +2 from SMB1)
        0x18 -> questionBlock()          //> .dw QuestionBlock     ;power-up
        0x19 -> questionBlock()          //> .dw QuestionBlock     ;coin
        0x1A -> questionBlock()          //> .dw QuestionBlock     ;hidden, coin
        0x1B -> questionBlock()          //> .dw QuestionBlock     ;hidden, poison mushroom (NEW)
        0x1C -> hidden1UpBlock()         //> .dw Hidden1UpBlock    ;hidden, 1-up
        0x1D -> questionBlock()          //> .dw QuestionBlock     ;(NEW extra)
        0x1E -> questionBlock()          //> .dw QuestionBlock     ;(NEW extra)
        0x1F -> brickWithItem()          //> .dw BrickWithItem     ;brick, power-up
        0x20 -> brickWithItem()          //> .dw BrickWithItem     ;brick, vine
        0x21 -> brickWithItem()          //> .dw BrickWithItem     ;brick, star
        0x22 -> brickWithItem()          //> .dw BrickWithItem     ;(NEW extra brick)
        0x23 -> brickWithCoins()         //> .dw BrickWithCoins    ;brick, coins
        0x24 -> brickWithItem()          //> .dw BrickWithItem     ;brick, 1-up
        0x25 -> waterPipe()              //> .dw WaterPipe
        0x26 -> emptyBlock()             //> .dw EmptyBlock
        0x27 -> jumpspring()             //> .dw Jumpspring
        //> ;objects for special row $0d (d6 set)
        0x28 -> introPipe()              //> .dw IntroPipe
        0x29 -> flagpoleObject()         //> .dw FlagpoleObject
        0x2A -> { axeObj(); chainObjWithIndex(0) } //> .dw AxeObj
        0x2B -> chainObjWithIndex(1)     //> .dw ChainObj
        0x2C -> castleBridgeObj()        //> .dw CastleBridgeObj
        0x2D -> scrollLockObject_Warp()  //> .dw ScrollLockObject_Warp
        0x2E -> scrollLockObject()       //> .dw ScrollLockObject
        0x2F -> scrollLockObject()       //> .dw ScrollLockObject
        0x30 -> areaFrenzy(0)            //> .dw AreaFrenzy
        0x31 -> areaFrenzy(1)            //> .dw AreaFrenzy
        0x32 -> areaFrenzy(2)            //> .dw AreaFrenzy
        0x33 -> loopCmdE()               //> .dw LoopCmdE
        0x34 -> windOn()                 //> .dw WindOn  (sm2data2/sm2data4)
        0x35 -> windOff()                //> .dw WindOff (sm2data2/sm2data4)
        //> ;object for special row $0e
        0x36 -> alterAreaAttributes()    //> .dw AlterAreaAttributes
    }
}

// by Claude - UpsideDownPipe: renders inverted pipe (body on top, opening on bottom)
// with upside-down piranha plant enemy (SMB2J only)
//> UpsideDownPipe_High: lda #$01; pha; bne UDP
//> UpsideDownPipe_Low:  lda #$04; pha
//> UDP:
private fun System.upsideDownPipe(high: Boolean) {
    val x = ram.objectOffset

    //> lda #$01 / lda #$04    ;start row: 1 (high) or 4 (low)
    //> pha
    val startRow = if (high) 1 else 4

    //> jsr GetPipeHeight       ;get pipe height from object byte
    val pipeInfo = getPipeHeight(x.toInt())
    val verticalLength = pipeInfo.verticalLength
    val horizLengthLeft = pipeInfo.horizLengthLeft

    //> pla
    //> sta $07                 ;save buffer offset (overrides row from GetLrgObjAttrib)
    //> tya; pha                ;save horiz length temporarily

    //> ldy AreaObjectLength,x  ;if on second column of pipe, skip enemy setup
    //> beq NoUDP
    if (ram.areaObjectLength[x.toInt()] != 0.toByte()) {
        //> jsr FindEmptyEnemySlot  ;try to insert upside-down piranha
        //> bcs NoUDP               ;if no empty slots, skip
        val slot = findEmptyEnemySlot()
        if (slot != null) {
            //> lda #$04
            //> jsr SetupPiranhaPlant   ;set up upside-down piranha plant (enemy ID $04)
            // Inline SetupPiranhaPlant with enemy ID $04:
            val xPos = (getAreaObjXPosition().toInt() and 0xFF) + 8
            ram.sprObjXPos[1 + slot] = (xPos and 0xFF).toByte()
            ram.sprObjPageLoc[1 + slot] = ((ram.currentPageLoc.toInt() + (if (xPos > 0xFF) 1 else 0)) and 0xFF).toByte()
            ram.sprObjYHighPos[1 + slot] = 1
            ram.enemyFlags[slot] = 1
            ram.sprObjYPos[1 + slot] = getAreaObjYPosition(startRow.toByte())
            ram.enemyID[slot] = 0x04  // UpsideDownPiranhaP
            val savedOffset = ram.objectOffset
            ram.objectOffset = slot.toByte()
            initPiranhaPlant()
            ram.objectOffset = savedOffset

            //> lda $06                 ;get vertical length
            //> asl; asl; asl; asl      ;multiply by 16
            //> clc
            //> adc Enemy_Y_Position,x  ;add to enemy's Y position
            //> sec
            //> sbc #$0a               ;subtract 10 pixels
            val baseY = ram.sprObjYPos[1 + slot].toInt() and 0xFF
            val adjustedY = ((verticalLength shl 4) + baseY - 0x0a) and 0xFF
            //> sta Enemy_Y_Position,x  ;set as new Y position
            ram.sprObjYPos[1 + slot] = adjustedY.toByte()
            //> sta PiranhaPlantDownYPos,x  ;set as "down" position (inside pipe)
            ram.sprObjYMoveForce[1 + slot] = adjustedY.toByte()
            //> clc
            //> adc #$18               ;add 24 for "up" position (outside pipe, below)
            //> sta PiranhaPlantUpYPos,x
            ram.sprObjYMFDummy[1 + slot] = ((adjustedY + 0x18) and 0xFF).toByte()
            //> inc PiranhaPlant_MoveFlag,x ;set movement flag
            ram.sprObjYSpeed[1 + slot] = 1
        }
    }

    //> NoUDP: pla; tay          ;restore horiz length
    //> pha
    val y = horizLengthLeft
    //> ldx $07                  ;get buffer row offset (1 or 4)
    //> lda VerticalPipeData+2,y ;get pipe BODY metatile
    val bodyMetatile = VerticalPipeData.getOrElse(y + 2) { 0x15u }
    //> ldy $06                  ;get vertical length
    //> dey                      ;-1 for RenderUnderPart count
    //> jsr RenderUnderPart      ;render body from startRow downward
    renderUnderPart(bodyMetatile, startRow, verticalLength - 1)

    //> pla; tay
    //> lda VerticalPipeData,y   ;get pipe OPENING metatile
    val openingMetatile = VerticalPipeData.getOrElse(y) { 0x15u }
    //> sta MetatileBuffer,x     ;write at bottom (after body)
    ram.metatileBuffer[startRow + verticalLength] = openingMetatile
}

//> WindOn: (sm2data2/sm2data4)
//> lda #$01; sta WindFlag; rts
private fun System.windOn() {
    ram.windFlag = true
}

//> WindOff: (sm2data2/sm2data4)
//> lda #$00; sta WindFlag; rts
private fun System.windOff() {
    ram.windFlag = false
}

// by Claude - data tables for area parser object routines

//> FrenzyIDData:
//>   .db FlyCheepCheepFrenzy, BBill_CCheep_Frenzy, Stop_Frenzy
private val FrenzyIDData = byteArrayOf(
    EnemyId.FlyCheepCheepFrenzy.byte, EnemyId.BBillCCheepFrenzy.byte, EnemyId.StopFrenzy.byte
)

//> CastleMetatiles:
private val CastleMetatiles = ubyteArrayOf(
    //> .db $00, $45, $45, $45, $00
    0x00u, 0x45u, 0x45u, 0x45u, 0x00u,
    //> .db $00, $48, $47, $46, $00
    0x00u, 0x48u, 0x47u, 0x46u, 0x00u,
    //> .db $45, $49, $49, $49, $45
    0x45u, 0x49u, 0x49u, 0x49u, 0x45u,
    //> .db $47, $47, $4a, $47, $47
    0x47u, 0x47u, 0x4au, 0x47u, 0x47u,
    //> .db $47, $47, $4b, $47, $47
    0x47u, 0x47u, 0x4bu, 0x47u, 0x47u,
    //> .db $49, $49, $49, $49, $49
    0x49u, 0x49u, 0x49u, 0x49u, 0x49u,
    //> .db $47, $4a, $47, $4a, $47
    0x47u, 0x4au, 0x47u, 0x4au, 0x47u,
    //> .db $47, $4b, $47, $4b, $47
    0x47u, 0x4bu, 0x47u, 0x4bu, 0x47u,
    //> .db $47, $47, $47, $47, $47
    0x47u, 0x47u, 0x47u, 0x47u, 0x47u,
    //> .db $4a, $47, $4a, $47, $4a
    0x4au, 0x47u, 0x4au, 0x47u, 0x4au,
    //> .db $4b, $47, $4b, $47, $4b
    0x4bu, 0x47u, 0x4bu, 0x47u, 0x4bu,
)

//> PulleyRopeMetatiles:
//>   .db $42, $41, $43
private val PulleyRopeMetatiles = ubyteArrayOf(0x42u, 0x41u, 0x43u)

//> CoinMetatileData:
//>   .db $c3, $c2, $c2, $c2
private val CoinMetatileData = ubyteArrayOf(0xc3u, 0xc2u, 0xc2u, 0xc2u)

//> C_ObjectRow:
//>   .db $06, $07, $08
private val C_ObjectRow = byteArrayOf(0x06, 0x07, 0x08)

//> C_ObjectMetatile:
//>   .db $c5, $0c, $89
private val C_ObjectMetatile = ubyteArrayOf(0xc5u, 0x0cu, 0x89u.toUByte())

//> SolidBlockMetatiles:
//>   .db $69, $61, $61, $62
private val SolidBlockMetatiles = ubyteArrayOf(0x69u, 0x61u, 0x61u, 0x62u)

//> BrickMetatiles:
//>   .db $22, $51, $52, $52
//>   .db $88 ;used only by row of bricks object
private val BrickMetatiles = ubyteArrayOf(0x22u, 0x51u, 0x52u, 0x52u, 0x88u)

//> HoleMetatiles:
//>   .db $87, $00, $00, $00
private val HoleMetatiles = ubyteArrayOf(0x87u, 0x00u, 0x00u, 0x00u)

//> StaircaseHeightData:
//>   .db $07, $07, $06, $05, $04, $03, $02, $01, $00
private val StaircaseHeightData = byteArrayOf(0x07, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01, 0x00)

//> StaircaseRowData:
//>   .db $03, $03, $04, $05, $06, $07, $08, $09, $0a
private val StaircaseRowData = byteArrayOf(0x03, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a)

//> SidePipeShaftData:
//>   .db $15, $14  ;used to control whether or not vertical pipe shaft
//>   .db $00, $00  ;is drawn, and if so, controls the metatile number
private val SidePipeShaftData = ubyteArrayOf(0x15u, 0x14u, 0x00u, 0x00u)

//> SidePipeTopPart:
//>   .db $15, $1e  ;top part of sideways part of pipe
//>   .db $1d, $1c
private val SidePipeTopPart = ubyteArrayOf(0x15u, 0x1eu, 0x1du, 0x1cu)

//> SidePipeBottomPart:
//>   .db $15, $21  ;bottom part of sideways part of pipe
//>   .db $20, $1f
private val SidePipeBottomPart = ubyteArrayOf(0x15u, 0x21u, 0x20u, 0x1fu)

// VerticalPipeData moved to areaparser/shared.kt (shared with verticalPipe.kt)

//> BrickQBlockMetatiles: (SMB1)
//>   .db $c1, $c0, $5f, $60          ;used by question blocks
//>   .db $55, $56, $57, $58, $59     ;used by ground level types
//>   .db $5a, $5b, $5c, $5d, $5e     ;used by other level types
private val BrickQBlockMetatiles_SMB1 = ubyteArrayOf(
    0xc1u, 0xc0u, 0x5fu, 0x60u,
    0x55u, 0x56u, 0x57u, 0x58u, 0x59u,
    0x5au, 0x5bu, 0x5cu, 0x5du, 0x5eu,
)

//> BrickQBlockMetatiles: (SMB2J, sm2main line 6351)
//>   .db $c1, $c2, $c0, $5e, $5f, $60, $61  ;used by question blocks (7 entries)
//>   .db $52, $53, $54, $55, $56, $57         ;used by ground level bricks
//>   .db $58, $59, $5a, $5b, $5c, $5d         ;used by other level bricks
private val BrickQBlockMetatiles_SMB2J = ubyteArrayOf(
    0xc1u, 0xc2u, 0xc0u, 0x5eu, 0x5fu, 0x60u, 0x61u,
    0x52u, 0x53u, 0x54u, 0x55u, 0x56u, 0x57u,
    0x58u, 0x59u, 0x5au, 0x5bu, 0x5cu, 0x5du,
)

private fun System.brickQBlockMetatiles() =
    if (variant == GameVariant.SMB2J) BrickQBlockMetatiles_SMB2J else BrickQBlockMetatiles_SMB1

// by Claude - area parser object routines

//> AlterAreaAttributes:
private fun System.alterAreaAttributes() {
    val x = ram.objectOffset
    //> ldy AreaObjOffsetBuffer,x ;load offset for level object data saved in buffer
    val y = ram.areaObjOffsetBuffer[x].toInt() and 0xFF
    //> iny                       ;load second byte
    //> lda (AreaData),y
    val secondByte = ram.areaData!![y + 1]
    //> pha                       ;save in stack for now
    //> and #%01000000
    //> bne Alter2                ;branch if d6 is set
    if (secondByte and 0b01000000.toByte() != 0.toByte()) {
        //> Alter2: pla
        //> and #%00000111            ;mask out all but 3 LSB
        val low3 = secondByte.toInt() and 0x07
        //> cmp #$04                  ;if four or greater, set color control bits
        //> bcc SetFore               ;and nullify foreground scenery bits
        if (low3 >= 4) {
            //> sta BackgroundColorCtrl
            ram.backgroundColorCtrl = low3.toByte()
            //> lda #$00
            //> SetFore: sta ForegroundScenery
            ram.foregroundScenery = 0
        } else {
            //> SetFore: sta ForegroundScenery     ;otherwise set new foreground scenery bits
            ram.foregroundScenery = low3.toByte()
        }
    } else {
        //> pla; pha                       ;pull and push offset to copy to A
        //> and #%00001111            ;mask out high nybble and store as
        //> sta TerrainControl        ;new terrain height type bits
        ram.terrainControl = (secondByte.toInt() and 0x0F).toByte()
        //> pla
        //> and #%00110000            ;pull and mask out all but d5 and d4
        //> lsr; lsr; lsr; lsr       ;move bits to lower nybble and store
        //> sta BackgroundScenery     ;as new background scenery bits
        ram.backgroundScenery = ((secondByte.toInt() and 0x30) shr 4).toByte()
    }
    //> rts
}

//> ScrollLockObject_Warp:
private fun System.scrollLockObject_Warp() {
    if (variant == GameVariant.SMB2J) {
        scrollLockObject_Warp_Smb2j()
    } else {
        scrollLockObject_Warp_Smb1()
    }
}

private fun System.scrollLockObject_Warp_Smb1() {
    //> ldx #$04            ;load value of 4 for game text routine as default
    var textNum = 4
    //> lda WorldNumber     ;warp zone (4-3-2), then check world number
    //> beq WarpNum
    if (ram.worldNumber != 0.toByte()) {
        //> inx                 ;if world number > 1, increment for next warp zone (5)
        textNum++
        //> ldy AreaType        ;check area type
        //> dey
        //> bne WarpNum         ;if ground area type, increment for last warp zone
        if (ram.areaType == AreaType.Ground) {
            //> inx                 ;(8-7-6) and move on
            textNum++
        }
    }
    //> WarpNum: txa
    //> sta WarpZoneControl ;store number here to be used by warp zone routine
    ram.warpZoneControl = textNum.toByte()
    //> jsr WriteGameText   ;print text and warp zone numbers
    writeGameText(textNum.toByte())
    //> lda #PiranhaPlant
    //> jsr KillEnemies     ;load identifier for piranha plants and do sub
    killEnemies(EnemyId.PiranhaPlant.byte)
    //> (falls through to ScrollLockObject)
    scrollLockObject()
}

/**
 * SMB2J ScrollLockObject_Warp: completely different warp zone control logic.
 * Uses $80+ base values with bit 7 set to prevent zero condition in pipe entry code.
 * Warp destinations vary by world, area type, area offset, and level number.
 *
 * Assembly reference (sm2main.asm lines 3425-3469):
 *   World 1: base $80, +1 for ground, +2 for non-first underground
 *   Worlds A-D: $87 + LevelNumber
 *   World 3: $83
 *   World 5: $84 base, +1 for non-ground non-5-1, +2 for ground
 *   Others: $84 + 3 (falls through W678Warp/W5Warp3/W5Warp2)
 */
private fun System.scrollLockObject_Warp_Smb2j() {
    val worldNum = ram.worldNumber.toInt() and 0xFF
    val warpCtrl: Int

    if (ram.hardWorldFlag) {
        //> WarpWorldsAThruD:
        //> lda #$87; clc; adc LevelNumber; bne DumpWarpCtrl
        warpCtrl = 0x87 + (ram.levelNumber.toInt() and 0xFF)
    } else if (worldNum == 0) {
        //> World 1: ldx #$80
        var x = 0x80
        if (ram.areaType == AreaType.Ground) {
            //> dey; beq W1Warp2 -> inx -> W1Warp1 -> BaseW
            x++ // $81
        } else if ((ram.areaAddrsLOffset.toInt() and 0xFF) != 0) {
            //> lda AreaAddrsLOffset; beq W1Warp1 (skip if first underground)
            //> inx -> W1Warp2: inx -> W1Warp1 -> BaseW
            x += 2 // $82
        }
        // else: AreaAddrsLOffset==0 -> W1Warp1 -> BaseW = $80
        warpCtrl = x
    } else {
        //> WarpWorlds2Thru8: ldx #$83
        var x = 0x83
        if (worldNum == 2) {
            //> cmp #World3; beq BaseW -> $83
        } else {
            //> inx -> $84
            x++
            if (worldNum != 4) {
                //> cmp #World5; bne W678Warp
                //> W678Warp: inx -> W5Warp3: inx -> W5Warp2: inx -> BaseW
                x += 3 // $87
            } else {
                //> World 5 paths:
                val areaOfs = ram.areaAddrsLOffset.toInt() and 0xFF
                if (areaOfs == 0x0b) {
                    //> cmp #$0b; beq BaseW -> $84
                } else if (ram.areaType == AreaType.Ground) {
                    //> dey; beq W5Warp3: inx -> W5Warp2: inx -> BaseW
                    x += 2 // $86
                } else {
                    //> jmp W5Warp2: inx -> BaseW
                    x++ // $85
                }
            }
        }
        warpCtrl = x
    }

    //> DumpWarpCtrl: sta WarpZoneControl
    ram.warpZoneControl = warpCtrl.toByte()
    //> jsr WriteGameText    ;SMB2J uses same text routine for warp zones
    writeGameText(0x04.toByte()) // warp zone text
    //> lda #$0d; jsr KillEnemies
    killEnemies(EnemyId.PiranhaPlant.byte)
    //> (falls through to ScrollLockObject)
    scrollLockObject()
}

//> ScrollLockObject:
private fun System.scrollLockObject() {
    //> lda ScrollLock      ;invert scroll lock to turn it on
    //> eor #%00000001
    //> sta ScrollLock
    ram.scrollLock = !ram.scrollLock
    //> rts
}

// by Claude - AreaFrenzy implementation
private fun System.areaFrenzy(frenzyIndex: Int) {
    //> AreaFrenzy:  ldx $00               ;use area object identifier bit as offset
    //> lda FrenzyIDData-8,x  ;note that it starts at 8, thus weird address here
    val frenzyId = FrenzyIDData[frenzyIndex]
    //> ldy #$05
    //> FreCompLoop: dey                   ;check regular slots of enemy object buffer
    //> bmi ExitAFrenzy       ;if all slots checked and enemy object not found, branch to store
    //> cmp Enemy_ID,y    ;check for enemy object in buffer versus frenzy object
    //> bne FreCompLoop
    var found = false
    for (y in 4 downTo 0) {
        if (ram.enemyID[y] == frenzyId) {
            //> lda #$00              ;if enemy object already present, nullify queue and leave
            ram.enemyFrenzyQueue = 0
            found = true
            break
        }
    }
    if (!found) {
        //> ExitAFrenzy: sta EnemyFrenzyQueue  ;store enemy into frenzy queue
        ram.enemyFrenzyQueue = frenzyId
    }
    //> rts
}

//> LoopCmdE:
private fun System.loopCmdE() {
    //> LoopCmdE: rts
    // does nothing - just returns
}

//> AreaStyleObject:
private fun System.areaStyleObject() {
    //> lda AreaStyle        ;load level object style and jump to the right sub
    //> jsr JumpEngine
    //> .dw TreeLedge        ;also used for cloud type levels
    //> .dw MushroomLedge
    //> .dw BulletBillCannon
    when (ram.areaStyle.toInt() and 0xFF) {
        0 -> treeLedge()
        1 -> mushroomLedge()
        2 -> bulletBillCannon()
    }
}

//> TreeLedge:
private fun System.treeLedge() {
    val x = ram.objectOffset
    //> jsr GetLrgObjAttrib     ;get row and length of green ledge
    val attrib = getLrgObjAttrib(x)
    val row = attrib.row.toInt() and 0xFF
    //> lda AreaObjectLength,x  ;check length counter for expiration
    val len = ram.areaObjectLength[x]
    //> beq EndTreeL
    if (len == 0.toByte()) {
        //> EndTreeL: lda #$18                ;render end of tree ledge
        //> jmp NoUnder
        val metatile = 0x18.toUByte()
        //> NoUnder: ldx $07                    ;load row of ledge
        //> ldy #$00                   ;set 0 for no bottom on this part
        //> jmp RenderUnderPart
        renderUnderPart(metatile, row, 0)
        return
    }
    //> bpl MidTreeL
    if (len > 0.toByte()) {
        //> MidTreeL: ldx $07
        //> lda #$17                ;render middle of tree ledge
        //> sta MetatileBuffer,x    ;note that this is also used if ledge position is
        ram.metatileBuffer[row] = 0x17.toUByte()
        //> lda #$4c                ;at the start of level for continuous effect
        //> jmp AllUnder            ;now render the part underneath
        //> AllUnder: inx
        //> ldy #$0f                   ;set $0f to render all way down
        //> jmp RenderUnderPart
        renderUnderPart(0x4c.toUByte(), row + 1, 0x0f)
        return
    }
    //> (len < 0, just starting)
    //> tya
    //> sta AreaObjectLength,x  ;store lower nybble into buffer flag as length of ledge
    ram.areaObjectLength[x] = attrib.length
    //> lda CurrentPageLoc
    //> ora CurrentColumnPos    ;are we at the start of the level?
    //> beq MidTreeL
    if (ram.currentPageLoc.toInt() or ram.currentColumnPos.toInt() == 0) {
        //> MidTreeL: (same code as above)
        ram.metatileBuffer[row] = 0x17.toUByte()
        renderUnderPart(0x4c.toUByte(), row + 1, 0x0f)
    } else {
        //> lda #$16                ;render start of tree ledge
        //> jmp NoUnder
        renderUnderPart(0x16.toUByte(), row, 0)
    }
}

//> MushroomLedge:
private fun System.mushroomLedge() {
    val x = ram.objectOffset
    //> jsr ChkLrgObjLength        ;get shroom dimensions
    val (attrib, fixedLen) = chkLrgObjLength(x)
    val row = attrib.row.toInt() and 0xFF
    //> sty $06                    ;store length here for now
    var zp06 = attrib.length.toInt() and 0xFF
    //> bcc EndMushL
    if (fixedLen.justStarting) {
        //> lda AreaObjectLength,x     ;divide length by 2 and store elsewhere
        //> lsr
        //> sta MushroomLedgeHalfLen,x
        ram.mushroomLedgeHalfLen[x] = ((ram.areaObjectLength[x].toInt() and 0xFF) shr 1).toByte()
        //> lda #$19                   ;render start of mushroom
        //> jmp NoUnder
        renderUnderPart(0x19.toUByte(), row, 0)
        return
    }
    //> EndMushL: lda #$1b                   ;if at the end, render end of mushroom
    //> ldy AreaObjectLength,x
    //> beq NoUnder
    val areaLen = ram.areaObjectLength[x].toInt() and 0xFF
    if (areaLen == 0) {
        //> lda #$1b ; jmp NoUnder
        renderUnderPart(0x1b.toUByte(), row, 0)
        return
    }
    //> lda MushroomLedgeHalfLen,x ;get divided length and store where length
    //> sta $06                    ;was stored originally
    zp06 = ram.mushroomLedgeHalfLen[x].toInt() and 0xFF
    //> ldx $07
    //> lda #$1a
    //> sta MetatileBuffer,x       ;render middle of mushroom
    ram.metatileBuffer[row] = 0x1a.toUByte()
    //> cpy $06                    ;are we smack dab in the center?
    //> bne MushLExit              ;if not, branch to leave
    if (areaLen == zp06) {
        //> inx
        //> lda #$4f
        //> sta MetatileBuffer,x       ;render stem top of mushroom underneath the middle
        ram.metatileBuffer[row + 1] = 0x4f.toUByte()
        //> lda #$50
        //> AllUnder: inx
        //> ldy #$0f                   ;set $0f to render all way down
        //> jmp RenderUnderPart       ;now render the stem of mushroom
        renderUnderPart(0x50.toUByte(), row + 2, 0x0f)
    }
    //> MushLExit: rts
}

//> PulleyRopeObject:
private fun System.pulleyRopeObject() {
    val x = ram.objectOffset
    //> jsr ChkLrgObjLength       ;get length of pulley/rope object
    val (_, fixedLen) = chkLrgObjLength(x)
    //> ldy #$00                  ;initialize metatile offset
    var metatileIdx = 0
    //> bcs RenderPul             ;if starting, render left pulley
    if (!fixedLen.justStarting) {
        //> iny
        metatileIdx = 1
        //> lda AreaObjectLength,x    ;if not at the end, render rope
        //> bne RenderPul
        if (ram.areaObjectLength[x] == 0.toByte()) {
            //> iny                       ;otherwise render right pulley
            metatileIdx = 2
        }
    }
    //> RenderPul: lda PulleyRopeMetatiles,y
    //> sta MetatileBuffer        ;render at the top of the screen
    ram.metatileBuffer[0] = PulleyRopeMetatiles[metatileIdx]
    //> MushLExit: rts
}

//> Bridge_High:
private fun System.bridge_High() {
    //> lda #$06  ;start on the seventh row from top of screen
    bridgeCommon(0x06)
}

//> Bridge_Middle:
private fun System.bridge_Middle() {
    //> lda #$07  ;start on the eighth row
    bridgeCommon(0x07)
}

//> Bridge_Low:
private fun System.bridge_Low() {
    //> lda #$09             ;start on the tenth row
    bridgeCommon(0x09)
}

// by Claude - shared bridge rendering logic
private fun System.bridgeCommon(startRow: Int) {
    val x = ram.objectOffset
    //> pha                  ;save whatever row to the stack for now
    //> jsr ChkLrgObjLength  ;get low nybble and save as length
    chkLrgObjLength(x)
    //> pla
    //> tax                  ;render bridge railing
    //> lda #$0b
    //> sta MetatileBuffer,x
    ram.metatileBuffer[startRow] = 0x0b.toUByte()
    //> inx
    //> ldy #$00             ;now render the bridge itself
    //> lda #$63
    //> jmp RenderUnderPart
    renderUnderPart(0x63.toUByte(), startRow + 1, 0)
}

//> Hole_Water:
private fun System.hole_Water() {
    val x = ram.objectOffset
    //> jsr ChkLrgObjLength   ;get low nybble and save as length
    chkLrgObjLength(x)
    //> lda #$86              ;render waves
    //> sta MetatileBuffer+10
    ram.metatileBuffer[10] = 0x86.toUByte()
    //> ldx #$0b
    //> ldy #$01              ;now render the water underneath
    //> lda #$87
    //> jmp RenderUnderPart
    renderUnderPart(0x87.toUByte(), 0x0b, 0x01)
}

//> QuestionBlockRow_High:
private fun System.questionBlockRow_High() {
    //> lda #$03    ;start on the fourth row
    questionBlockRowCommon(0x03)
}

//> QuestionBlockRow_Low:
private fun System.questionBlockRow_Low() {
    //> lda #$07             ;start on the eighth row
    questionBlockRowCommon(0x07)
}

// by Claude - shared question block row rendering
private fun System.questionBlockRowCommon(startRow: Int) {
    val x = ram.objectOffset
    //> pha                  ;save whatever row to the stack for now
    //> jsr ChkLrgObjLength  ;get low nybble and save as length
    chkLrgObjLength(x)
    //> pla
    //> tax                  ;render question boxes with coins
    //> lda #$c0
    //> sta MetatileBuffer,x
    ram.metatileBuffer[startRow] = 0xc0.toUByte()
    //> rts
}

//> EndlessRope:
private fun System.endlessRope() {
    //> ldx #$00       ;render rope from the top to the bottom of screen
    //> ldy #$0f
    //> jmp DrawRope
    //> DrawRope: lda #$40            ;render the actual rope
    //> jmp RenderUnderPart
    renderUnderPart(0x40.toUByte(), 0x00, 0x0f)
}

//> BalancePlatRope:
private fun System.balancePlatRope() {
    val x = ram.objectOffset
    //> txa                 ;save object buffer offset for now
    //> pha
    //> ldx #$01            ;blank out all from second row to the bottom
    //> ldy #$0f            ;with blank used for balance platform rope
    //> lda #$44
    //> jsr RenderUnderPart
    renderUnderPart(0x44.toUByte(), 0x01, 0x0f)
    //> pla                 ;get back object buffer offset
    //> tax
    //> jsr GetLrgObjAttrib ;get vertical length from lower nybble
    val attrib = getLrgObjAttrib(x)
    //> ldx #$01
    //> DrawRope: lda #$40            ;render the actual rope
    //> jmp RenderUnderPart
    renderUnderPart(0x40.toUByte(), 0x01, attrib.length.toInt() and 0xFF)
}

//> CastleObject:
private fun System.castleObject() {
    val x = ram.objectOffset
    //> jsr GetLrgObjAttrib      ;save lower nybble as starting row
    val attrib = getLrgObjAttrib(x)
    //> sty $07                  ;if starting row is above $0a, game will crash!!!
    val startRow = attrib.row.toInt() and 0xFF  // assembly saves length to $07, but actually it's attrib.length
    // Wait, re-reading the assembly:
    // GetLrgObjAttrib returns: $07 = row, Y = length
    // Then: sty $07 — this OVERWRITES $07 with Y (the length value)
    // So $07 = length after this line. The "starting row" comment is misleading.
    // Actually no — let me re-read. GetLrgObjAttrib sets $07 = row (from first byte & 0x0F)
    // and returns Y = lower nybble of second byte (length).
    // Then `sty $07` overwrites $07 with Y. So $07 now = length = attrib.length.
    // But the comment says "save lower nybble as starting row" — the lower nybble
    // of the FIRST byte IS the row. And Y = lower nybble of SECOND byte = length/height.
    // So after `sty $07`, $07 = the height/length value, NOT the row.
    // Wait, that makes no sense for the castle. Let me re-read the full routine:
    //
    // CastleObject:
    //     jsr GetLrgObjAttrib      ;save lower nybble as starting row
    //     sty $07                  ;if starting row is above $0a, game will crash!!!
    //
    // GetLrgObjAttrib returns: $07 = row (first byte & 0x0F), Y = length (second byte & 0x0F)
    // Then `sty $07` saves Y (length) into $07, overwriting the row.
    // But later: `ldx $07 ; begin at starting row` — this uses $07 as the starting row!
    // So actually Y IS the starting row here. Wait, that's confusing.
    //
    // Let me re-read GetLrgObjAttrib more carefully:
    //   lda (AreaData),y  ;get first byte
    //   and #%00001111
    //   sta $07           ;save row location
    //   iny
    //   lda (AreaData),y  ;get second byte
    //   and #%00001111    ;lower nybble = height/length
    //   tay               ;Y = lower nybble of second byte
    //
    // So after GetLrgObjAttrib: $07 = row from first byte, Y = lower nybble from second byte.
    // Then CastleObject does: sty $07 — overwrites row with length.
    // Then: ldy #$04; jsr ChkLrgObjFixedLength — sets AreaObjectLength to 4 if new.
    // Then: ldy AreaObjectLength,x — Y = current column of castle (4,3,2,1,0)
    // Then: ldx $07 — X = $07 = the value stored by sty $07 = lower nybble of 2nd byte
    //
    // The "starting row" referred to in the comment IS the lower nybble of the second byte.
    // In the original NES level encoding, for row 15 objects ($0f), the first byte's lower
    // nybble doesn't carry row info in the same way. Actually for row $0f objects, the row
    // value from decodeAreaData is just $0f, and the actual rendering row comes from the
    // second byte's lower nybble.
    //
    // So for CastleObject: the starting row = lower nybble of second byte = attrib.length
    val castleStartRow = attrib.length.toInt() and 0xFF

    //> ldy #$04
    //> jsr ChkLrgObjFixedLength ;load length of castle if not already loaded
    chkLrgObjFixedLength(x, 0x04)
    //> txa; pha                      ;save obj buffer offset to stack
    // (we just remember x)
    //> ldy AreaObjectLength,x   ;use current length as offset for castle data
    var castleCol = ram.areaObjectLength[x].toInt() and 0xFF
    //> ldx $07                  ;begin at starting row
    var bufX = castleStartRow
    //> lda #$0b
    //> sta $06                  ;load upper limit of number of rows to print
    var upperLimit = 0x0b
    //> CRendLoop:
    while (true) {
        //> lda CastleMetatiles,y    ;load current byte using offset
        //> sta MetatileBuffer,x
        ram.metatileBuffer[bufX] = CastleMetatiles[castleCol]
        //> inx                      ;store in buffer and increment buffer offset
        bufX++
        //> lda $06
        //> beq ChkCFloor            ;have we reached upper limit yet?
        if (upperLimit != 0) {
            //> iny; iny; iny; iny; iny  ;if not, increment column-wise to byte in next row
            castleCol += 5
            //> dec $06                  ;move closer to upper limit
            upperLimit--
        }
        //> ChkCFloor: cpx #$0b                 ;have we reached the row just before floor?
        //> bne CRendLoop            ;if not, go back and do another row
        if (bufX == 0x0b) break
    }
    //> pla; tax                      ;get obj buffer offset from before
    // (x is still the objectOffset)
    //> lda CurrentPageLoc
    //> beq ExitCastle           ;if we're at page 0, we do not need to do anything else
    if (ram.currentPageLoc == 0.toUByte()) return
    //> lda AreaObjectLength,x   ;check length
    val areaLen = ram.areaObjectLength[x].toInt() and 0xFF
    //> cmp #$01                 ;if length almost about to expire, put brick at floor
    //> beq PlayerStop
    if (areaLen == 0x01) {
        //> PlayerStop: ldy #$52                 ;put brick at floor to stop player at end of level
        //> sty MetatileBuffer+10    ;this is only done if we're on the second column
        ram.metatileBuffer[10] = 0x52.toUByte()
        //> ExitCastle: rts
        return
    }
    //> ldy $07                  ;check starting row for tall castle ($00)
    //> bne NotTall
    if (castleStartRow == 0) {
        //> cmp #$03                 ;if found, then check to see if we're at the second column
        //> beq PlayerStop
        if (areaLen == 0x03) {
            ram.metatileBuffer[10] = 0x52.toUByte()
            return
        }
    }
    //> NotTall: cmp #$02                 ;if not tall castle, check to see if we're at the third column
    //> bne ExitCastle           ;if we aren't and the castle is tall, don't create flag yet
    if (areaLen != 0x02) return
    //> jsr GetAreaObjXPosition  ;otherwise, obtain and save horizontal pixel coordinate
    val xPixel = getAreaObjXPosition()
    //> pha
    //> jsr FindEmptyEnemySlot   ;find an empty place on the enemy object buffer
    val slot = findEmptyEnemySlot() ?: return
    //> pla
    //> sta Enemy_X_Position,x   ;then write horizontal coordinate for star flag
    ram.sprObjXPos[1 + slot] = xPixel
    //> lda CurrentPageLoc
    //> sta Enemy_PageLoc,x      ;set page location for star flag
    ram.sprObjPageLoc[1 + slot] = ram.currentPageLoc.toByte()
    //> lda #$01
    //> sta Enemy_Y_HighPos,x    ;set vertical high byte
    ram.sprObjYHighPos[1 + slot] = 0x01
    //> sta Enemy_Flag,x         ;set flag for buffer
    ram.enemyFlags[slot] = 0x01
    //> lda #$90
    //> sta Enemy_Y_Position,x   ;set vertical coordinate
    ram.sprObjYPos[1 + slot] = 0x90.toByte()
    //> lda #StarFlagObject      ;set star flag value in buffer itself
    //> sta Enemy_ID,x
    ram.enemyID[slot] = EnemyId.StarFlagObject.byte
    //> rts
}

//> WaterPipe:
private fun System.waterPipe() {
    val x = ram.objectOffset
    //> jsr GetLrgObjAttrib     ;get row and lower nybble
    val attrib = getLrgObjAttrib(x)
    //> ldy AreaObjectLength,x  ;get length (residual code, water pipe is 1 col thick)
    //> ldx $07                 ;get row
    val row = attrib.row.toInt() and 0xFF
    //> lda #$6b (SMB1) / #$6d (SMB2J)
    //> sta MetatileBuffer,x    ;draw something here and below it
    val topTile: UByte = if (variant == GameVariant.SMB2J) 0x6d.toUByte() else 0x6b.toUByte()
    val botTile: UByte = if (variant == GameVariant.SMB2J) 0x6e.toUByte() else 0x6c.toUByte()
    if (row < ram.metatileBuffer.size) ram.metatileBuffer[row] = topTile
    //> lda #$6c (SMB1) / #$6e (SMB2J)
    //> sta MetatileBuffer+1,x  (on NES, overflow writes past buffer into adjacent RAM)
    if (row + 1 < ram.metatileBuffer.size) ram.metatileBuffer[row + 1] = botTile
    //> rts
}

//> IntroPipe:
private fun System.introPipe() {
    val x = ram.objectOffset
    //> ldy #$03                 ;check if length set, if not set, set it
    //> jsr ChkLrgObjFixedLength
    chkLrgObjFixedLength(x, 0x03)
    //> ldy #$0a                 ;set fixed value and render the sideways part
    //> jsr RenderSidewaysPipe
    val carrySet = renderSidewaysPipe(x, 0x0a)
    //> bcs NoBlankP             ;if carry flag set, not time to draw vertical pipe part
    if (!carrySet) {
        //> ldx #$06                 ;blank everything above the vertical pipe part
        //> VPipeSectLoop: lda #$00                 ;all the way to the top of the screen
        //> sta MetatileBuffer,x     ;because otherwise it will look like exit pipe
        //> dex
        //> bpl VPipeSectLoop
        for (i in 6 downTo 0) {
            ram.metatileBuffer[i] = 0x00.toUByte()
        }
        //> lda VerticalPipeData,y   ;draw the end of the vertical pipe part
        //> sta MetatileBuffer+7
        // After RenderSidewaysPipe, Y still holds the shaft data index.
        // For IntroPipe, the AreaObjectLength column determines Y.
        // When shaft is drawn (length=0 or 1), Y from the pipe data is the length offset.
        // Actually let me trace: after RenderSidewaysPipe returns with carry clear:
        //   - The vertical shaft was drawn, meaning SidePipeShaftData[y] != 0
        //   - y = AreaObjectLength (the horizontal offset, 0 or 1 at this point)
        // Actually RenderSidewaysPipe is complex. Let me re-read.
        // For IntroPipe with inputLength=0x0a:
        //   $05 = 0x0a - 2 = 8 (vertical length of shaft)
        //   $06 = AreaObjectLength (horizontal offset: 3, 2, 1, or 0)
        //   x = $05 + 1 = 9
        //   SidePipeShaftData[$06]: if $06 >= 2, value is $00 (no shaft drawn), carry stays set
        //   If $06 < 2, shaft is drawn: RenderUnderPart called, carry cleared
        //
        // So carry is clear only when $06 = 0 or 1 (last two columns)
        // At that point, Y = $06 after the pipe drawing.
        // VerticalPipeData,y where y = 0 gives $11, y = 1 gives $10.
        val horizOffset = ram.areaObjectLength[x].toInt() and 0xFF
        ram.metatileBuffer[7] = VerticalPipeData.getOrElse(horizOffset) { 0x15u }
    }
    //> NoBlankP: rts
}

//> ExitPipe:
private fun System.exitPipe() {
    val x = ram.objectOffset
    //> ldy #$03                 ;check if length set, if not set, set it
    //> jsr ChkLrgObjFixedLength
    chkLrgObjFixedLength(x, 0x03)
    //> jsr GetLrgObjAttrib      ;get vertical length, then plow on through RenderSidewaysPipe
    val attrib = getLrgObjAttrib(x)
    //> (falls through to RenderSidewaysPipe with Y = attrib.length)
    renderSidewaysPipe(x, attrib.length.toInt() and 0xFF)
}

// by Claude - RenderSidewaysPipe: renders a sideways pipe section
// Returns true if carry flag is set (shaft not drawn), false if carry clear (shaft drawn)
private fun System.renderSidewaysPipe(objOffset: Byte, inputLength: Int): Boolean {
    //> RenderSidewaysPipe:
    //> dey; dey                       ;decrement twice to make room for shaft at bottom
    //> sty $05                       ;and store here for now as vertical length
    val vertLen = inputLength - 2
    //> ldy AreaObjectLength,x    ;get length left over and store here
    //> sty $06
    val horizOffset = ram.areaObjectLength[objOffset].toInt() and 0xFF
    //> ldx $05; inx                   ;get vertical length plus one, use as buffer offset
    var bufX = vertLen + 1
    //> lda SidePipeShaftData,y   ;check for value $00 based on horizontal offset
    //> cmp #$00
    //> beq DrawSidePart          ;if found, do not draw the vertical pipe shaft
    var carrySet = true
    if (horizOffset in SidePipeShaftData.indices && SidePipeShaftData[horizOffset] != 0.toUByte()) {
        //> ldx #$00
        //> ldy $05                   ;init buffer offset and get vertical length
        //> jsr RenderUnderPart       ;and render vertical shaft using tile number in A
        renderUnderPart(SidePipeShaftData[horizOffset], 0, vertLen)
        //> clc                       ;clear carry flag to be used by IntroPipe
        carrySet = false
        bufX = vertLen + 1
    }
    //> DrawSidePart: ldy $06                   ;render side pipe part at the bottom
    //> lda SidePipeTopPart,y
    //> sta MetatileBuffer,x      ;note that the pipe parts are stored
    ram.metatileBuffer[bufX] = SidePipeTopPart[horizOffset]
    //> lda SidePipeBottomPart,y  ;backwards horizontally
    //> sta MetatileBuffer+1,x
    ram.metatileBuffer[bufX + 1] = SidePipeBottomPart[horizOffset]
    //> rts
    return carrySet
}

//> FlagBalls_Residual:
private fun System.flagBalls_Residual() {
    val x = ram.objectOffset
    //> jsr GetLrgObjAttrib  ;get low nybble from object byte
    val attrib = getLrgObjAttrib(x)
    //> ldx #$02             ;render flag balls on third row from top
    //> lda #$6d             ;of screen downwards based on low nybble
    //> jmp RenderUnderPart
    renderUnderPart(0x6d.toUByte(), 0x02, attrib.length.toInt() and 0xFF)
}

//> FlagpoleObject:
private fun System.flagpoleObject() {
    //> lda #$24                 ;render flagpole ball on top
    //> sta MetatileBuffer
    ram.metatileBuffer[0] = 0x24.toUByte()
    //> ldx #$01                 ;now render the flagpole shaft
    //> ldy #$08
    //> lda #$25
    //> jsr RenderUnderPart
    renderUnderPart(0x25.toUByte(), 0x01, 0x08)
    //> lda #$61                 ;render solid block at the bottom
    //> sta MetatileBuffer+10
    ram.metatileBuffer[10] = 0x61.toUByte()
    //> jsr GetAreaObjXPosition
    val xPos = getAreaObjXPosition().toInt() and 0xFF
    //> sec
    //> sbc #$08                 ;get pixel coordinate of where the flagpole is,
    val flagX = (xPos - 0x08) and 0xFF
    //> sta Enemy_X_Position+5   ;subtract eight pixels and use as horizontal coordinate for the flag
    ram.sprObjXPos[6] = flagX.toByte()
    //> lda CurrentPageLoc
    //> sbc #$00                 ;subtract borrow from page location and use as
    val borrow = if (xPos < 0x08) 1 else 0
    //> sta Enemy_PageLoc+5      ;page location for the flag
    ram.sprObjPageLoc[6] = ((ram.currentPageLoc.toInt() - borrow) and 0xFF).toByte()
    //> lda #$30
    //> sta Enemy_Y_Position+5   ;set vertical coordinate for flag
    ram.sprObjYPos[6] = 0x30
    //> lda #$b0
    //> sta FlagpoleFNum_Y_Pos   ;set initial vertical coordinate for flagpole's floatey number
    ram.flagpoleFNumYPos = 0xb0.toByte()
    //> lda #FlagpoleFlagObject
    //> sta Enemy_ID+5           ;set flag identifier, note that identifier and coordinates
    ram.enemyID[5] = EnemyId.FlagpoleFlagObject.byte
    //> inc Enemy_Flag+5         ;use last space in enemy object buffer
    ram.enemyFlags[5] = (ram.enemyFlags[5] + 1).toByte()
    //> rts
}

//> CastleBridgeObj:
// by Claude - castle bridge: 13 columns of bridge tiles
private fun System.castleBridgeObj() {
    val x = ram.objectOffset
    //> ldy #$0c                  ;load length of 13 columns
    //> jsr ChkLrgObjFixedLength
    chkLrgObjFixedLength(x, 0x0c)
    //> jmp ChainObj              ;$00=4, C_Object index = 4-2 = 2
    chainObjWithIndex(2)
}

//> AxeObj:
// by Claude - sets bowser palette; the chainObjWithIndex call is made by the when dispatcher
private fun System.axeObj() {
    //> lda #$08                  ;load bowser's palette into sprite portion of palette
    //> sta VRAM_Buffer_AddrCtrl
    ram.vRAMBufferAddrCtrl = 0x08
    //> (falls through to ChainObj — handled by caller in when block)
}

// by Claude - actual chainObj implementation with the C_Object index
private fun System.chainObjWithIndex(cIndex: Int) {
    //> ChainObj:
    //> ldy $00                   ;get value loaded earlier from decoder
    //> ldx C_ObjectRow-2,y       ;get appropriate row and metatile for object
    val row = C_ObjectRow[cIndex].toInt() and 0xFF
    //> lda C_ObjectMetatile-2,y
    val metatile = C_ObjectMetatile[cIndex]
    //> ColObj: ldy #$00             ;column length of 1
    //> jmp RenderUnderPart
    renderUnderPart(metatile, row, 0)
}

//> EmptyBlock:
private fun System.emptyBlock() {
    val x = ram.objectOffset
    //> jsr GetLrgObjAttrib  ;get row location
    val attrib = getLrgObjAttrib(x)
    //> ldx $07
    val row = attrib.row.toInt() and 0xFF
    //> lda #$c4
    //> ColObj: ldy #$00             ;column length of 1
    //> jmp RenderUnderPart
    renderUnderPart(0xc4.toUByte(), row, 0)
}

//> RowOfCoins:
private fun System.rowOfCoins() {
    //> ldy AreaType            ;get area type
    //> lda CoinMetatileData,y  ;load appropriate coin metatile
    val metatile = CoinMetatileData[ram.areaType.ordinal]
    //> jmp GetRow
    getRow(metatile)
}

//> RowOfBricks:
private fun System.rowOfBricks() {
    //> ldy AreaType           ;load area type obtained from area offset pointer
    var y = ram.areaType.ordinal
    //> lda CloudTypeOverride  ;check for cloud type override
    //> beq DrawBricks
    if (ram.cloudTypeOverride) {
        //> ldy #$04               ;if cloud type, override area type
        y = 4
    }
    //> DrawBricks: lda BrickMetatiles,y   ;get appropriate metatile
    val metatile = BrickMetatiles[y]
    //> jmp GetRow             ;and go render it
    getRow(metatile)
}

//> RowOfSolidBlocks:
private fun System.rowOfSolidBlocks() {
    //> ldy AreaType               ;load area type obtained from area offset pointer
    val y = ram.areaType.ordinal
    //> lda SolidBlockMetatiles,y  ;get metatile
    val metatile = SolidBlockMetatiles[y]
    //> GetRow: (falls through)
    getRow(metatile)
}

// by Claude - GetRow: shared row rendering (pha, ChkLrgObjLength, DrawRow)
private fun System.getRow(metatile: UByte) {
    val x = ram.objectOffset
    //> GetRow: pha                        ;store metatile here
    //> jsr ChkLrgObjLength        ;get row number, load length
    val (attrib, _) = chkLrgObjLength(x)
    //> DrawRow: ldx $07
    val row = attrib.row.toInt() and 0xFF
    //> ldy #$00                   ;set vertical height of 1
    //> pla
    //> jmp RenderUnderPart        ;render object
    renderUnderPart(metatile, row, 0)
}

//> ColumnOfBricks:
private fun System.columnOfBricks() {
    //> ldy AreaType          ;load area type obtained from area offset
    val y = ram.areaType.ordinal
    //> lda BrickMetatiles,y  ;get metatile (no cloud override as for row)
    val metatile = BrickMetatiles[y]
    //> jmp GetRow2
    getRow2(metatile)
}

//> ColumnOfSolidBlocks:
private fun System.columnOfSolidBlocks() {
    //> ldy AreaType               ;load area type obtained from area offset
    val y = ram.areaType.ordinal
    //> lda SolidBlockMetatiles,y  ;get metatile
    val metatile = SolidBlockMetatiles[y]
    //> GetRow2: (falls through)
    getRow2(metatile)
}

// by Claude - GetRow2: shared column rendering (pha, GetLrgObjAttrib, pla, RenderUnderPart)
private fun System.getRow2(metatile: UByte) {
    val x = ram.objectOffset
    //> GetRow2: pha                        ;save metatile to stack for now
    //> jsr GetLrgObjAttrib        ;get length and row
    val attrib = getLrgObjAttrib(x)
    //> pla                        ;restore metatile
    //> ldx $07                    ;get starting row
    val row = attrib.row.toInt() and 0xFF
    //> jmp RenderUnderPart        ;now render the column
    renderUnderPart(metatile, row, attrib.length.toInt() and 0xFF)
}

//> BulletBillCannon:
private fun System.bulletBillCannon() {
    val x = ram.objectOffset
    //> jsr GetLrgObjAttrib      ;get row and length of bullet bill cannon
    val attrib = getLrgObjAttrib(x)
    //> ldx $07                  ;start at first row
    var bufX = attrib.row.toInt() and 0xFF
    var lengthY = attrib.length.toInt() and 0xFF
    //> lda #$64                 ;render bullet bill cannon
    //> sta MetatileBuffer,x
    ram.metatileBuffer[bufX] = 0x64.toUByte()
    //> inx
    bufX++
    //> dey                      ;done yet?
    lengthY--
    //> bmi SetupCannon
    if (lengthY >= 0) {
        //> lda #$65                 ;if not, render middle part
        //> sta MetatileBuffer,x
        ram.metatileBuffer[bufX] = 0x65.toUByte()
        //> inx
        bufX++
        //> dey                      ;done yet?
        lengthY--
        //> bmi SetupCannon
        if (lengthY >= 0) {
            //> lda #$66                 ;if not, render bottom until length expires
            //> jsr RenderUnderPart
            renderUnderPart(0x66.toUByte(), bufX, lengthY)
        }
    }
    //> SetupCannon: ldx Cannon_Offset        ;get offset for data used by cannons and whirlpools
    val cannonIdx = ram.cannonOffset.toInt() and 0xFF
    //> jsr GetAreaObjYPosition  ;get proper vertical coordinate for cannon
    //> sta Cannon_Y_Position,x  ;and store it here
    ram.cannonYPositions[cannonIdx] = getAreaObjYPosition(attrib.row)
    //> lda CurrentPageLoc
    //> sta Cannon_PageLoc,x     ;store page number for cannon here
    ram.cannonPageLocs[cannonIdx] = ram.currentPageLoc.toByte()
    //> jsr GetAreaObjXPosition  ;get proper horizontal coordinate for cannon
    //> sta Cannon_X_Position,x  ;and store it here
    ram.cannonXPositions[cannonIdx] = getAreaObjXPosition()
    //> inx
    var newOffset = cannonIdx + 1
    //> cpx #$06                 ;increment and check offset
    //> bcc StrCOffset           ;if not yet reached sixth cannon, branch to save offset
    if (newOffset >= 6) {
        //> ldx #$00                 ;otherwise initialize it
        newOffset = 0
    }
    //> StrCOffset: stx Cannon_Offset        ;save new offset and leave
    ram.cannonOffset = newOffset.toByte()
    //> rts
}

//> StaircaseObject:
private fun System.staircaseObject() {
    val x = ram.objectOffset
    //> jsr ChkLrgObjLength       ;check and load length
    val (_, fixedLen) = chkLrgObjLength(x)
    //> bcc NextStair             ;if length already loaded, skip init part
    if (fixedLen.justStarting) {
        //> lda #$09                  ;start past the end for the bottom
        //> sta StaircaseControl      ;of the staircase
        ram.staircaseControl = 0x09
    }
    //> NextStair: dec StaircaseControl      ;move onto next step (or first if starting)
    ram.staircaseControl--
    //> ldy StaircaseControl
    val step = ram.staircaseControl.toInt() and 0xFF
    //> ldx StaircaseRowData,y    ;get starting row and height to render
    val startRow = StaircaseRowData.getOrElse(step) { 0x03 }.toInt() and 0xFF
    //> lda StaircaseHeightData,y
    //> tay
    val height = StaircaseHeightData.getOrElse(step) { 0x00 }.toInt() and 0xFF
    //> lda #$61                  ;now render solid block staircase
    //> jmp RenderUnderPart
    renderUnderPart(0x61.toUByte(), startRow, height)
}

//> Jumpspring:
private fun System.jumpspring() {
    val x = ram.objectOffset
    //> jsr GetLrgObjAttrib
    val attrib = getLrgObjAttrib(x)
    val row = attrib.row.toInt() and 0xFF
    //> jsr FindEmptyEnemySlot      ;find empty space in enemy object buffer
    val slot = findEmptyEnemySlot() ?: return
    //> jsr GetAreaObjXPosition     ;get horizontal coordinate for jumpspring
    //> sta Enemy_X_Position,x      ;and store
    ram.sprObjXPos[1 + slot] = getAreaObjXPosition()
    //> lda CurrentPageLoc          ;store page location of jumpspring
    //> sta Enemy_PageLoc,x
    ram.sprObjPageLoc[1 + slot] = ram.currentPageLoc.toByte()
    //> jsr GetAreaObjYPosition     ;get vertical coordinate for jumpspring
    //> sta Enemy_Y_Position,x      ;and store
    val yPos = getAreaObjYPosition(attrib.row)
    ram.sprObjYPos[1 + slot] = yPos
    //> sta Jumpspring_FixedYPos,x  ;store as permanent coordinate here
    // Jumpspring_FixedYPos at $58+x = sprObjXSpeed[1+slot]
    ram.sprObjXSpeed[1 + slot] = yPos
    //> lda #JumpspringObject
    //> sta Enemy_ID,x              ;write jumpspring object to enemy object buffer
    ram.enemyID[slot] = EnemyId.JumpspringObject.byte
    //> ldy #$01
    //> sty Enemy_Y_HighPos,x       ;store vertical high byte
    ram.sprObjYHighPos[1 + slot] = 0x01
    //> inc Enemy_Flag,x            ;set flag for enemy object buffer
    ram.enemyFlags[slot] = (ram.enemyFlags[slot] + 1).toByte()
    //> ldx $07
    //> lda #$67                    ;draw metatiles in two rows where jumpspring is
    //> sta MetatileBuffer,x
    if (row in ram.metatileBuffer.indices) ram.metatileBuffer[row] = 0x67.toUByte()
    //> lda #$68
    //> sta MetatileBuffer+1,x
    if (row + 1 in ram.metatileBuffer.indices) ram.metatileBuffer[row + 1] = 0x68.toUByte()
    //> rts
}

//> Hidden1UpBlock:
private fun System.hidden1UpBlock() {
    //> lda Hidden1UpFlag  ;if flag not set, do not render object
    //> beq ExitDecBlock
    if (!ram.hidden1UpFlag) return
    //> lda #$00           ;if set, init for the next one
    //> sta Hidden1UpFlag
    ram.hidden1UpFlag = false
    //> jmp BrickWithItem  ;jump to code shared with unbreakable bricks
    brickWithItem()
}

//> QuestionBlock:
private fun System.questionBlock() {
    //> jsr GetAreaObjectID ;get value from level decoder routine
    val objId = getAreaObjectID()
    //> jmp DrawQBlk        ;go to render it
    drawQBlk(objId)
}

//> BrickWithCoins:
private fun System.brickWithCoins() {
    //> lda #$00                 ;initialize multi-coin timer flag
    //> sta BrickCoinTimerFlag
    ram.brickCoinTimerFlag = 0
    //> (falls through to BrickWithItem)
    brickWithItem()
}

//> BrickWithItem:
private fun System.brickWithItem() {
    val x = ram.objectOffset
    //> jsr GetAreaObjectID         ;save area object ID
    val objId = getAreaObjectID()
    //> sty $07
    // ($07 = objId — used as temporary)
    //> lda #$00                    ;load default adder for bricks with lines
    var adder = 0
    //> ldy AreaType                ;check level type for ground level
    //> dey
    //> beq BWithL                  ;if ground type, do not start with 5
    if (ram.areaType != AreaType.Ground) {
        //> lda #$05                    ;otherwise use adder for bricks without lines
        adder = 5
    }
    //> BWithL: clc                         ;add object ID to adder
    //> adc $07
    //> tay                         ;use as offset for metatile
    val metatileIdx = adder + objId
    //> DrawQBlk: (falls through)
    drawQBlk(metatileIdx)
}

// by Claude - GetAreaObjectID: get value saved from area parser routine
private fun System.getAreaObjectID(): Int {
    //> GetAreaObjectID:
    //> lda $00    ;get value saved from area parser routine
    //> sec
    //> sbc #$00   ;possibly residual code
    //> tay        ;save to Y
    //> ExitDecBlock: rts
    // $00 holds the decoded object identifier. In the Kotlin dispatch, for small objects
    // (rows $00-$0b with d6-d4 clear), temp07 = 0x16 and the when cases are 0x16..0x21.
    // So objId = when_case - 0x16. For QuestionBlock cases: 0x16-0x16=0, 0x17-0x16=1, 0x18-0x16=2.
    // For Hidden1UpBlock: 0x19-0x16=3. BrickWithItem: 0x1A-0x16=4, etc.
    // But we need the value of $00 which is the objId before temp07 is added.
    // Since we can't access it directly, we need to thread it through.
    // For now, I'll use a field to pass the value. See the refactored when block.
    return areaParserObjId
}

// by Claude - DrawQBlk: shared rendering for question blocks and bricks
private fun System.drawQBlk(metatileIdx: Int) {
    val x = ram.objectOffset
    //> DrawQBlk: lda BrickQBlockMetatiles,y  ;get appropriate metatile for brick/question block
    val table = brickQBlockMetatiles()
    val metatile = table[metatileIdx.coerceIn(0, table.lastIndex)]
    //> pha                         ;save
    //> jsr GetLrgObjAttrib         ;get row from location byte
    val attrib = getLrgObjAttrib(x)
    //> jmp DrawRow
    //> DrawRow: ldx $07
    val row = attrib.row.toInt() and 0xFF
    //> ldy #$00                   ;set vertical height of 1
    //> pla
    //> jmp RenderUnderPart        ;render object
    renderUnderPart(metatile, row, 0)
}

//> Hole_Empty:
private fun System.hole_Empty() {
    val x = ram.objectOffset
    //> jsr ChkLrgObjLength          ;get lower nybble and save as length
    val (attrib, fixedLen) = chkLrgObjLength(x)
    //> bcc NoWhirlP                 ;skip this part if length already loaded
    if (fixedLen.justStarting) {
        //> lda AreaType                 ;check for water type level
        //> bne NoWhirlP                 ;if not water type, skip this part
        if (ram.areaType == AreaType.Water) {
            //> ldx Whirlpool_Offset         ;get offset for data used by cannons and whirlpools
            val wpIdx = ram.cannonOffset.toInt() and 0xFF // whirlpool shares same offset as cannon
            //> jsr GetAreaObjXPosition      ;get proper horizontal coordinate of where we're at
            val xPos = getAreaObjXPosition().toInt() and 0xFF
            //> sec
            //> sbc #$10                     ;subtract 16 pixels
            val leftExtent = (xPos - 0x10) and 0xFF
            //> sta Whirlpool_LeftExtent,x   ;store as left extent of whirlpool
            ram.cannonXPositions[wpIdx] = leftExtent.toByte()
            //> lda CurrentPageLoc           ;get page location of where we're at
            //> sbc #$00                     ;subtract borrow
            val borrow = if (xPos < 0x10) 1 else 0
            //> sta Whirlpool_PageLoc,x      ;save as page location of whirlpool
            ram.cannonPageLocs[wpIdx] = ((ram.currentPageLoc.toInt() - borrow) and 0xFF).toByte()
            //> iny; iny                     ;increment length by 2
            val extendedLen = (attrib.length.toInt() and 0xFF) + 2
            //> tya; asl; asl; asl; asl      ;multiply by 16 to get size of whirlpool
            val wpLength = (extendedLen shl 4) and 0xFF
            //> sta Whirlpool_Length,x       ;save size of whirlpool here
            ram.cannonYPositions[wpIdx] = wpLength.toByte()
            //> inx
            var newOffset = wpIdx + 1
            //> cpx #$05                     ;increment and check offset
            //> bcc StrWOffset               ;if not yet reached fifth whirlpool, branch to save offset
            if (newOffset >= 5) {
                //> ldx #$00                     ;otherwise initialize it
                newOffset = 0
            }
            //> StrWOffset: stx Whirlpool_Offset         ;save new offset here
            ram.cannonOffset = newOffset.toByte() // whirlpoolOffset is same field
        }
    }
    //> NoWhirlP: ldx AreaType                 ;get appropriate metatile, then
    val areaTypeOrd = ram.areaType.ordinal
    //> lda HoleMetatiles,x          ;render the hole proper
    val metatile = HoleMetatiles[areaTypeOrd]
    //> ldx #$08
    //> ldy #$0f                     ;start at ninth row and go to bottom, run RenderUnderPart
    //> (falls through to RenderUnderPart)
    renderUnderPart(metatile, 0x08, 0x0f)
}

// by Claude - field used to pass $00 (the decoded object ID) from the when dispatch to object routines
// internal so verticalPipe.kt (in areaparser package) can access it
internal var areaParserObjId: Int = 0