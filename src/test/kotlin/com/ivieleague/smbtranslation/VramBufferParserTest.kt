package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.nes.Color
import com.ivieleague.smbtranslation.nes.PictureProcessingUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class VramBufferParserTest {

    @Test
    fun parseBackgroundStringAndApply() {
        val ppu = PictureProcessingUnit()
        // Sequence: addr=$2000, control len=3 horiz, data [1,2,3], terminator
        val bytes = byteArrayOf(
            0x20, 0x00, 0x03, 0x01, 0x02, 0x03,
            0x00
        )
        val updates = BufferedPpuUpdate.parseVramBuffer(ppu, bytes)
        assertEquals(1, updates.size)
        // Apply
        updates.forEach { it(ppu) }
        // Verify patterns written at (0,0), (1,0), (2,0) in nametable 0
        val nt = ppu.backgroundTiles[0]
        assertEquals(ppu.originalRomBackgrounds[1], nt[0, 0].pattern)
        assertEquals(ppu.originalRomBackgrounds[2], nt[1, 0].pattern)
        assertEquals(ppu.originalRomBackgrounds[3], nt[2, 0].pattern)
    }

    @Test
    fun parseBackgroundRepeatAndApply() {
        val ppu = PictureProcessingUnit()
        // Sequence: addr=$2002, control repeat,len=4 horiz (0b01000100=0x44), data single byte 0x07, terminator
        val bytes = byteArrayOf(
            0x20.toByte(), 0x02, 0x44, 0x07,
            0x00
        )
        val updates = BufferedPpuUpdate.parseVramBuffer(ppu, bytes)
        assertEquals(1, updates.size)
        // Apply
        updates.forEach { it(ppu) }
        val nt = ppu.backgroundTiles[0]
        val pat = ppu.originalRomBackgrounds[7]
        assertEquals(pat, nt[2, 0].pattern)
        assertEquals(pat, nt[3, 0].pattern)
        assertEquals(pat, nt[4, 0].pattern)
        assertEquals(pat, nt[5, 0].pattern)
    }

    @Test fun testMarioThanksMessage() {
//        MarioThanksMessage:
//        ;"THANK YOU MARIO!"
        val ppu = PictureProcessingUnit()
        val bytes = byteArrayOf(0x25, 0x48, 0x10, 0x1d, 0x11, 0x0a, 0x17, 0x14, 0x24, 0x22, 0x18, 0x1e, 0x24, 0x16, 0x0a, 0x1b, 0x12, 0x18, 0x2b, 0x00)
        val result = BufferedPpuUpdate.parseVramBuffer(ppu, bytes)
        assertEquals("THANK YOU MARIO!", (result[0] as BufferedPpuUpdate.BackgroundPatternString).patterns.joinToString("") { it.name ?: "*" })
    }

    @Test fun testBowserPaletteData() {
//        BowserPaletteData:
//        .db $3f, $14, $04
//        .db $0f, $1a, $30, $27
//        .db $00
        val ppu = PictureProcessingUnit()
        val bytes = byteArrayOf(
            0x3f, 0x14,  // Address
            0x04,  // Control Byte
            0x0f, 0x1a, 0x30, 0x27,  // Colors
            0x00  // Closing byte
        )
        val result = BufferedPpuUpdate.parseVramBuffer(ppu, bytes)
        assertEquals(BufferedPpuUpdate.SpriteSetPalette(
            index = 1,
            colors = listOf(
                Color(byte = 0x0f),
                Color(byte = 0x1a),
                Color(byte = 0x30),
                Color(byte = 0x27),
            )
        ), result[0])
    }
    @Test fun testGroundPaletteData() {
        val ppu = PictureProcessingUnit()
        val bytes = byteArrayOf(
            0x3f, 0x00,  // Address
            0x20,  // Control
            0x0f, 0x29, 0x1a, 0x0f,  // Background palette 0
            0x0f, 0x36, 0x17, 0x0f,  // Background palette 1
            0x0f, 0x30, 0x21, 0x0f,  // Background palette 2
            0x0f, 0x27, 0x17, 0x0f,  // Background palette 3
            0x0f, 0x16, 0x27, 0x18,  // Sprite palette 0
            0x0f, 0x1a, 0x30, 0x27,  // Sprite palette 1
            0x0f, 0x16, 0x30, 0x27,  // Sprite palette 2
            0x0f, 0x0f, 0x36, 0x17,  // Sprite palette 3
            0x00,
        )
        val result = BufferedPpuUpdate.parseVramBuffer(ppu, bytes)
        assertEquals(listOf(0x0f, 0x29, 0x1a, 0x0f).map { Color(it.toByte()) }, (result[0] as BufferedPpuUpdate.BackgroundSetPalette).colors)
        assertEquals(listOf(0x0f, 0x36, 0x17, 0x0f).map { Color(it.toByte()) }, (result[1] as BufferedPpuUpdate.BackgroundSetPalette).colors)
        assertEquals(listOf(0x0f, 0x30, 0x21, 0x0f).map { Color(it.toByte()) }, (result[2] as BufferedPpuUpdate.BackgroundSetPalette).colors)
        assertEquals(listOf(0x0f, 0x27, 0x17, 0x0f).map { Color(it.toByte()) }, (result[3] as BufferedPpuUpdate.BackgroundSetPalette).colors)
        assertEquals(listOf(0x0f, 0x16, 0x27, 0x18).map { Color(it.toByte()) }, (result[4] as BufferedPpuUpdate.SpriteSetPalette).colors)
        assertEquals(listOf(0x0f, 0x1a, 0x30, 0x27).map { Color(it.toByte()) }, (result[5] as BufferedPpuUpdate.SpriteSetPalette).colors)
        assertEquals(listOf(0x0f, 0x16, 0x30, 0x27).map { Color(it.toByte()) }, (result[6] as BufferedPpuUpdate.SpriteSetPalette).colors)
        assertEquals(listOf(0x0f, 0x0f, 0x36, 0x17).map { Color(it.toByte()) }, (result[7] as BufferedPpuUpdate.SpriteSetPalette).colors)
    }

    @Test fun ultimateTest() {
        with(System()) {
            println("ram.vRAMBuffer1: ${ram.vRAMBuffer1}")
            println("WaterPaletteData: ${WaterPaletteData}")
            println("GroundPaletteData: ${GroundPaletteData}")
            println("UndergroundPaletteData: ${UndergroundPaletteData}")
            println("CastlePaletteData: ${CastlePaletteData}")
            println("ram.vRAMBuffer1: ${ram.vRAMBuffer1}")
            println("ram.vRAMBuffer2: ${ram.vRAMBuffer2}")
            println("ram.vRAMBuffer2: ${ram.vRAMBuffer2}")
            println("BowserPaletteData: ${BowserPaletteData}")
            println("DaySnowPaletteData: ${DaySnowPaletteData}")
            println("NightSnowPaletteData: ${NightSnowPaletteData}")
            println("MushroomPaletteData: ${MushroomPaletteData}")
            println("MarioThanksMessage: ${MarioThanksMessage}")
            println("LuigiThanksMessage: ${LuigiThanksMessage}")
            println("MushroomRetainerSaved: ${MushroomRetainerSaved}")
            println("PrincessSaved1: ${PrincessSaved1}")
            println("PrincessSaved2: ${PrincessSaved2}")
            println("WorldSelectMessage1: ${WorldSelectMessage1}")
            println("WorldSelectMessage2: ${WorldSelectMessage2}")
        }
    }
}
