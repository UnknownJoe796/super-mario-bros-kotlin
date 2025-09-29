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

class Pattern(val bits: ByteArray = ByteArray(16), val name: String? = null) {
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

class IndirectPalette(var palette: Palette) : Palette {
    override val colors: Array<Color> get() = palette.colors
}

class DirectPalette(
    override val colors: Array<Color> = arrayOf(
        Color(0xFF000000.toInt()),
        Color(0xFFFFFFFF.toInt()),
        Color(0xFFBBBBBB.toInt()),
        Color(0xFF666666.toInt()),
    )
): Palette

@JvmInline
value class Color(val argb: Int) {
    companion object {
        val nesPaletteLookup = intArrayOf(
            0x7C7C7C,
            0x0000FC,
            0x0000BC,
            0x4428BC,
            0x940084,
            0xA80020,
            0xA81000,
            0x881400,
            0x503000,
            0x007800,
            0x006800,
            0x005800,
            0x004058,
            0x000000,
            0x000000,
            0x000000,
            0xBCBCBC,
            0x0078F8,
            0x0058F8,
            0x6844FC,
            0xD800CC,
            0xE40058,
            0xF83800,
            0xE45C10,
            0xAC7C00,
            0x00B800,
            0x00A800,
            0x00A844,
            0x008888,
            0x000000,
            0x000000,
            0x000000,
            0xF8F8F8,
            0x3CBCFC,
            0x6888FC,
            0x9878F8,
            0xF878F8,
            0xF85898,
            0xF87858,
            0xFCA044,
            0xF8B800,
            0xB8F818,
            0x58D854,
            0x58F898,
            0x00E8D8,
            0x787878,
            0x000000,
            0x000000,
            0xFCFCFC,
            0xA4E4FC,
            0xB8B8F8,
            0xD8B8F8,
            0xF8B8F8,
            0xF8A4C0,
            0xF0D0B0,
            0xFCE0A8,
            0xF8D878,
            0xD8F878,
            0xB8F8B8,
            0xB8F8D8,
            0x00FCFC,
            0xF8D8F8,
            0x000000,
            0x000000,
        )
    }

    constructor(byte: Byte) : this(
        nesPaletteLookup[byte.toInt() and 31],
    )
}

class Sprite {
    var y: UByte = 0U
    var pattern: Pattern = Pattern()
    var attributes: SpriteFlags = SpriteFlags()
    var x: UByte = 0U
}