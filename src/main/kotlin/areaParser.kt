package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.Constants.World8
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
    areaParserTasks()
    //> dec AreaParserTaskNum     ;if all tasks not complete do not
    //> bne SkipATRender          ;render attribute table yet
    if (--ram.areaParserTaskNum != 0.toByte()) return
    //> jsr RenderAttributeTables
    renderAttributeTables()
    //> SkipATRender: rts
}

fun System.areaParserTasks() {
    //> AreaParserTasks:
    //> jsr JumpEngine

    when(ram.operModeTask) {
        //> .dw IncrementColumnPos
        0.toByte() -> Unit //TODO: incrementColumnPos()
        //> .dw RenderAreaGraphics
        1.toByte() -> renderAreaGraphics()
        //> .dw RenderAreaGraphics
        2.toByte() -> renderAreaGraphics()
        //> .dw AreaParserCore
        3.toByte() -> Unit //TODO: areaParserCore()
        //> .dw IncrementColumnPos
        4.toByte() -> Unit //TODO: incrementColumnPos()
        //> .dw RenderAreaGraphics
        5.toByte() -> renderAreaGraphics()
        //> .dw RenderAreaGraphics
        6.toByte() -> renderAreaGraphics()
        //> .dw AreaParserCore
        7.toByte() -> Unit //TODO: areaParserCore()
    }
}

