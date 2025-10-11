package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
import com.ivieleague.smbtranslation.nes.Color
import kotlin.experimental.and

//> ;$00 - used as temporary counter in ColorRotation

//> ColorRotatePalette:
//> .db $27, $27, $27, $17, $07, $17
private val colorRotatePalette = listOf(
    Color(0x27), Color(0x27), Color(0x27), Color(0x17), Color(0x07), Color(0x17)
)

//> BlankPalette:
//> .db $3f, $0c, $04, $ff, $ff, $ff, $ff, $00
private val blankPalette = BufferedPpuUpdate.BackgroundSetPalette(
    index = 3,
    colors = listOf(
        Color(0xff000000.toInt()),
        Color(0xff000000.toInt()),
        Color(0xff000000.toInt()),
        Color(0xff000000.toInt())
    )
)

//> ;used based on area type
//> Palette3Data:
private val palette3Data = listOf(
    listOf(Color(0x0f), Color(0x07), Color(0x12), Color(0x0f)),
    listOf(Color(0x0f), Color(0x07), Color(0x17), Color(0x0f)),
    listOf(Color(0x0f), Color(0x07), Color(0x17), Color(0x1c)),
    listOf(Color(0x0f), Color(0x07), Color(0x17), Color(0x00)),
)
//> .db $0f, $07, $12, $0f
//> .db $0f, $07, $17, $0f
//> .db $0f, $07, $17, $1c
//> .db $0f, $07, $17, $00

fun System.colorRotation() {
    //> ColorRotation:
    //> lda FrameCounter         ;get frame counter
    //> and #$07                 ;mask out all but three LSB
    //> bne ExitColorRot         ;branch if not set to zero to do this every eighth frame
    if (ram.frameCounter and 0b111.toByte() != 0.toByte()) return

    //> ldx VRAM_Buffer1_Offset  ;check vram buffer offset
    //> cpx #$31
    //> bcs ExitColorRot         ;if offset over 48 bytes, branch to leave
    // Fun thing: we actually CAN'T really do this well.
    // We're high level in that PPU emulation.
    // We can come back and make this possible if we have to.

    //> tay                      ;otherwise use frame counter's 3 LSB as offset here
    //> GetBlankPal:  lda BlankPalette,y       ;get blank palette for palette 3
    //> sta VRAM_Buffer1,x       ;store it in the vram buffer
    //> inx                      ;increment offsets
    //> iny
    //> cpy #$08
    //> bcc GetBlankPal          ;do this until all bytes are copied
    // The original copies in the blank and modifies it; we're going to add it at the end.
    var paletteToAdd = blankPalette

    //> ldx VRAM_Buffer1_Offset  ;get current vram buffer offset
    //> lda #$03
    //> sta $00                  ;set counter here
    // No need to set up a counter, since we don't have to have a loop.
    //> lda AreaType             ;get area type
    //> asl                      ;multiply by 4 to get proper offset
    //> asl
    //> tay                      ;save as offset here

    //> GetAreaPal:   lda Palette3Data,y       ;fetch palette to be written based on area type
    //> sta VRAM_Buffer1+3,x     ;store it to overwrite blank palette in vram buffer
    //> iny
    //> inx
    //> dec $00                  ;decrement counter
    //> bpl GetAreaPal           ;do this until the palette is all copied
    paletteToAdd = paletteToAdd.copy(
        colors = palette3Data[ram.areaType]
    )

    //> ldx VRAM_Buffer1_Offset  ;get current vram buffer offset
    //> ldy ColorRotateOffset    ;get color cycling offset
    //> lda ColorRotatePalette,y
    //> sta VRAM_Buffer1+4,x     ;get and store current color in second slot of palette
    paletteToAdd = paletteToAdd.copy(
        colors = paletteToAdd.colors.toMutableList().apply {
            this[1] = colorRotatePalette[ram.colorRotateOffset]
        }
    )

    //> lda VRAM_Buffer1_Offset
    //> clc                      ;add seven bytes to vram buffer offset
    //> adc #$07
    //> sta VRAM_Buffer1_Offset
    // Not relevant here, because of high-level PPU buffer emulation.

    //> inc ColorRotateOffset    ;increment color cycling offset
    //> lda ColorRotateOffset
    //> cmp #$06                 ;check to see if it's still in range
    //> bcc ExitColorRot         ;if so, branch to leave
    if(++ram.colorRotateOffset >= 0x06) {
        //> lda #$00
        //> sta ColorRotateOffset    ;otherwise, init to keep it in range
        ram.colorRotateOffset = 0x00.toByte()
    }
    //> ExitColorRot: rts                      ;leave
    ram.vRAMBuffer1.add(paletteToAdd)
}
