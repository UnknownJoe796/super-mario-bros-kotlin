package com.ivieleague.smbtranslation.nes

import com.ivieleague.smbtranslation.GameRam
import com.ivieleague.smbtranslation.PpuMap
import com.ivieleague.smbtranslation.chr.rawChrData
import com.ivieleague.smbtranslation.utils.PpuControl
import com.ivieleague.smbtranslation.utils.PpuMask
import com.ivieleague.smbtranslation.utils.PpuStatus
import java.io.File

class PictureProcessingUnit {
    val backgroundTiles = Array(2) { NesNametable() }
    val backgroundPalettes = Array(4) { IndirectPalette(Palette.EMPTY, "background $it") }
    val sprites = Array(64) { Sprite() }
    val spritePalettes = Array(4) { IndirectPalette(Palette.EMPTY, "foreground $it") }

    /**
     * PPU_CTRL_REG1: $2000
     */
    var control: PpuControl = PpuControl(0)

    /**
     * PPU_CTRL_REG2: $2001
     */
    var mask: PpuMask = PpuMask(0)

    /**
     * PPU_STATUS / PPUSTATUS: $2002
     */
    val status: PpuStatus = PpuStatus(0)

    //    PPU_SPR_ADDR / OAMADDR 	$2003 	AAAA AAAA 	W 	OAM read/write address
    var oamAddress: Byte = 0x00
    fun writeOamAddress(address: Byte) {
        // In hardware this writes to OAMADDR ($2003); here we mirror into our property
        oamAddress = address
        // TODO: Implement side effects if/when OAM is emulated
    }

    //    PPU_SPR_DATA / OAMDATA 	$2004 	DDDD DDDD 	RW 	OAM data read/write
    fun readOamData(): Byte  {
        /*TODO*/
        return 0x0
    }
    fun writeOamData(data: Byte) {
        oamAddress++
        // TODO
    }

    //    PPU_SCROLL_REG / PPUSCROLL 	$2005 	XXXX XXXX YYYY YYYY 	Wx2 	X and Y scroll bits 7-0 (two writes: X scroll, then Y scroll)
    fun scroll(x: Byte, y: Byte) {
        // TODO
    }

    //    PPU_ADDRESS / PPUADDR 	$2006 	..AA AAAA AAAA AAAA 	Wx2 	VRAM address (two writes: most significant byte, then least significant byte)
    var internalVramAddress: VramAddress = 0x0000
    fun setVramAddress(value: VramAddress) {
        internalVramAddress = value
    }

    //    PPU_DATA / PPUDATA 	$2007 	DDDD DDDD 	RW 	VRAM data read/write
    fun readVram(): Byte {
        internalVramAddress = internalVramAddress.plus(if (control.drawVertical) 32 else 1).toShort()
        // TODO
        return 0x0
    }

    fun writeVram(value: Byte) {
        internalVramAddress = internalVramAddress.plus(if (control.drawVertical) 32 else 1).toShort()
        // TODO
    }

    //    SPR_DMA / OAMDMA 	$4014 	AAAA AAAA 	W 	OAM DMA high address
    fun updateSpriteData(values: Array<GameRam.Sprite>) {
        // TODO
    }
}