//private fun System.incrementColumnPos() {
//    //> IncrementColumnPos:
//    //> inc CurrentColumnPos     ;increment column where we're at
//    //> lda CurrentColumnPos
//    //> and #%00001111           ;mask out higher nybble
//    //> bne NoColWrap
//    val newColumn = ++ram.currentColumnPos and 0b1111
//    if (newColumn == 0.toByte()) {
//        //> sta CurrentColumnPos     ;if no bits left set, wrap back to zero (0-f)
//        ram.currentColumnPos = newColumn
//        //> inc CurrentPageLoc       ;and increment page number where we're at
//        ram.currentPageLoc++
//    }
//    //> NoColWrap: inc BlockBufferColumnPos ;increment column offset where we're at
//    //> lda BlockBufferColumnPos
//    //> and #%00011111           ;mask out all but 5 LSB (0-1f)
//    //> sta BlockBufferColumnPos ;and save
//    ram.blockBufferColumnPos = ((ram.blockBufferColumnPos + 1) and 0b11111).toByte()
//    //> rts
//}
//
////> ;$00 - used as counter, store for low nybble for background, ceiling byte for terrain
////> ;$01 - used to store floor byte for terrain
////> ;$07 - used to store terrain metatile
////> ;$06-$07 - used to store block buffer address
//
////> BSceneDataOffsets:
////> .db $00, $30, $60
//val BSceneDataOffsets = byteArrayOf(0x0, 0x30, 0x60)
//
////> BackSceneryData:
//val BackSceneryData = byteArrayOf(
////> .db $93, $00, $00, $11, $12, $12, $13, $00 ;clouds
//    0x93.toByte(), 0x00.toByte(), 0x00.toByte(), 0x11.toByte(), 0x12.toByte(), 0x12.toByte(), 0x13.toByte(), 0x00.toByte(),
////> .db $00, $51, $52, $53, $00, $00, $00, $00
//    0x00.toByte(), 0x51.toByte(), 0x52.toByte(), 0x53.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
////> .db $00, $00, $01, $02, $02, $03, $00, $00
//    0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x02.toByte(), 0x02.toByte(), 0x03.toByte(), 0x00.toByte(), 0x00.toByte(),
////> .db $00, $00, $00, $00, $91, $92, $93, $00
//    0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x91.toByte(), 0x92.toByte(), 0x93.toByte(), 0x00.toByte(),
////> .db $00, $00, $00, $51, $52, $53, $41, $42
//    0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x51.toByte(), 0x52.toByte(), 0x53.toByte(), 0x41.toByte(), 0x42.toByte(),
////> .db $43, $00, $00, $00, $00, $00, $91, $92
//    0x43.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x91.toByte(), 0x92.toByte(),
//
////> .db $97, $87, $88, $89, $99, $00, $00, $00 ;mountains and bushes
//    0x97.toByte(), 0x87.toByte(), 0x88.toByte(), 0x89.toByte(), 0x99.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
////> .db $11, $12, $13, $a4, $a5, $a5, $a5, $a6
//    0x11.toByte(), 0x12.toByte(), 0x13.toByte(), 0xa4.toByte(), 0xa5.toByte(), 0xa5.toByte(), 0xa5.toByte(), 0xa6.toByte(),
////> .db $97, $98, $99, $01, $02, $03, $00, $a4
//    0x97.toByte(), 0x98.toByte(), 0x99.toByte(), 0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x00.toByte(), 0xa4.toByte(),
////> .db $a5, $a6, $00, $11, $12, $12, $12, $13
//    0xa5.toByte(), 0xa6.toByte(), 0x00.toByte(), 0x11.toByte(), 0x12.toByte(), 0x12.toByte(), 0x12.toByte(), 0x13.toByte(),
////> .db $00, $00, $00, $00, $01, $02, $02, $03
//    0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x02.toByte(), 0x02.toByte(), 0x03.toByte(),
////> .db $00, $a4, $a5, $a5, $a6, $00, $00, $00
//    0x00.toByte(), 0xa4.toByte(), 0xa5.toByte(), 0xa5.toByte(), 0xa6.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
//
////> .db $11, $12, $12, $13, $00, $00, $00, $00 ;trees and fences
//    0x11.toByte(), 0x12.toByte(), 0x12.toByte(), 0x13.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
////> .db $00, $00, $00, $9c, $00, $8b, $aa, $aa
//    0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x9c.toByte(), 0x00.toByte(), 0x8b.toByte(), 0xaa.toByte(), 0xaa.toByte(),
////> .db $aa, $aa, $11, $12, $13, $8b, $00, $9c
//    0xaa.toByte(), 0xaa.toByte(), 0x11.toByte(), 0x12.toByte(), 0x13.toByte(), 0x8b.toByte(), 0x00.toByte(), 0x9c.toByte(),
////> .db $9c, $00, $00, $01, $02, $03, $11, $12
//    0x9c.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x02.toByte(), 0x03.toByte(), 0x11.toByte(), 0x12.toByte(),
////> .db $12, $13, $00, $00, $00, $00, $aa, $aa
//    0x12.toByte(), 0x13.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0xaa.toByte(), 0xaa.toByte(),
////> .db $9c, $aa, $00, $8b, $00, $01, $02, $03
//    0x9c.toByte(), 0xaa.toByte(), 0x00.toByte(), 0x8b.toByte(), 0x00.toByte(), 0x01.toByte(), 0x02.toByte(), 0x03.toByte(),
//)
//
////> BackSceneryMetatiles:
//val BackSceneryMetatiles = byteArrayOf(
////> .db $80, $83, $00 ;cloud left
//    0x80.toByte(), 0x83.toByte(), 0x00.toByte(),
////> .db $81, $84, $00 ;cloud middle
//    0x81.toByte(), 0x84.toByte(), 0x00.toByte(),
////> .db $82, $85, $00 ;cloud right
//    0x82.toByte(), 0x85.toByte(), 0x00.toByte(),
////> .db $02, $00, $00 ;bush left
//    0x02.toByte(), 0x00.toByte(), 0x00.toByte(),
////> .db $03, $00, $00 ;bush middle
//    0x03.toByte(), 0x00.toByte(), 0x00.toByte(),
////> .db $04, $00, $00 ;bush right
//    0x04.toByte(), 0x00.toByte(), 0x00.toByte(),
////> .db $00, $05, $06 ;mountain left
//    0x00.toByte(), 0x05.toByte(), 0x06.toByte(),
////> .db $07, $06, $0a ;mountain middle
//    0x07.toByte(), 0x06.toByte(), 0x0a.toByte(),
////> .db $00, $08, $09 ;mountain right
//    0x00.toByte(), 0x08.toByte(), 0x09.toByte(),
////> .db $4d, $00, $00 ;fence
//    0x4d.toByte(), 0x00.toByte(), 0x00.toByte(),
////> .db $0d, $0f, $4e ;tall tree
//    0x0d.toByte(), 0x0f.toByte(), 0x4e.toByte(),
////> .db $0e, $4e, $4e ;short tree
//    0x0e.toByte(), 0x4e.toByte(), 0x4e.toByte(),
//)
//
////> FSceneDataOffsets:
////> .db $00, $0d, $1a
//val FSceneDataOffsets = byteArrayOf(0x00.toByte(), 0x0d.toByte(), 0x1a.toByte())
//
////> ForeSceneryData:
//val ForeSceneryData = byteArrayOf(
////> .db $86, $87, $87, $87, $87, $87, $87   ;in water
//    0x86.toByte(), 0x87.toByte(), 0x87.toByte(), 0x87.toByte(), 0x87.toByte(), 0x87.toByte(), 0x87.toByte(),
////> .db $87, $87, $87, $87, $69, $69
//    0x87.toByte(), 0x87.toByte(), 0x87.toByte(), 0x87.toByte(), 0x69.toByte(), 0x69.toByte(),
//
////> .db $00, $00, $00, $00, $00, $45, $47   ;wall
//    0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x45.toByte(), 0x47.toByte(),
////> .db $47, $47, $47, $47, $00, $00
//    0x47.toByte(), 0x47.toByte(), 0x47.toByte(), 0x47.toByte(), 0x00.toByte(), 0x00.toByte(),
//
////> .db $00, $00, $00, $00, $00, $00, $00   ;over water
//    0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
////> .db $00, $00, $00, $00, $86, $87
//    0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x86.toByte(), 0x87.toByte(),
//)
//
////> TerrainMetatiles:
////> .db $69, $54, $52, $62
//val TerrainMetatiles = byteArrayOf(0x69.toByte(), 0x54.toByte(), 0x52.toByte(), 0x62.toByte())
//
////> TerrainRenderBits:
//val TerrainRenderBits = byteArrayOf(
//    //> .db %00000000, %00000000 ;no ceiling or floor
//    0b00000000.toByte(), 0b00000000.toByte(),
//    //> .db %00000000, %00011000 ;no ceiling, floor 2
//    0b00000000.toByte(), 0b00011000.toByte(),
//    //> .db %00000001, %00011000 ;ceiling 1, floor 2
//    0b00000001.toByte(), 0b00011000.toByte(),
//    //> .db %00000111, %00011000 ;ceiling 3, floor 2
//    0b00000111.toByte(), 0b00011000.toByte(),
//    //> .db %00001111, %00011000 ;ceiling 4, floor 2
//    0b00001111.toByte(), 0b00011000.toByte(),
//    //> .db %11111111, %00011000 ;ceiling 8, floor 2
//    0b11111111.toByte(), 0b00011000.toByte(),
//    //> .db %00000001, %00011111 ;ceiling 1, floor 5
//    0b00000001.toByte(), 0b00011111.toByte(),
//    //> .db %00000111, %00011111 ;ceiling 3, floor 5
//    0b00000111.toByte(), 0b00011111.toByte(),
//    //> .db %00001111, %00011111 ;ceiling 4, floor 5
//    0b00001111.toByte(), 0b00011111.toByte(),
//    //> .db %10000001, %00011111 ;ceiling 1, floor 6
//    0b10000001.toByte(), 0b00011111.toByte(),
//    //> .db %00000001, %00000000 ;ceiling 1, no floor
//    0b00000001.toByte(), 0b00000000.toByte(),
//    //> .db %10001111, %00011111 ;ceiling 4, floor 6
//    0b10001111.toByte(), 0b00011111.toByte(),
//    //> .db %11110001, %00011111 ;ceiling 1, floor 9
//    0b11110001.toByte(), 0b00011111.toByte(),
//    //> .db %11111001, %00011000 ;ceiling 1, middle 5, floor 2
//    0b11111001.toByte(), 0b00011000.toByte(),
//    //> .db %11110001, %00011000 ;ceiling 1, middle 4, floor 2
//    0b11110001.toByte(), 0b00011000.toByte(),
//    //> .db %11111111, %00011111 ;completely solid top to bottom
//    0b11111111.toByte(), 0b00011111.toByte(),
//)
//
//// TODO: Everything below here is considered untrusted.
//
//private fun System.areaParserCore() {
//    //> AreaParserCore:
//    //> lda BackloadingFlag       ;check to see if we are starting right of start
//    //> beq RenderSceneryTerrain  ;if not, go ahead and render background, foreground and terrain
//    if (ram.backloadingFlag != 0.toByte()) {
//        //> jsr ProcessAreaData       ;otherwise skip ahead and load level data
//        processAreaData()
//        // fall through to render anyway per structure (next part renders after ProcessAreaData too)
//    }
//
//    //> RenderSceneryTerrain:
//    //> ldx #$0c
//    //> lda #$00
//    //> ClrMTBuf: sta MetatileBuffer,x       ;clear out metatile buffer
//    //> dex
//    //> bpl ClrMTBuf
//    ram.metatileBuffer.fill(0x00)
//
//    //> ldy BackgroundScenery      ;do we need to render the background scenery?
//    //> beq RendFore               ;if not, skip to check the foreground
//    if (ram.backgroundScenery != 0.toByte()) {
//        //> lda CurrentPageLoc         ;otherwise check for every third page
//        var a = ram.currentPageLoc
//        //> ThirdP:   cmp #$03
//        //> bmi RendBack               ;if less than three we're there
//        //> sec
//        //> sbc #$03                   ;if 3 or more, subtract 3 and
//        //> bpl ThirdP                 ;do an unconditional branch
//        a = (a % 3).toByte()
//
//        //> RendBack: asl                        ;move results to higher nybble
//        //> asl
//        //> asl
//        //> asl
//        a = ((a.toInt() shl 4) and 0xFF).toByte()
//        // Carry guaranteed clear
//        //> adc BSceneDataOffsets-1,y  ;add to it offset loaded from here
//        a = a bytePlus BSceneDataOffsets[ram.backgroundScenery-1]
//        //> adc CurrentColumnPos       ;add to the result our current column position
//        a = a bytePlus ram.currentColumnPos
//        //> tax
//        var x = a
//        //> lda BackSceneryData,x      ;load data from sum of offsets
//        a = BackSceneryData[x.toInt()]
//        //> beq RendFore               ;if zero, no scenery for that part
//        if (a != 0.toByte()) {
//            //> pha
//            val stacked = a
//            //> and #$0f                   ;save to stack and clear high nybble
//            a = a and 0x0f
//            //> sec
//            //> sbc #$01                   ;subtract one (because low nybble is $01-$0c)
//            a--
//            //> sta $00                    ;save low nybble
//            //> asl                        ;multiply by three (shift to left and add result to old one)
//            //> adc $00                    ;note that since d7 was nulled, the carry flag is always clear
//            a = (a * 3).toByte()
//            //> tax                        ;save as offset for background scenery metatile data
//            x = a
//            // TODO: Continue reviewing
//            //> pla                        ;get high nybble from stack, move low
//            a = stacked
//            //> lsr
//            //> lsr
//            //> lsr
//            //> lsr
//            a = (a ushr 4) and 0x0F
//            //> tay                        ;use as second offset (used to determine height)
//            var y = a
//            //> lda #$03                   ;use previously saved memory location for counter
//            //> sta $00
//            zp00 = 0x03
//            //> SceLoop1: lda BackSceneryMetatiles,x ;load metatile data from offset of (lsb - 1) * 3
//            //> sta MetatileBuffer,y       ;store into buffer from offset of (msb / 16)
//            //> inx
//            //> iny
//            //> cpy #$0b                   ;if at this location, leave loop
//            //> beq RendFore
//            //> dec $00                    ;decrement until counter expires, barring exception
//            //> bne SceLoop1
//            while (true) {
//                val valMt = BackSceneryMetatiles.getOrNull(x)?.toInt()?.and(0xFF) ?: 0
//                if (y in 0 until ram.metatileBuffer.size) ram.metatileBuffer[y] = valMt.toByte()
//                x = (x + 1) and 0xFF
//                y = (y + 1) and 0xFF
//                if (y == 0x0b) break
//                zp00 = (zp00 - 1) and 0xFF
//                if (zp00 != 0) continue else break
//            }
//        }
//    }
//
//    //> RendFore: ldx ForegroundScenery      ;check for foreground data needed or not
//    //> beq RendTerr               ;if not, skip this part
//    if (ram.foregroundScenery != 0.toByte()) {
//        //> ldy FSceneDataOffsets-1,x  ;load offset from location offset by header value, then
//        var y = FSceneDataOffsets[(ram.foregroundScenery.toInt() and 0xFF) - 1].toInt() and 0xFF
//        //> ldx #$00                   ;reinit X
//        var x = 0
//        //> SceLoop2: lda ForeSceneryData,y      ;load data until counter expires
//        //> beq NoFore                 ;do not store if zero found
//        //> sta MetatileBuffer,x
//        //> NoFore:   iny
//        //> inx
//        //> cpx #$0d                   ;store up to end of metatile buffer
//        //> bne SceLoop2
//        while (x != 0x0d) {
//            val v = ForeSceneryData.getOrNull(y)?.toInt()?.and(0xFF) ?: 0
//            if (v != 0) {
//                if (x in ram.metatileBuffer.indices) ram.metatileBuffer[x] = v.toByte()
//            }
//            y = (y + 1) and 0xFF
//            x = (x + 1) and 0xFF
//        }
//    }
//
//    //> RendTerr: ldy AreaType               ;check world type for water level
//    //> bne TerMTile               ;if not water level, skip this part
//    //> lda WorldNumber            ;check world number, if not world number eight
//    //> cmp #World8                ;then skip this part
//    //> bne TerMTile
//    //> lda #$62                   ;if set as water level and world number eight,
//    //> jmp StoreMT                ;use castle wall metatile as terrain type
//    var aTerrain: Int
//    if (ram.areaType == 0.toByte() && ram.worldNumber == World8) {
//        aTerrain = 0x62
//    } else {
//        //> TerMTile: lda TerrainMetatiles,y     ;otherwise get appropriate metatile for area type
//        aTerrain = TerrainMetatiles[ram.areaType.toInt() and 0xFF].toInt() and 0xFF
//        //> ldy CloudTypeOverride      ;check for cloud type override
//        //> beq StoreMT                ;if not set, keep value otherwise
//        //> lda #$88                   ;use cloud block terrain
//        if ((ram.cloudTypeOverride.toInt() and 0xFF) != 0) aTerrain = 0x88
//    }
//    //> StoreMT:  sta $07                    ;store value here
//    var zp07 = aTerrain and 0xFF
//    //> ldx #$00                   ;initialize X, use as metatile buffer offset
//    var x = 0
//    //> lda TerrainControl         ;use yet another value from the header
//    //> asl                        ;multiply by 2 and use as yet another offset
//    //> tay
//    var y = ((ram.terrainControl.toInt() and 0xFF) shl 1) and 0xFF
//    // We will emulate two passes: ceiling byte then floor byte
//    repeat(2) { passIndex ->
//        //> TerrLoop: lda TerrainRenderBits,y    ;get one of the terrain rendering bit data
//        var zp00 = TerrainRenderBits[y].toInt() and 0xFF
//        //> sta $00
//        // (already in zp00)
//        //> iny                        ;increment Y and use as offset next time around
//        //> sty $01
//        y = (y + 1) and 0xFF
//        var zp01 = y
//        //> lda CloudTypeOverride      ;skip if value here is zero
//        //> beq NoCloud2
//        //> cpx #$00                   ;otherwise, check if we're doing the ceiling byte
//        //> beq NoCloud2
//        //> lda $00                    ;if not, mask out all but d3
//        //> and #%00001000
//        //> sta $00
//        if ((ram.cloudTypeOverride.toInt() and 0xFF) != 0 && passIndex != 0) {
//            zp00 = zp00 and 0b00001000
//        }
//        //> NoCloud2: ldy #$00                   ;start at beginning of bitmasks
//        var bitY = 0
//        //> TerrBChk: lda Bitmasks,y             ;load bitmask, then perform AND on contents of first byte
//        //> bit $00
//        //> beq NextTBit               ;if not set, skip this part (do not write terrain to buffer)
//        //> lda $07
//        //> sta MetatileBuffer,x       ;load terrain type metatile number and store into buffer here
//        //> NextTBit: inx                        ;continue until end of buffer
//        //> cpx #$0d
//        //> beq RendBBuf               ;if we're at the end, break out of this loop
//        //> lda AreaType               ;check world type for underground area
//        //> cmp #$02
//        //> bne EndUChk                ;if not underground, skip this part
//        //> cpx #$0b
//        //> bne EndUChk                ;if we're at the bottom of the screen, override
//        //> lda #$54                   ;old terrain type with ground level terrain type
//        //> sta $07
//        //> EndUChk:  iny                        ;increment bitmasks offset in Y
//        //> cpy #$08
//        //> bne TerrBChk               ;if not all bits checked, loop back
//        while (true) {
//            val mask = Bitmasks[bitY].toInt() and 0xFF
//            if ((zp00 and mask) != 0) {
//                if (x in ram.metatileBuffer.indices) ram.metatileBuffer[x] = (zp07 and 0xFF).toByte()
//            }
//            x = (x + 1) and 0xFF
//            if (x == 0x0d) break
//            if (ram.areaType == 0x02.toByte() && x == 0x0b) {
//                zp07 = 0x54
//            }
//            bitY = (bitY + 1) and 0xFF
//            if (bitY != 0x08) continue else break
//        }
//        //> ldy $01
//        //> bne TerrLoop               ;unconditional branch, use Y to load next byte
//        y = zp01
//        // loop repeats due to repeat(2)
//    }
//
//    //> RendBBuf: jsr ProcessAreaData        ;do the area data loading routine now
//    processAreaData()
//    //> lda BlockBufferColumnPos
//    //> jsr GetBlockBufferAddr     ;get block buffer address from where we're at
//    val column = ram.blockBufferColumnPos.toInt() and 0x1F
//    if (_blockBufferBacking == null) _blockBufferBacking = ByteArray(0x400)
//    val baseAddr = column // simplified base for our backing buffer
//    //> ldx #$00
//    //> ldy #$00                   ;init index regs and start at beginning of smaller buffer
//    x = 0
//    var yStore = 0
//    //> ChkMTLow: sty $00
//    // loop over 13 entries and write low bytes to block buffer based on thresholds
//    while (true) {
//        var zp00 = yStore and 0xFF
//        //> lda MetatileBuffer,x       ;load stored metatile number
//        var a = ram.metatileBuffer.getOrNull(x)?.toInt()?.and(0xFF) ?: 0
//        //> and #%11000000             ;mask out all but 2 MSB
//        var top2 = a and 0xC0
//        //> asl
//        //> rol                        ;make %xx000000 into %000000xx
//        //> rol
//        var attrIndex = ((top2 shl 2) and 0xFF) ushr 6 // 0..3
//        //> tay                        ;use as offset in Y
//        val yAttr = attrIndex
//        //> lda MetatileBuffer,x       ;reload original unmasked value here
//        a = ram.metatileBuffer.getOrNull(x)?.toInt()?.and(0xFF) ?: 0
//        //> cmp BlockBuffLowBounds,y   ;check for certain values depending on bits set
//        //> bcs StrBlock               ;if equal or greater, branch
//        if (a < (BlockBuffLowBounds[yAttr].toInt() and 0xFF)) {
//            //> lda #$00                   ;if less, init value before storing
//            a = 0
//        }
//        //> StrBlock: ldy $00                    ;get offset for block buffer
//        //> sta ($06),y                ;store value into block buffer
//        val index = (baseAddr + zp00) and 0x3FF
//        _blockBufferBacking!![index] = a.toByte()
//        //> tya
//        //> clc                        ;add 16 (move down one row) to offset
//        //> adc #$10
//        //> tay
//        yStore = (zp00 + 0x10) and 0xFF
//        //> inx                        ;increment column value
//        //> cpx #$0d
//        //> bcc ChkMTLow               ;continue until we pass last row, then leave
//        x = (x + 1) and 0xFF
//        if (x < 0x0d) continue
//        break
//    }
//    //> rts
//}
//
//private fun System.processAreaData() {
//    // Placeholder translation hook for ProcessAreaData
//}
//
//
//// Bitmasks used for terrain rendering (1<<0 .. 1<<7)
//private val Bitmasks = byteArrayOf(
//    0x01, 0x02, 0x04, 0x08,
//    0x10, 0x20, 0x40, 0x80.toByte()
//)
//
//// Thresholds for block buffer low byte based on attribute bits (top 2 bits of metatile)
//private val BlockBuffLowBounds = byteArrayOf(
//    0x10, 0x51, 0x88.toByte(), 0xC0.toByte()
//)
//
//// Minimal backing for block buffer to satisfy writes in AreaParserCore
//private var _blockBufferBacking: ByteArray? = null
