package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.nes.PictureProcessingUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class AttributeWrappingTest {
    @Test
    fun horizontalWrapsToNextRow() {
        val ppu = PictureProcessingUnit()
        // Start at $23C7 (ax=7, ay=0), len=3 horizontal, bytes [1,2,3]
        val bytes = byteArrayOf(
            0x23, 0xC7.toByte(), 0x03, 0x01, 0x02, 0x03,
            0x00
        )
        val updates = BufferedPpuUpdate.parseVramBuffer(ppu, bytes)
        updates.forEach { it(ppu) }
        val nt = ppu.backgroundTiles[0]
        // (ax,ay) = (7,0) affects tiles starting at x=28,y=0
        assertEquals(ppu.backgroundPalettes[0x01 and 0x03], nt[28, 0].palette)
        // Next should wrap to (0,1) -> tiles (0,4)
        assertEquals(ppu.backgroundPalettes[0x02 and 0x03], nt[0, 4].palette)
        // Then (1,1) -> tiles (4,4)
        assertEquals(ppu.backgroundPalettes[0x03 and 0x03], nt[4, 4].palette)
    }

    @Test
    fun verticalWrapsToNextColumn() {
        val ppu = PictureProcessingUnit()
        // Start at $23F8 (ax=0, ay=7), len=3 vertical, bytes [1,2,3]
        val bytes = byteArrayOf(
            0x23, 0xF8.toByte(), 0x83.toByte(), 0x01, 0x02, 0x03,
            0x00
        )
        val updates = BufferedPpuUpdate.parseVramBuffer(ppu, bytes)
        updates.forEach { it(ppu) }
        val nt = ppu.backgroundTiles[0]
        // (0,7) -> tiles (0,28)
        assertEquals(ppu.backgroundPalettes[0x01 and 0x03], nt[0, 28].palette)
        // Wrap to (1,0) -> tiles (4,0)
        assertEquals(ppu.backgroundPalettes[0x02 and 0x03], nt[4, 0].palette)
        // Then (1,1) -> tiles (4,4)
        assertEquals(ppu.backgroundPalettes[0x03 and 0x03], nt[4, 4].palette)
    }
}