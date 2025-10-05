package com.ivieleague.smbtranslation.chr

import com.ivieleague.smbtranslation.PpuMap
import com.ivieleague.smbtranslation.nes.Pattern
import java.io.File

val rawChrData: ByteArray = run {

    val prgUnitSize = 16 * 1024
    val chrUnitSize = 8 * 1024

    val rom = File("smb.nes")
    if (!rom.exists()) return@run ByteArray(chrUnitSize)
    val data = rom.readBytes()

    // --- Minimal iNES parsing helpers (local to PPU to avoid external deps) ---
    if (data.size < 16) return@run ByteArray(chrUnitSize)
    if (!(data[0] == 'N'.code.toByte() && data[1] == 'E'.code.toByte() && data[2] == 'S'.code.toByte() && data[3] == 0x1A.toByte())) return@run ByteArray(chrUnitSize)
    val prgUnits = data[4].toInt() and 0xFF
    val chrUnits = data[5].toInt() and 0xFF
    val flag6 = data[6].toInt() and 0xFF
    val flag7 = data[7].toInt() and 0xFF
    val hasTrainer = (flag6 and 0x04) != 0
    val isNes2 = (flag7 and 0x0C) == 0x08

    var prgSize = prgUnits * prgUnitSize
    var chrSize = chrUnits * chrUnitSize
    if (isNes2 && data.size >= 16) {
        val sizeMsb = data[9].toInt() and 0xFF
        val prgMsb = sizeMsb and 0x0F
        val chrMsb = (sizeMsb ushr 4) and 0x0F
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