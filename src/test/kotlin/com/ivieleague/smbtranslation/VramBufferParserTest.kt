package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.chr.OriginalRom
import com.ivieleague.smbtranslation.nes.Color
import com.ivieleague.smbtranslation.nes.PictureProcessingUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class VramBufferParserTest {

    @Test
    fun parseAttributeStringHorizontal() {
        val ppu = PictureProcessingUnit()
        // addr=$23C0 (attribute table start NT0), len=3 horiz, bytes [0x01,0x02,0x03]
        val bytes = byteArrayOf(
            0x23, 0xC0.toByte(), 0x03, 0x01, 0x02, 0x03,
            0x00
        )
        val updates = BufferedPpuUpdate.parseVramBuffer(bytes)
        assertEquals(1, updates.size)
        val upd = updates[0] as BufferedPpuUpdate.BackgroundAttributeString
        assertEquals(0, upd.nametable.toInt())
        assertEquals(0, upd.ax.toInt())
        assertEquals(0, upd.ay.toInt())
        assertEquals(false, upd.drawVertically)
        assertEquals(listOf(0x01, 0x02, 0x03).map { it.toByte() }, upd.values)

        // Apply and verify palettes applied to the correct 4x4 tile blocks (ax 0,1,2 on ay 0)
        updates.forEach { it(ppu) }
        // We can only sanity-check by sampling a tile from each block's top-left quadrant
        // Block (0,0) uses palette encoded in bits0..1 of 0x01 -> index 1
        val nt = ppu.backgroundTiles[0]
        // Set known patterns so that copies don’t change palette object identity aside from what applyAttributeCell sets
        // Sample tile at (0,0) should now use backgroundPalettes[1]
        assertEquals(ppu.backgroundPalettes[1], nt[0, 0].palette)
        // Next blocks (1,0) and (2,0)
        assertEquals(ppu.backgroundPalettes[(0x02) and 0x03], nt[4, 0].palette)
        assertEquals(ppu.backgroundPalettes[(0x03) and 0x03], nt[8, 0].palette)
    }

    @Test
    fun parseAttributeRepeatHorizontal() {
        val ppu = PictureProcessingUnit()
        // addr=$23C5 (ax=5, ay=0), control repeat,len=4 horiz (0x44), data=0x07
        val bytes = byteArrayOf(
            0x23, 0xC5.toByte(), 0x44, 0x07,
            0x00
        )
        val updates = BufferedPpuUpdate.parseVramBuffer(bytes)
        assertEquals(1, updates.size)
        val upd = updates[0] as BufferedPpuUpdate.BackgroundAttributeRepeat
        assertEquals(0, upd.nametable.toInt())
        assertEquals(5, upd.ax.toInt())
        assertEquals(0, upd.ay.toInt())
        assertEquals(false, upd.drawVertically)
        assertEquals(4, upd.repetitions)
        assertEquals(0x07.toByte(), upd.value)

        // Apply and verify a couple of affected tiles (e.g., (20,0) in tiles since ax=5 -> x=20)
        updates.forEach { it(ppu) }
        val nt = ppu.backgroundTiles[0]
        // 0x07 -> BR quadrant palette index 1 (bits 6..7=0), TL index 3? Actually 0x07: TL=3, TR=1, BL=1, BR=0
        // We’ll just spot-check TL of the first block: palette index 3
        assertEquals(ppu.backgroundPalettes[0x07 and 0x03], nt[20, 0].palette)
    }

    @Test
    fun parseAttributeStringVertical() {
        val ppu = PictureProcessingUnit()
        // addr=$23C0 (ax=0, ay=0), len=2 vertical, bytes [0x01, 0x02]
        val bytes = byteArrayOf(
            0x23, 0xC0.toByte(), 0x83.toByte(), 0x01, 0x02,
            0x00
        )
        val updates = BufferedPpuUpdate.parseVramBuffer(bytes)
        assertEquals(1, updates.size)
        val upd = updates[0] as BufferedPpuUpdate.BackgroundAttributeString
        assertEquals(true, upd.drawVertically)
        assertEquals(0, upd.ax.toInt())
        assertEquals(0, upd.ay.toInt())
        // Apply and verify the first two blocks down the first column got palettes set
        updates.forEach { it(ppu) }
        val nt = ppu.backgroundTiles[0]
        assertEquals(ppu.backgroundPalettes[0x01 and 0x03], nt[0, 0].palette)
        assertEquals(ppu.backgroundPalettes[0x02 and 0x03], nt[0, 4].palette)
    }

    @Test
    fun parseBackgroundStringAndApply() {
        val ppu = PictureProcessingUnit()
        // Sequence: addr=$2000, control len=3 horiz, data [1,2,3], terminator
        val bytes = byteArrayOf(
            0x20, 0x00, 0x03, 0x01, 0x02, 0x03,
            0x00
        )
        val updates = BufferedPpuUpdate.parseVramBuffer(bytes)
        assertEquals(1, updates.size)
        // Apply
        updates.forEach { it(ppu) }
        // Verify patterns written at (0,0), (1,0), (2,0) in nametable 0
        val nt = ppu.backgroundTiles[0]
        assertEquals(OriginalRom.backgrounds[1], nt[0, 0].pattern)
        assertEquals(OriginalRom.backgrounds[2], nt[1, 0].pattern)
        assertEquals(OriginalRom.backgrounds[3], nt[2, 0].pattern)
    }

    @Test
    fun parseBackgroundRepeatAndApply() {
        val ppu = PictureProcessingUnit()
        // Sequence: addr=$2002, control repeat,len=4 horiz (0b01000100=0x44), data single byte 0x07, terminator
        val bytes = byteArrayOf(
            0x20.toByte(), 0x02, 0x44, 0x07,
            0x00
        )
        val updates = BufferedPpuUpdate.parseVramBuffer(bytes)
        assertEquals(1, updates.size)
        // Apply
        updates.forEach { it(ppu) }
        val nt = ppu.backgroundTiles[0]
        val pat = OriginalRom.backgrounds[7]
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
        val result = BufferedPpuUpdate.parseVramBuffer(bytes)
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
        val result = BufferedPpuUpdate.parseVramBuffer(bytes)
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
        val result = BufferedPpuUpdate.parseVramBuffer(bytes)
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
