@file:OptIn(ExperimentalUnsignedTypes::class)

package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
import com.ivieleague.smbtranslation.Constants.World8
import com.ivieleague.smbtranslation.areaparser.verticalPipe
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

//> ForeSceneryData:
val ForeSceneryData = ubyteArrayOf(
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

//> TerrainMetatiles:
//> .db $69, $54, $52, $62
val TerrainMetatiles = ubyteArrayOf(0x69u, 0x54u, 0x52u, 0x62u)

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
            val v = ForeSceneryData[y]
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
    if (ram.areaType == 0.toByte() && ram.worldNumber == World8) {
        //> lda #$62                   ;if set as water level and world number eight,
        aTerrain = 0x62u
        //> jmp StoreMT                ;use castle wall metatile as terrain type
    } else {
        //> TerMTile: lda TerrainMetatiles,y     ;otherwise get appropriate metatile for area type
        aTerrain = TerrainMetatiles[ram.areaType]
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
            if (ram.areaType == 0x02.toByte() && x == 0x0B.toByte()) {
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
        buffer[offset + yStore] = a.toByte()
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
        var y = ram.areaDataOffset
        //> lda (AreaData),y         ;get first byte of area object
        //> cmp #$fd                 ;if end-of-area, skip all this crap
        //> beq RdyDecode
        //> lda AreaObjectLength,x   ;check area object buffer flag
        //> bpl RdyDecode            ;if buffer not negative, branch, otherwise
        var behind: Boolean? = false
        if (ram.areaData!![y] != 0xFD.toByte() && ram.areaObjectLength[x] < 0.toByte()) {
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
            //> and #$0f                 ;mask out high nybble
            val tmp = ram.areaData!![y] and 0xf
            //> cmp #$0d                 ;row 13?
            //> bne Chk1Row14
            if(tmp == 0xd.toByte()) {
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
                if(tmp != 0xe.toByte() || !ram.backloadingFlag) {
                    //> CheckRear:  lda AreaObjectPageLoc    ;check to see if current page of level object is
                    //> cmp CurrentPageLoc       ;behind current page of renderer
                    //> bcc SetBehind            ;if so branch
                    if (ram.areaObjectPageLoc.toUByte() < ram.currentPageLoc) behind = true
                }
            }
        }
        if(behind == false) {
            //> RdyDecode:  jsr DecodeAreaData       ;do sub and do not turn on flag
            decodeAreaData(x, y)
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
        //> lda AreaObjectLength,x   ;check object length for anything stored here
        //> bmi ProcLoopb            ;if not, branch to handle loopback
        if(ram.areaObjectLength[ram.objectOffset] >= 0.toByte()) {
            //> dec AreaObjectLength,x   ;otherwise decrement length or get rid of it
            ram.areaObjectLength[ram.objectOffset]--
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
    //> Chk1stB:  ldx #$10                   ;load offset of 16 for special row 15
    var add: Byte = 0x10
    //> lda (AreaData),y           ;get first byte of level object again
    var a: Byte = ram.areaData!![y]
    //> cmp #$fd
    //> beq EndAParse              ;if end of level, leave this routine
    if (a == 0xFD.toByte()) return
    //> and #$0f                   ;otherwise, mask out low nybble
    val row: Byte = a and 0x0F
    //> cmp #$0f                   ;row 15?
    //> beq ChkRow14               ;if so, keep the offset of 16
    if (row != 0x0F.toByte()) {
        //> ldx #$08                   ;otherwise load offset of 8 for special row 12
        add = 0x08
        //> cmp #$0c                   ;row 12?
        //> beq ChkRow14               ;if so, keep the offset value of 8
        if (row != 0x0C.toByte()) {
            //> ldx #$00                   ;otherwise nullify value by default
            add = 0x00
        }
    }
    //> ChkRow14: stx $07                    ;store whatever value we just loaded here
    var temp07: Byte = add
    //> ldx ObjectOffset           ;get object offset again
    //> cmp #$0e                   ;row 14?
    //> bne ChkRow13
    if (row == 0x0E.toByte()) {
        //> lda #$00                   ;if so, load offset with $00
        //> sta $07
        temp07 = 0x00
        //> lda #$2e                   ;and load A with another value
        a = 0x2E.toByte()
        //> bne NormObj                ;unconditional branch
        // jump to NormObj
    } else if (row == 0x0D.toByte()) {
        //> ChkRow13: cmp #$0d                   ;row 13?
        //> bne ChkSRows
        //> lda #$22                   ;if so, load offset with 34
        //> sta $07
        temp07 = 0x22
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
        if (row < 0x0C.toByte()) {
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
                //> sta $07                    ;otherwise set offset of 24 for small object
                temp07 = 0x16
                //> lda (AreaData),y           ;reload second byte of level object
                a = ram.areaData!![y]
                //> and #%00001111             ;mask out higher nybble and jump
                a = a and 0x0F
                //> jmp NormObj
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
                // fallthrough to MoveAOId below
            }
        } else {
            //> SpecObj:  iny                        ;branch here for rows 12-15
            y++
            //> lda (AreaData),y
            a = ram.areaData!![y]
            //> and #%01110000             ;get next byte and mask out all but d6-d4
            a = a and 0b0111_0000.toByte()
            // fallthrough to MoveAOId
        }
        //> MoveAOId:  lsr                        ;move d6-d4 to lower nybble
        //>           lsr
        //>           lsr
        //>           lsr
        a = (a.toUByte() shr 4.toUByte()).toByte()
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
            a = ram.areaData!![y]
            //> and #%00001111
            val lowN: Byte = a and 0x0F.toByte()
            //> cmp #$0e                   ;row 14?
            //> bne LeavePar
            if (lowN != 0x0E.toByte()) return
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
            //> lda (AreaData),y
            a = ram.areaData!![y]
            //> and #%11110000             ;mask out low nybble and move high to low
            //> lsr
            //> lsr
            //> lsr
            //> lsr
            val col: Byte = a and 0xF0.toByte() ushr 4
            //> cmp CurrentColumnPos       ;is this where we're at?
            //> bne LeavePar               ;if not, branch to leave
            if (col != ram.currentColumnPos.toByte()) return
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

        //> ;small objects (rows $00-$0b or 00-11, d6-d4 all clear)
        //> .dw QuestionBlock     ;power-up
        0x16 -> questionBlock()
        //> .dw QuestionBlock     ;coin
        0x17 -> questionBlock()
        //> .dw QuestionBlock     ;hidden, coin
        0x18 -> questionBlock()
        //> .dw Hidden1UpBlock    ;hidden, 1-up
        0x19 -> hidden1UpBlock()
        //> .dw BrickWithItem     ;brick, power-up
        0x1A -> brickWithItem()
        //> .dw BrickWithItem     ;brick, vine
        0x1B -> brickWithItem()
        //> .dw BrickWithItem     ;brick, star
        0x1C -> brickWithItem()
        //> .dw BrickWithCoins    ;brick, coins
        0x1D -> brickWithCoins()
        //> .dw BrickWithItem     ;brick, 1-up
        0x1E -> brickWithItem()
        //> .dw WaterPipe
        0x1F -> waterPipe()
        //> .dw EmptyBlock
        0x20 -> emptyBlock()
        //> .dw Jumpspring
        0x21 -> jumpspring()

        //> ;objects for special row $0d or 13 (d6 set)
        //> .dw IntroPipe
        0x22 -> introPipe()
        //> .dw FlagpoleObject
        0x23 -> flagpoleObject()
        //> .dw AxeObj
        0x24 -> axeObj()
        //> .dw ChainObj
        0x25 -> chainObj()
        //> .dw CastleBridgeObj
        0x26 -> castleBridgeObj()
        //> .dw ScrollLockObject_Warp
        0x27 -> scrollLockObject_Warp()
        //> .dw ScrollLockObject
        0x28 -> scrollLockObject()
        //> .dw ScrollLockObject
        0x29 -> scrollLockObject()
        //> .dw AreaFrenzy            ;flying cheep-cheeps
        0x2A -> areaFrenzy()
        //> .dw AreaFrenzy            ;bullet bills or swimming cheep-cheeps
        0x2B -> areaFrenzy()
        //> .dw AreaFrenzy            ;stop frenzy
        0x2C -> areaFrenzy()
        //> .dw LoopCmdE
        0x2D -> loopCmdE()

        //> ;object for special row $0e or 14
        //> .dw AlterAreaAttributes
        0x2E -> alterAreaAttributes()
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

private fun System.areaStyleObject(): Unit { /*TODO*/ }
private fun System.rowOfBricks(): Unit { /*TODO*/ }
private fun System.rowOfSolidBlocks(): Unit { /*TODO*/ }
private fun System.rowOfCoins(): Unit { /*TODO*/ }
private fun System.columnOfBricks(): Unit { /*TODO*/ }
private fun System.columnOfSolidBlocks(): Unit { /*TODO*/ }
private fun System.hole_Empty(): Unit { /*TODO*/ }
private fun System.pulleyRopeObject(): Unit { /*TODO*/ }
private fun System.bridge_High(): Unit { /*TODO*/ }
private fun System.bridge_Middle(): Unit { /*TODO*/ }
private fun System.bridge_Low(): Unit { /*TODO*/ }
private fun System.hole_Water(): Unit { /*TODO*/ }
private fun System.questionBlockRow_High(): Unit { /*TODO*/ }
private fun System.questionBlockRow_Low(): Unit { /*TODO*/ }
private fun System.endlessRope(): Unit { /*TODO*/ }
private fun System.balancePlatRope(): Unit { /*TODO*/ }
private fun System.castleObject(): Unit { /*TODO*/ }
private fun System.staircaseObject(): Unit { /*TODO*/ }
private fun System.exitPipe(): Unit { /*TODO*/ }
private fun System.flagBalls_Residual(): Unit { /*TODO*/ }
private fun System.questionBlock(): Unit { /*TODO*/ }
private fun System.hidden1UpBlock(): Unit { /*TODO*/ }
private fun System.brickWithItem(): Unit { /*TODO*/ }
private fun System.brickWithCoins(): Unit { /*TODO*/ }
private fun System.waterPipe(): Unit { /*TODO*/ }
private fun System.emptyBlock(): Unit { /*TODO*/ }
private fun System.jumpspring(): Unit { /*TODO*/ }
private fun System.introPipe(): Unit { /*TODO*/ }
private fun System.flagpoleObject(): Unit { /*TODO*/ }
private fun System.axeObj(): Unit { /*TODO*/ }
private fun System.chainObj(): Unit { /*TODO*/ }
private fun System.castleBridgeObj(): Unit { /*TODO*/ }
private fun System.scrollLockObject_Warp(): Unit { /*TODO*/ }
private fun System.scrollLockObject(): Unit { /*TODO*/ }
private fun System.areaFrenzy(): Unit { /*TODO*/ }
private fun System.loopCmdE(): Unit { /*TODO*/ }
private fun System.alterAreaAttributes(): Unit { /*TODO*/ }