package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
import com.ivieleague.smbtranslation.utils.InexactBitSetting


fun System.initializeNameTables() {
    //> InitializeNameTables:
    //> lda PPU_STATUS            ;reset flip-flop
    // This does not apply here.

    //> lda Mirror_PPU_CTRL_REG1  ;load mirror of ppu reg $2000
    //> ora #%00010000            ;set sprites for first 4k and background for second 4k
    //> and #%11110000            ;clear rest of lower nybble, leave higher alone
    //> jsr WritePPUReg1
    ppu.control = ppu.control.copy(
        backgroundTableOffset = true,
        spritePatternTableOffset = false,
        drawVertical = false,
        baseNametableAddress = 0,
    )

    //> lda #$24                  ;set vram address to start of name table 1
    //> jsr WriteNTAddr
    //> lda #$20                  ;and then set it to name table 0
    // We'll call these two after the declaration.

    fun writeNtAddr(highByte: Byte) {
        //> WriteNTAddr:  sta PPU_ADDRESS
        //> lda #$00
        //> sta PPU_ADDRESS
        ppu.internalVramAddress = (highByte.toInt() shl 8).toShort()

        //> ldx #$04                  ;clear name table with blank tile #24
        //> ldy #$c0
        //> lda #$24
        //> InitNTLoop:   sta PPU_DATA              ;count out exactly 768 tiles (0x300 tiles)
        //> dey
        //> bne InitNTLoop
        //> dex
        //> bne InitNTLoop
        repeat(0x300) {
            ppu.writeVram(0x24.toByte())
        }

        //> ldy #64                   ;now to clear the attribute table (with zero this time)
        // This is set up for a loop later
        //> txa

        //> sta VRAM_Buffer1_Offset   ;init vram buffer 1 offset
        //> sta VRAM_Buffer1          ;init vram buffer 1
        ram.vRAMBuffer1.clear()

        repeat(64) {
            //> InitATLoop:   sta PPU_DATA
            ppu.writeVram(0x00.toByte())
            //> dey
            //> bne InitATLoop
        }

        //> sta HorizontalScroll      ;reset scroll variables
        ram.horizontalScroll = 0x00.toByte()
        //> sta VerticalScroll
        ram.verticalScroll = 0x00.toByte()

        //> jmp InitScroll            ;initialize scroll registers to zero
        // The original code uses a subroutine to run this twice.
        return initScroll(0x0)
    }
    // These come from above the subroutine declaration
    writeNtAddr(0x24.toByte())
    writeNtAddr(0x20.toByte())
}

fun System.initScroll(a: Byte) {
    //> InitScroll:    sta PPU_SCROLL_REG        ;store contents of A into scroll registers
    //> sta PPU_SCROLL_REG        ;and end whatever subroutine led us here
    ppu.scroll(a, a)
    //> rts
}

