package com.ivieleague.smbtranslation.nes

import com.ivieleague.smbtranslation.utils.SpriteFlags

class NesNametable(val width: Int = 32, val height: Int = 30) {
    private val rawTiles = Array<Tile>(width * height) { Tile(Pattern.EMPTY, Palette.EMPTY) }
    operator fun get(x: Int, y: Int): Tile = rawTiles[y * width + x]
    operator fun set(x: Int, y: Int, value: Tile) {
        rawTiles[y * width + x] = value
    }
}

data class Tile(
    val pattern: Pattern,
    val palette: Palette,
)

class Pattern(val bits: ByteArray = ByteArray(16), val name: String? = null, val source: String? = null) {
    // NES 2bpp format: first 8 bytes are bit plane 0 (one byte per row), next 8 are bit plane 1
    // Each pixel's color index is (bit1 << 1) | bit0, with bit 7 being the leftmost pixel
    fun colorIndex(x: Int, y: Int): Int {
        require(x in 0..7 && y in 0..7) { "x/y out of range for 8x8 pattern" }
        val plane0 = bits[y].toInt() and 0xFF
        val plane1 = bits[8 + y].toInt() and 0xFF
        val shift = 7 - (x and 7)
        val b0 = (plane0 shr shift) and 1
        val b1 = (plane1 shr shift) and 1
        return (b1 shl 1) or b0
    }
    companion object {
        val EMPTY = Pattern()
    }

    override fun toString(): String = name ?: "??"
}

interface Palette {
    val colors: Array<Color>  // 4 colors, first mostly ignored
    companion object {
        val RGB = DirectPalette(arrayOf(
            Color(0xFF000000.toInt()),
            Color(0xFFFF0000.toInt()),
            Color(0xFF00FF00.toInt()),
            Color(0xFF0000FF.toInt()),
        ))
        val GRAYSCALE = DirectPalette(arrayOf(
            Color(0xFF000000.toInt()),
            Color(0xFFFFFFFF.toInt()),
            Color(0xFFBBBBBB.toInt()),
            Color(0xFF666666.toInt()),
        ))
        val EMPTY = GRAYSCALE
    }
}

class IndirectPalette(var palette: Palette, val label: String) : Palette {
    override val colors: Array<Color> get() = palette.colors
    override fun toString(): String = "Indirect $label ($palette)"
}

class DirectPalette(
    override val colors: Array<Color> = arrayOf(
        Color(0xFF000000.toInt()),
        Color(0xFFFFFFFF.toInt()),
        Color(0xFFBBBBBB.toInt()),
        Color(0xFF666666.toInt()),
    )
): Palette {
    override fun toString(): String = colors.joinToString(", ")
}

@JvmInline
value class Color(val argb: Int) {
    companion object {
        val nesPaletteLookup = intArrayOf(
            0x626262,
            0x001C95,
            0x1904AC,
            0x42009D,
            0x61006B,
            0x6E0025,
            0x650500,
            0x491E00,
            0x223700,
            0x004900,
            0x004F00,
            0x004816,
            0x00355E,
            0x000000,
            0x000000,
            0x000000,

            0xABABAB,
            0x0C4EDB,
            0x3D2EFF,
            0x7115F3,
            0x9B0BB9,
            0xB01262,
            0xA92704,
            0x894600,
            0x576600,
            0x237F00,
            0x008900,
            0x008332,
            0x006D90,
            0x000000,
            0x000000,
            0x000000,

            0xFFFFFF,
            0x57A5FF,
            0x8287FF,
            0xB46DFF,
            0xDF60FF,
            0xF863C6,
            0xF8746D,
            0xDE9020,
            0xB3AE00,
            0x81C800,
            0x56D522,
            0x3DD36F,
            0x3EC1C8,
            0x4E4E4E,
            0x000000,
            0x000000,

            0xFFFFFF,
            0xBEE0FF,
            0xCDD4FF,
            0xE0CAFF,
            0xF1C4FF,
            0xFCC4EF,
            0xFDCACE,
            0xF5D4AF,
            0xE6DF9C,
            0xD3E99A,
            0xC2EFA8,
            0xB7EFC4,
            0xB6EAE5,
            0xB8B8B8,
            0x000000,
            0x000000,
        )
    }

    constructor(byte: Byte) : this(
        nesPaletteLookup[byte.toInt() and 0x3F] + 0xFF000000.toInt(),
    )

    override fun toString(): String = "#${argb.toHexString().padStart(8, '0')}"
}

class Sprite {
    var y: UByte = 0U
    var pattern: Pattern = Pattern()
    var attributes: SpriteFlags = SpriteFlags()
    var x: UByte = 0U
}