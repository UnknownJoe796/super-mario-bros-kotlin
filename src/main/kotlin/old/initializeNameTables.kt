package com.ivieleague.smbtranslation.old

fun initializeNameTables() {
    //> InitializeNameTables:
    //> lda PPU_STATUS            ;reset flip-flop
    // This does not apply here.

    //> lda Mirror_PPU_CTRL_REG1  ;load mirror of ppu reg $2000
    //> ora #%00010000            ;set sprites for first 4k and background for second 4k
    //> and #%11110000            ;clear rest of lower nybble, leave higher alone
    //> jsr WritePPUReg1
    @InexactBitSetting
    PictureProcessingUnit.Control.backgroundTableOffset = true

    //> lda #$24                  ;set vram address to start of name table 1
    //> jsr WriteNTAddr
    //> lda #$20                  ;and then set it to name table 0
    // We'll call these two after the declaration.

    fun writeNtAddr(highByte: Byte) {
        //> WriteNTAddr:  sta PPU_ADDRESS
        //> lda #$00
        //> sta PPU_ADDRESS
        PictureProcessingUnit.vramAddress = (highByte.toInt() shl 8).toShort()

        //> ldx #$04                  ;clear name table with blank tile #24
        //> ldy #$c0
        //> lda #$24
        //> InitNTLoop:   sta PPU_DATA              ;count out exactly 768 tiles (0x300 tiles)
        //> dey
        //> bne InitNTLoop
        //> dex
        //> bne InitNTLoop
        repeat(0x300) {
            PictureProcessingUnit.writeVram(0x00)
        }

        //> ldy #64                   ;now to clear the attribute table (with zero this time)
        // This is set up for a loop later
        //> txa

        //> sta VRAM_Buffer1_Offset   ;init vram buffer 1 offset
        system.vramBuffer1.offset = 0x0
        //> sta VRAM_Buffer1          ;init vram buffer 1
        system.vramBuffer1.bytes[0] = 0x0

        repeat(64) {
            //> InitATLoop:   sta PPU_DATA
            PictureProcessingUnit.writeVram(0x00)
            //> dey
            //> bne InitATLoop
        }

        //> sta HorizontalScroll      ;reset scroll variables
        system.game.area.horizontalScroll = 0x00.toByte()
        //> sta VerticalScroll
        system.game.area.verticalScroll = 0x00.toByte()

        //> jmp InitScroll            ;initialize scroll registers to zero
        // The original code uses a subroutine to run this twice.
        return initScroll(0x0)
    }
    // These come from above the subroutine declaration
    writeNtAddr(0x24)
    writeNtAddr(0x20)
}

fun initScroll(a: Byte) {
    //> InitScroll:    sta PPU_SCROLL_REG        ;store contents of A into scroll registers
    //> sta PPU_SCROLL_REG        ;and end whatever subroutine led us here
    PictureProcessingUnit.scroll(a, a)
    //> rts
}

