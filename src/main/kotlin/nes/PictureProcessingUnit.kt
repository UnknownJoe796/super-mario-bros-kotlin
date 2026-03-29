package com.ivieleague.smbtranslation.nes

import com.ivieleague.smbtranslation.GameRam
import com.ivieleague.smbtranslation.PpuMap
import com.ivieleague.smbtranslation.chr.OriginalRom
import com.ivieleague.smbtranslation.utils.PpuControl
import com.ivieleague.smbtranslation.utils.PpuMask
import com.ivieleague.smbtranslation.utils.PpuStatus
import java.io.File

class PictureProcessingUnit {
    /** CHR tile source — set to Smb2jRom for SMB2J, OriginalRom for SMB1. */
    var chrSource: Any = OriginalRom
    val chrBackgrounds get() = when (chrSource) {
        is com.ivieleague.smbtranslation.chr.Smb2jRom -> com.ivieleague.smbtranslation.chr.Smb2jRom.backgrounds
        else -> OriginalRom.backgrounds
    }
    val chrSprites get() = when (chrSource) {
        is com.ivieleague.smbtranslation.chr.Smb2jRom -> com.ivieleague.smbtranslation.chr.Smb2jRom.sprites
        else -> OriginalRom.sprites
    }

    val backgroundTiles = Array(2) { NesNametable() }
    val backgroundPalettes = Array(4) { IndirectPalette(Palette.RGB, "background $it") }
    val sprites = Array(64) { Sprite() }
    val spritePalettes = Array(4) { IndirectPalette(Palette.RGB, "foreground $it") }

    /**
     * NES universal background color ($3F00). On the NES, all palette color-0 entries
     * are mirrors of $3F00. Any write to a color-0 position updates this.
     */
    var universalBackgroundColor: Color = Color(0xFF000000.toInt())

    /** On the NES, all palette color-0 entries are mirrors of $3F00. The renderer
     *  uses universalBackgroundColor for CI=0 pixels, so we just store the value. */
    fun syncUniversalBackgroundColor(color: Color) {
        universalBackgroundColor = color
    }

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
    var scrollX: Int = 0
    var scrollY: Int = 0

    fun scroll(x: Byte, y: Byte) {
        scrollX = x.toInt() and 0xFF
        scrollY = y.toInt() and 0xFF
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
        val addr = internalVramAddress.toInt() and 0x3FFF
        // Auto-increment AFTER using the address (matches NES hardware behavior)
        internalVramAddress = (addr + if (control.drawVertical) 32 else 1).toShort()

        when {
            // Nametable + attribute table: $2000-$2FFF
            addr in 0x2000..0x2FFF -> {
                val ntIdx = ((addr - 0x2000) / 0x400) and 0x01 // horizontal mirroring
                val offset = (addr - 0x2000) % 0x400
                val nt = backgroundTiles[ntIdx]
                if (offset < 0x3C0) {
                    // Tile data: offset = y*32 + x
                    val tx = offset % 32
                    val ty = offset / 32
                    if (ty < 30) {
                        val tileIdx = value.toInt() and 0xFF
                        val existing = nt[tx, ty]
                        nt[tx, ty] = existing.copy(pattern = chrBackgrounds[tileIdx])
                    }
                } else {
                    // Attribute table writes ($23C0-$23FF etc)
                    val attrOffset = offset - 0x3C0 // 0..63
                    val ax = attrOffset % 8
                    val ay = attrOffset / 8
                    applyAttributeCellToPpu(this, ntIdx.toByte(), ax.toByte(), ay.toByte(), value.toUByte())
                }
            }
            // Palette RAM: $3F00-$3F1F
            addr in 0x3F00..0x3F1F -> {
                val palOffset = addr - 0x3F00
                val palIdx = (palOffset / 4)
                val colorIdx = palOffset % 4
                val color = Color(value)
                if (palOffset < 0x10) {
                    backgroundPalettes[palIdx].colors[colorIdx] = color
                } else {
                    spritePalettes[palIdx - 4].colors[colorIdx] = color
                }
                // NES: only $3F00 and $3F10 (its mirror) update universal background
                if (palOffset == 0 || palOffset == 0x10) syncUniversalBackgroundColor(color)
            }
        }
    }

    //    SPR_DMA / OAMDMA 	$4014 	AAAA AAAA 	W 	OAM DMA high address
    fun updateSpriteData(values: Array<GameRam.Sprite>) {
        for (i in 0 until 64) {
            val src = values[i]
            val dst = sprites[i]
            dst.y = src.y
            dst.x = src.x
            dst.attributes = src.attributes
            dst.pattern = chrSprites[src.tilenumber.toInt() and 0xFF]
        }
    }
}

fun applyAttributeCellToPpu(ppu: PictureProcessingUnit, nametable: Byte, ax: Byte, ay: Byte, value: UByte) {
    val nt = ppu.backgroundTiles[nametable.toInt() and 0x01]
    val baseX = (ax.toInt() and 0x1F) * 4
    val baseY = (ay.toInt() and 0x1F) * 4
    fun paletteForQuadrant(q: Int): Palette {
        val idx = (value.toInt() shr (q * 2)) and 0x03
        return ppu.backgroundPalettes[idx]
    }
    // Top-left quadrant (bits 0-1): tiles (0..1, 0..1)
    val palTL = paletteForQuadrant(0)
    for (dy in 0..1) for (dx in 0..1) {
        if (baseX + dx in 0 until 32 && baseY + dy in 0 until 30) {
            val t = nt[baseX + dx, baseY + dy]
            nt[baseX + dx, baseY + dy] = t.copy(palette = palTL)
        }
    }
    // Top-right quadrant (bits 2-3): (2..3, 0..1)
    val palTR = paletteForQuadrant(1)
    for (dy in 0..1) for (dx in 2..3) {
        if (baseX + dx in 0 until 32 && baseY + dy in 0 until 30) {
            val t = nt[baseX + dx, baseY + dy]
            nt[baseX + dx, baseY + dy] = t.copy(palette = palTR)
        }
    }
    // Bottom-left quadrant (bits 4-5): (0..1, 2..3)
    val palBL = paletteForQuadrant(2)
    for (dy in 2..3) for (dx in 0..1) {
        if (baseX + dx in 0 until 32 && baseY + dy in 0 until 30) {
            val t = nt[baseX + dx, baseY + dy]
            nt[baseX + dx, baseY + dy] = t.copy(palette = palBL)
        }
    }
    // Bottom-right quadrant (bits 6-7): (2..3, 2..3)
    val palBR = paletteForQuadrant(3)
    for (dy in 2..3) for (dx in 2..3) {
        if (baseX + dx in 0 until 32 && baseY + dy in 0 until 30) {
            val t = nt[baseX + dx, baseY + dy]
            nt[baseX + dx, baseY + dy] = t.copy(palette = palBR)
        }
    }
}