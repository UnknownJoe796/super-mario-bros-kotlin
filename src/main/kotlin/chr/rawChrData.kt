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
        loadChrData(rawChrData, sprites, backgrounds)
    }
}

/** SMB2J CHR tile set loaded from FDS disk image. */
object Smb2jRom {
    val sprites = Array<Pattern>(256) { Pattern() }
    val backgrounds = Array<Pattern>(256) { Pattern() }

    init {
        loadChrData(smb2jChrData, sprites, backgrounds)
    }
}

private fun loadChrData(chrData: ByteArray, sprites: Array<Pattern>, backgrounds: Array<Pattern>) {
    if (chrData.size < 8192) return
    val sprStart = 0
    val bgStart = 16 * 256
    for (i in 0 until 256) {
        val bgSlice = chrData.copyOfRange(bgStart + i * 16, bgStart + (i + 1) * 16)
        val sprSlice = chrData.copyOfRange(sprStart + i * 16, sprStart + (i + 1) * 16)
        backgrounds[i] = Pattern(bgSlice, PpuMap.background[i], "chr.backgrounds[0x${i.toString(16)}]", tileIndex = i)
        sprites[i] = Pattern(sprSlice, PpuMap.sprites[i], "chr.sprites[0x${i.toString(16)}]", tileIndex = i)
    }
}

/** Load SMB2J CHR data from FDS disk image. Returns 8KB CHR with SM2CHAR2 patch applied. */
val smb2jChrData: ByteArray = run {
    val fdsFile = java.io.File("smb2j.fds")
    if (!fdsFile.exists()) return@run ByteArray(8192)
    val data = fdsFile.readBytes()
    val start = if (data.size >= 4 && data[0] == 0x46.toByte() && data[1] == 0x44.toByte() &&
        data[2] == 0x53.toByte() && data[3] == 0x1A.toByte()) 16 else 0

    // Parse FDS disk to find SM2CHAR1 and SM2CHAR2
    var offset = start + 56 // skip block 1
    if (offset >= data.size || data[offset] != 0x02.toByte()) return@run ByteArray(8192)
    val nfiles = data[offset + 1].toInt() and 0xFF
    offset += 2

    var chrData = ByteArray(8192)
    for (i in 0 until nfiles) {
        if (offset >= data.size || data[offset] != 0x03.toByte()) break
        val filename = String(data, offset + 3, 8).trim()
        val loadAddr = (data[offset + 11].toInt() and 0xFF) or ((data[offset + 12].toInt() and 0xFF) shl 8)
        val fileSize = (data[offset + 13].toInt() and 0xFF) or ((data[offset + 14].toInt() and 0xFF) shl 8)
        val fileType = data[offset + 15].toInt() and 0xFF
        offset += 16
        if (offset < data.size && data[offset] == 0x04.toByte()) {
            offset++ // skip block type
            if (fileType == 1) { // CHR type
                if (filename.contains("SM2CHAR1")) {
                    // Full 8KB CHR tile set
                    val end = minOf(offset + fileSize, data.size)
                    chrData = data.copyOfRange(offset, end)
                    if (chrData.size < 8192) chrData = chrData.copyOf(8192)
                } else if (filename.contains("SM2CHAR2")) {
                    // Small CHR patch at PPU address loadAddr
                    val patchSize = minOf(fileSize, data.size - offset)
                    for (j in 0 until patchSize) {
                        val patchAddr = loadAddr + j
                        if (patchAddr in chrData.indices) {
                            chrData[patchAddr] = data[offset + j]
                        }
                    }
                }
            }
            offset += fileSize
        }
    }
    chrData
}