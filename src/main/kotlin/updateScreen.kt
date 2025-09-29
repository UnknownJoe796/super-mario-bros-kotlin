package com.ivieleague.smbtranslation


fun System.updateScreen(bufferToWrite: VBuffer) {
    bufferToWrite.forEach {
        println("Applying $it")
        it(ppu)
    }
    //> WriteBufferToScreen:
    //> sta PPU_ADDRESS           ;store high byte of vram address
    //> iny
    //> lda ($00),y               ;load next byte (second)
    //> sta PPU_ADDRESS           ;store low byte of vram address
    //> iny
    //> lda ($00),y               ;load next byte (third)
    //> asl                       ;shift to left and save in stack
    //> pha
    //> lda Mirror_PPU_CTRL_REG1  ;load mirror of $2000,
    //> ora #%00000100            ;set ppu to increment by 32 by default
    //> bcs SetupWrites           ;if d7 of third byte was clear, ppu will
    //> and #%11111011            ;only increment by 1
    //> SetupWrites:   jsr WritePPUReg1          ;write to register
    //> pla                       ;pull from stack and shift to left again
    //> asl
    //> bcc GetLength             ;if d6 of third byte was clear, do not repeat byte
    //> ora #%00000010            ;otherwise set d1 and increment Y
    //> iny
    //> GetLength:     lsr                       ;shift back to the right to get proper length
    //> lsr                       ;note that d1 will now be in carry
    //> tax
    //> OutputToVRAM:  bcs RepeatByte            ;if carry set, repeat loading the same byte
    //> iny                       ;otherwise increment Y to load next byte
    //> RepeatByte:    lda ($00),y               ;load more data from buffer and write to vram
    //> sta PPU_DATA
    //> dex                       ;done writing?
    //> bne OutputToVRAM
    //> sec
    //> tya
    //> adc $00                   ;add end length plus one to the indirect at $00
    //> sta $00                   ;to allow this routine to read another set of updates
    //> lda #$00
    //> adc $01
    //> sta $01
    //> lda #$3f                  ;sets vram address to $3f00
    //> sta PPU_ADDRESS
    //> lda #$00
    //> sta PPU_ADDRESS
    //> sta PPU_ADDRESS           ;then reinitializes it for some reason
    //> sta PPU_ADDRESS
    //> UpdateScreen:  ldx PPU_STATUS            ;reset flip-flop
    //> ldy #$00                  ;load first byte from indirect as a pointer
    //> lda ($00),y
    //> bne WriteBufferToScreen   ;if byte is zero we have no further updates to make here
    //> InitScroll:    sta PPU_SCROLL_REG        ;store contents of A into scroll registers
    //> sta PPU_SCROLL_REG        ;and end whatever subroutine led us here
    //> rts
}