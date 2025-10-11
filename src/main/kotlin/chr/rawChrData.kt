package com.ivieleague.smbtranslation.chr

import com.ivieleague.smbtranslation.PpuMap
import com.ivieleague.smbtranslation.nes.Pattern
import com.ivieleague.smbtranslation.utils.shl
import com.ivieleague.smbtranslation.utils.shr
import java.io.File
import kotlin.experimental.and
import kotlin.experimental.or

val rawChrData: ByteArray = run {

    val prgUnitSize = 16 * 1024
    val chrUnitSize = 8 * 1024

    val rom = File("smb.nes")
    if (!rom.exists()) return@run ByteArray(chrUnitSize)
    val data = rom.readBytes()

    // --- Minimal iNES parsing helpers (local to PPU to avoid external deps) ---
    if (data.size < 16) return@run ByteArray(chrUnitSize)
    if (!(data[0] == 'N'.code.toByte() && data[1] == 'E'.code.toByte() && data[2] == 'S'.code.toByte() && data[3] == 0x1A.toByte())) return@run ByteArray(chrUnitSize)
    val prgUnits = data[4]
    val chrUnits = data[5]
    val flag6 = data[6]
    val flag7 = data[7]
    val hasTrainer = (flag6 and 0x04) != 0.toByte()
    val isNes2 = (flag7 and 0x0C) == 0x08.toByte()

    var prgSize = prgUnits * prgUnitSize
    var chrSize = chrUnits * chrUnitSize
    if (isNes2) {
        val sizeMsb = data[9]
        val prgMsb = sizeMsb and 0x0F
        val chrMsb = (sizeMsb.toUByte() shr 4).toByte() and 0x0F
        prgSize = ((prgMsb shl 8) or prgUnits) * prgUnitSize
        chrSize = ((chrMsb shl 8) or chrUnits) * chrUnitSize
    }

    var offset = 16
    if (hasTrainer) offset += 512
    offset += prgSize
    if (chrSize <= 0) ByteArray(chrUnitSize)
    else if (data.size < offset + chrSize) ByteArray(chrUnitSize)
    else data.copyOfRange(offset, offset + chrSize)
}


object OriginalRom {
    val sprites = Array<Pattern>(256) { Pattern() }
    val backgrounds = Array<Pattern>(256) { Pattern() }

    init {
        // Attempt to parse CHR data from the bundled SMB ROM and populate pattern tables.
        // Non-fatal on failure: tests and other code paths should not crash if the ROM is missing.
        val sprStart = 0
        val bgStart = 16 * 256
        for (i in 0 until 256) {
            val bgSlice = rawChrData.copyOfRange(bgStart + i * 16, bgStart + (i + 1) * 16)
            val sprSlice = rawChrData.copyOfRange(sprStart + i * 16, sprStart + (i + 1) * 16)
            backgrounds[i] = Pattern(bgSlice, PpuMap.background[i], "OriginalRom.backgrounds[0x${i.toString(16)}]")
            sprites[i] = Pattern(sprSlice, PpuMap.sprites[i], "OriginalRom.sprites[0x${i.toString(16)}]")
        }
    }
}