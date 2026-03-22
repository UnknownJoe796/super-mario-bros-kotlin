// by Claude - Typed data structures for the NES level format
package com.ivieleague.smbtranslation.areaparser


/**
 * The 2-byte header at the start of each area's object data.
 *
 * Byte 0: TTPPPSSS
 *   T = game timer setting (2 bits, 6-7)
 *   P = player entrance control (3 bits, 3-5)
 *   S = foreground scenery or background color control (3 bits, 0-2)
 *       Values 0-3 set foreground scenery; 4-7 set background color control
 *
 * Byte 1: SSBBTTTT
 *   S = area style / cloud type override (2 bits, 6-7)
 *       Value 3 means cloud override; 0-2 set area style
 *   B = background scenery (2 bits, 4-5)
 *   T = terrain control (4 bits, 0-3)
 */
@JvmInline
value class AreaHeader(val raw: Short) {
    constructor(byte0: Byte, byte1: Byte) : this(
        ((byte0.toInt() and 0xFF) or ((byte1.toInt() and 0xFF) shl 8)).toShort()
    )

    val byte0: Byte get() = (raw.toInt() and 0xFF).toByte()
    val byte1: Byte get() = ((raw.toInt() ushr 8) and 0xFF).toByte()

    // --- Byte 0 fields ---

    /** Game timer setting (0-3). Controls how much time the player gets. */
    val timerSetting: Int get() = (byte0.toInt() and 0xC0) ushr 6

    /** Player entrance control (0-7). Determines spawn position/behavior. */
    val playerEntranceCtrl: Int get() = (byte0.toInt() and 0x38) ushr 3

    /**
     * Raw 3-bit foreground/background color field (0-7).
     * Values 0-3 are foreground scenery types; 4-7 are background color control.
     */
    val foregroundOrBgColorBits: Int get() = byte0.toInt() and 0x07

    /** Foreground scenery type (0-3), or 0 if this field is used for background color. */
    val foregroundScenery: Int get() = if (foregroundOrBgColorBits < 4) foregroundOrBgColorBits else 0

    /** Background color control (4-7), or 0 if this field is used for foreground scenery. */
    val backgroundColorCtrl: Int get() = if (foregroundOrBgColorBits >= 4) foregroundOrBgColorBits else 0

    // --- Byte 1 fields ---

    /** Terrain control (0-15). Determines ceiling/floor heights. */
    val terrainControl: Int get() = byte1.toInt() and 0x0F

    /** Background scenery type (0-3): 0=none, 1=clouds, 2=mountains+bushes, 3=trees+fences. */
    val backgroundScenery: Int get() = (byte1.toInt() and 0x30) ushr 4

    /** Raw style bits (0-3) from bits 6-7 of byte 1. Value 3 means cloud type override. */
    val styleBits: Int get() = (byte1.toInt() and 0xC0) ushr 6

    /** Area style (0-2), or 0 if cloud type override is active. */
    val areaStyle: Int get() = if (styleBits == 3) 0 else styleBits

    /** True if style bits == 3, which activates cloud block terrain override. */
    val cloudTypeOverride: Boolean get() = styleBits == 3
}

/**
 * The raw area pointer byte stored at $0750.
 * Encodes area type (2 bits) and area data table offset (5 bits).
 *
 * Format: 0TTOOOOO
 *   T = area type (bits 6-5): 0=water, 1=ground, 2=underground, 3=castle
 *   O = low offset (bits 4-0): index within that area type's data table
 */
@JvmInline
value class AreaPointer(val raw: Byte) {
    constructor(raw: Int) : this(raw.toByte())

    /** Area type (0-3): 0=water, 1=ground, 2=underground, 3=castle. */
    val areaType: Int get() = (raw.toInt() and 0x60) ushr 5

    /** Low offset (0-31) into the area type's data table. */
    val lowOffset: Int get() = raw.toInt() and 0x1F
}

/**
 * First byte of a 2-byte area object entry in the level data stream.
 *
 * Format: CCCCYYYY
 *   C = column position within page (high nibble, 0-15)
 *   Y = row position (low nibble, 0-15)
 *       Rows 0-11 are normal screen rows
 *       Row 12 ($0C) = special objects (holes, bridges, question block rows)
 *       Row 13 ($0D) = special objects (flagpole, castle bridge, scroll lock, etc.)
 *       Row 14 ($0E) = alter area attributes
 *       Row 15 ($0F) = special objects (castle, staircase, exit pipe, etc.)
 */
@JvmInline
value class AreaObjByte1(val raw: Byte) {
    /** Column position within the current page (0-15). */
    val column: Int get() = (raw.toInt() and 0xF0) ushr 4

    /** Row position (0-15). Rows 12-15 have special meanings. */
    val row: Int get() = raw.toInt() and 0x0F

    val isEndOfData: Boolean get() = raw == 0xFD.toByte()
    val isSpecialRow12: Boolean get() = row == 0x0C
    val isSpecialRow13: Boolean get() = row == 0x0D
    val isAlterAttributes: Boolean get() = row == 0x0E
    val isSpecialRow15: Boolean get() = row == 0x0F
    val isNormalRow: Boolean get() = row < 0x0C
}

/**
 * First byte of an enemy data entry.
 *
 * Format: XXXXYYYY
 *   X = column position (high nibble, 0-15)
 *   Y = row position (low nibble, 0-15)
 *       Row $0E = page control / area pointer change
 *       Row $0F = page control (set page number)
 */
@JvmInline
value class EnemyObjByte1(val raw: Byte) {
    val rawInt: Int get() = raw.toInt() and 0xFF

    /** Column position within the current page (0-15). High nibble stored as full byte (upper 4 bits set). */
    val columnBits: Int get() = rawInt and 0xF0

    /** Row position (0-15). */
    val row: Int get() = rawInt and 0x0F

    val isEndOfData: Boolean get() = raw == 0xFF.toByte()
    val isPageControl: Boolean get() = row == 0x0F
    val isAreaPointerChange: Boolean get() = row == 0x0E
}
