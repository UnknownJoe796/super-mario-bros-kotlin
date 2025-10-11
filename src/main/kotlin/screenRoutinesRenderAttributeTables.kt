package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor


fun System.renderAttributeTables() {
    //> RenderAttributeTables:
    //> lda CurrentNTAddr_Low    ;get low byte of next name table address
    //> and #%00011111           ;to be written to, mask out all but 5 LSB,
    val low5 = ram.currentNTAddrLow.toInt() and 0b11111
    //> sec                      ;subtract four
    //> sbc #$04
    // Compute (low5 - 4) mod 32 and remember if we borrowed
    val borrowed = low5 < 4
    //> and #%00011111           ;mask out bits again and store
    //> sta $01
    val lowMinus4 = (low5 - 4) and 0b11111

    //> lda CurrentNTAddr_High   ;get high byte and branch if borrow not set
    var addrHigh = ram.currentNTAddrHigh
    //> bcs SetATHigh
    if (!borrowed)
    //> eor #%00000100           ;otherwise invert d2
    addrHigh = addrHigh xor 0b100.toByte()
    // Recreate high-byte selection for attribute table page: $23xx or $27xx
    //> SetATHigh:   and #%00000100           ;mask out all other bits
    addrHigh = addrHigh and 0b100
    //> ora #$23                 ;add $2300 to the high byte and store
    addrHigh = addrHigh or 0x23  // $23 or $27
    //> sta $00

    // Determine nametable index from addrHigh ($23 -> 0, $27 -> 1)
    val nametableIndex = if ((addrHigh and 0x04) == 0.toByte()) 0 else 1

    //> lda $01                  ;get low byte - 4, divide by 4, add offset for
    //> lsr                      ;attribute table and store
    //> lsr
    val ax = (lowMinus4 ushr 2) and 0x07 // attribute X within 0..7
    //> adc #$c0                 ;we should now have the appropriate block of
    // In original buffer, low starts at $C0+ax, then each write adds +8
    var ay = 0
    //> sta $01                  ;attribute table in our temp address

    //> ldx #$00
    //> ldy VRAM_Buffer2_Offset  ;get buffer offset
    // (We append to high-level buffer; offset bookkeeping is implicit.)

    for (x in 0..6) {
        //> AttribLoop:  lda $00
        //> sta VRAM_Buffer2,y       ;store high byte of attribute table address
        // high byte is encoded by nametableIndex in our high-level op

        //> lda $01
        //> clc                      ;get low byte, add 8 because we want to start
        //> adc #$08                 ;below the status bar, and store
        // The +8 moves down one attribute row each iteration; our ay already tracks that.
        //> sta VRAM_Buffer2+1,y
        //> sta $01                  ;also store in temp again
        ay++

        //> lda AttributeBuffer,x    ;fetch current attribute table byte and store
        val value = ram.attributeBuffer[x]
        // Emit one attribute byte at (ax, ay) in the selected nametable
        //> sta VRAM_Buffer2+3,y     ;in the buffer
        // (handled by the high-level update above)
        //> lda #$01
        //> sta VRAM_Buffer2+2,y     ;store length of 1 in buffer
        // (implicit in repetitions = 1)
        //> lsr
        //> sta AttributeBuffer,x    ;clear current byte in attribute buffer
        ram.vRAMBuffer2.add(
            BufferedPpuUpdate.BackgroundAttributeRepeat(
                nametable = nametableIndex.toByte(),
                ax = ax.toByte(),
                ay = ay.toByte(),
                drawVertically = false,
                value = value,
                repetitions = 1,
            )
        )
        ram.attributeBuffer[x] = 0u
        //> iny                      ;increment buffer offset by 4 bytes
        //> iny
        //> iny
        //> iny
        //> inx                      ;increment attribute offset and check to see
        //> cpx #$07                 ;if we're at the end yet
        //> bcc AttribLoop
    }

    //> sta VRAM_Buffer2,y       ;put null terminator at the end
    // (Not needed for high-level buffer representation)
    //> sty VRAM_Buffer2_Offset  ;store offset in case we want to do any more
    // (Not needed)

    //> SetVRAMCtrl: lda #$06
    //> sta VRAM_Buffer_AddrCtrl ;set buffer to $0341 and leave
    ram.vRAMBufferAddrCtrl = 0x06
    //> rts
}