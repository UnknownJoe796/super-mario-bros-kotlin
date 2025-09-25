package com.ivieleague.smbtranslation.chr


import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.ceil
import kotlin.math.max

private const val INES_HEADER_SIZE = 16
private const val TRAINER_SIZE = 512
private const val PRG_UNIT = 16 * 1024
private const val CHR_UNIT = 8 * 1024

class INesParseException(message: String) : Exception(message)

data class INesHeader(
    val hasTrainer: Boolean,
    val prgRomSize: Int,
    val chrRomSize: Int,
    val mapper: Int,
    val isNes2: Boolean
)

private fun parseInesHeader(data: ByteArray): INesHeader {
    if (data.size < INES_HEADER_SIZE) {
        throw INesParseException("File too small to be an iNES/NES2.0 ROM")
    }
    if (!(data[0] == 'N'.code.toByte() && data[1] == 'E'.code.toByte() && data[2] == 'S'.code.toByte() && data[3] == 0x1A.toByte())) {
        throw INesParseException("Missing iNES magic 'NES\\u001A'")
    }

    val prgRomUnits = data[4].toInt() and 0xFF
    val chrRomUnits = data[5].toInt() and 0xFF
    val flag6 = data[6].toInt() and 0xFF
    val flag7 = data[7].toInt() and 0xFF

    val hasTrainer = (flag6 and 0x04) != 0

    // Minimal NES 2.0 detection: upper two bits of byte 7 contain 0b10 (0x08 in low nibble test)
    val nes2 = (flag7 and 0x0C) == 0x08

    var prgRomSize = prgRomUnits * PRG_UNIT
    var chrRomSize = chrRomUnits * CHR_UNIT

    if (nes2 && data.size >= 16) {
        val sizeMsb = data[9].toInt() and 0xFF
        val prgMsb = sizeMsb and 0x0F
        val chrMsb = (sizeMsb ushr 4) and 0x0F
        prgRomSize = ((prgMsb shl 8) or prgRomUnits) * PRG_UNIT
        chrRomSize = ((chrMsb shl 8) or chrRomUnits) * CHR_UNIT
    }

    val mapper = ((flag7 and 0xF0) or (flag6 ushr 4))

    return INesHeader(
        hasTrainer = hasTrainer,
        prgRomSize = prgRomSize,
        chrRomSize = chrRomSize,
        mapper = mapper,
        isNes2 = nes2,
    )
}

private fun locateChrOffset(data: ByteArray, header: INesHeader): Pair<Int, Int> {
    var offset = INES_HEADER_SIZE
    if (header.hasTrainer) {
        if (data.size < offset + TRAINER_SIZE) throw INesParseException("Trainer indicated, but file too small")
        offset += TRAINER_SIZE
    }
    // Skip PRG to reach CHR
    if (data.size < offset + header.prgRomSize) throw INesParseException("PRG ROM size exceeds file length")
    offset += header.prgRomSize

    val chrSize = header.chrRomSize
    return offset to chrSize
}

private fun decodeChrToImage(chr: ByteArray, tilesPerRow: Int = 16): BufferedImage {
    if (chr.size % 16 != 0) throw INesParseException("CHR size is not a multiple of 16 bytes per tile")
    val numTiles = chr.size / 16
    if (numTiles == 0) throw INesParseException("No CHR ROM present (CHR RAM cart)")

    val tpr = max(1, tilesPerRow)
    val rows = ceil(numTiles / tpr.toDouble()).toInt()

    val width = tpr * 8
    val height = rows * 8

    // Use ARGB image and map 2bpp indices to grayscale colors
    val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

    // 4-tone grayscale (index 0=white, 1=light gray, 2=dark gray, 3=black)
    val palette = intArrayOf(
        0xFFFFFFFF.toInt(), // 0
        0xFFC0C0C0.toInt(), // 1
        0xFF606060.toInt(), // 2
        0xFF000000.toInt(), // 3
    )

    var tileIndex = 0
    while (tileIndex < numTiles) {
        val base = tileIndex * 16
        // 8 bytes plane 0 (bit0 per pixel), then 8 bytes plane 1 (bit1 per pixel)
        val tx = tileIndex % tpr
        val ty = tileIndex / tpr
        val x0 = tx * 8
        val y0 = ty * 8

        for (row in 0 until 8) {
            val b0 = chr[base + row].toInt() and 0xFF
            val b1 = chr[base + 8 + row].toInt() and 0xFF
            for (col in 0 until 8) {
                val mask = 0x80 ushr col
                val lo = if ((b0 and mask) != 0) 1 else 0
                val hi = if ((b1 and mask) != 0) 2 else 0
                val idx = lo or hi // 0..3
                img.setRGB(x0 + col, y0 + row, palette[idx])
            }
        }
        tileIndex++
    }

    return img
}

fun main(args: Array<String>) {
    if (args.size !in 2..3) {
        System.err.println("Usage: kotlin ExtractChrToPng <rom.nes> <out.png> [tilesPerRow=16]")
        return
    }

    val romPath = args[0]
    val outPath = args[1]
    val tilesPerRow = if (args.size >= 3) args[2].toIntOrNull()?.takeIf { it > 0 } ?: 16 else 16

    val data = File(romPath).readBytes()
    val header = parseInesHeader(data)

    if (header.chrRomSize == 0) {
        throw INesParseException("This ROM has no CHR ROM (CHR RAM cartridge). Dump PPU pattern tables at runtime instead.")
    }

    val (chrOff, chrLen) = locateChrOffset(data, header)
    if (data.size < chrOff + chrLen) throw INesParseException("CHR ROM size exceeds file length")

    val chrBytes = data.copyOfRange(chrOff, chrOff + chrLen)
    val image = decodeChrToImage(chrBytes, tilesPerRow)

    val outFile = File(outPath)
    outFile.parentFile?.mkdirs()
    ImageIO.write(image, "png", outFile)

    println("Wrote ${outFile.absolutePath} (${image.width}x${image.height}) with ${chrBytes.size / 16} tiles")
}