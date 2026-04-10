package com.ivieleague.smbtranslation.utils

import com.ivieleague.smbtranslation.GameRam

@JvmInline
value class JoypadBits(val byte: Byte) {
    val a: Boolean get() = byte.bit(7)
    val b: Boolean get() = byte.bit(6)
    val select: Boolean get() = byte.bit(5)
    val start: Boolean get() = byte.bit(4)
    val up: Boolean get() = byte.bit(3)
    val down: Boolean get() = byte.bit(2)
    val left: Boolean get() = byte.bit(1)
    val right: Boolean get() = byte.bit(0)
    constructor(
        a: Boolean = false,
        b: Boolean = false,
        select: Boolean = false,
        start: Boolean = false,
        up: Boolean = false,
        down: Boolean = false,
        left: Boolean = false,
        right: Boolean = false,
    ): this((
            (if (a) 0x1 shl 7 else 0) +
                    (if (b) 0x1 shl 6 else 0) +
                    (if (select) 0x1 shl 5 else 0) +
                    (if (start) 0x1 shl 4 else 0) +
                    (if (up) 0x1 shl 3 else 0) +
                    (if (down) 0x1 shl 2 else 0) +
                    (if (left) 0x1 shl 1 else 0) +
                    (if (right) 0x1 shl 0 else 0)
            ).toByte())
    fun copy(
        a: Boolean = this.a,
        b: Boolean = this.b,
        select: Boolean = this.select,
        start: Boolean = this.start,
        up: Boolean = this.up,
        down: Boolean = this.down,
        left: Boolean = this.left,
        right: Boolean = this.right,
    ) = JoypadBits(a, b, select, start, up, down, left, right)

    companion object : SimpleRamConverter<JoypadBits>() {
        override fun toByte(value: JoypadBits): Byte = value.byte
        override fun fromByte(byte: Byte): JoypadBits = JoypadBits(byte)
    }
}

@JvmInline
value class SpriteFlags(val byte: Byte) {
    val flipVertical: Boolean get() = byte.bit(7)
    val flipHorizontal: Boolean get() = byte.bit(6)
    val behindBackground: Boolean get() = byte.bit(5)
    val palette: Byte get() = byte.bitRange(0, 1)
    constructor(
        flipVertical: Boolean = false,
        flipHorizontal: Boolean = false,
        behindBackground: Boolean = false,
        palette: Byte = 0,
    ): this((
            (if (flipVertical) 0x1 shl 7 else 0) +
                    (if (flipHorizontal) 0x1 shl 6 else 0) +
                    (if (behindBackground) 0x1 shl 5 else 0) +
                    (palette.toInt() and 0x03)
            ).toByte())
    fun copy(
        flipVertical: Boolean = this.flipVertical,
        flipHorizontal: Boolean = this.flipHorizontal,
        behindBackground: Boolean = this.behindBackground,
        palette: Byte = this.palette,
    ) = SpriteFlags(flipVertical, flipHorizontal, behindBackground, palette)

    companion object : SimpleRamConverter<SpriteFlags>() {
        override fun toByte(value: SpriteFlags): Byte = value.byte
        override fun fromByte(byte: Byte): SpriteFlags = SpriteFlags(byte)
    }
}

@JvmInline
value class PpuControl(val byte: Byte) {
    /** Vblank NMI enable (0: off, 1: on) **/
    val nmiEnabled: Boolean get() = byte.bit(7)
    /** PPU master/slave select (0: read backdrop from EXT pins; 1: output color on EXT pins) **/
    val extWrite: Boolean get() = byte.bit(6)
    /** Sprite size (0: 8x8 pixels; 1: 8x16 pixels – see PPU OAM#Byte 1) **/
    val tallSpriteMode: Boolean get() = byte.bit(5)
    /** Background pattern table address (0: $0000; 1: $1000) **/
    val backgroundTableOffset: Boolean get() = byte.bit(4)
    /** Sprite pattern table address for 8x8 sprites (0: $0000; 1: $1000; ignored in 8x16 mode) **/
    val spritePatternTableOffset: Boolean get() = byte.bit(3)
    /** VRAM address increment per CPU read/write of PPUDATA (0: add 1, going across; 1: add 32, going down) **/
    val drawVertical: Boolean get() = byte.bit(2)
    /** Base nametable address (0 = $2000; 1 = $2400; 2 = $2800; 3 = $2C00) **/
    val baseNametableAddress: Byte get() = byte.bitRange(0, 1)
    constructor(
        nmiEnabled: Boolean = false,
        extWrite: Boolean = false,
        tallSpriteMode: Boolean = false,
        backgroundTableOffset: Boolean = false,
        spritePatternTableOffset: Boolean = false,
        drawVertical: Boolean = false,
        baseNametableAddress: Byte = 0,
    ): this((
            (if (nmiEnabled) 0x1 shl 7 else 0) +
                    (if (extWrite) 0x1 shl 6 else 0) +
                    (if (tallSpriteMode) 0x1 shl 5 else 0) +
                    (if (backgroundTableOffset) 0x1 shl 4 else 0) +
                    (if (spritePatternTableOffset) 0x1 shl 3 else 0) +
                    (if (drawVertical) 0x1 shl 2 else 0) +
                    (baseNametableAddress.toInt() shr 0 and 1.shl(1).minus(1))
            ).toByte())
    fun copy(
        nmiEnabled: Boolean = this.nmiEnabled,
        extWrite: Boolean = this.extWrite,
        tallSpriteMode: Boolean = this.tallSpriteMode,
        backgroundTableOffset: Boolean = this.backgroundTableOffset,
        spritePatternTableOffset: Boolean = this.spritePatternTableOffset,
        drawVertical: Boolean = this.drawVertical,
        baseNametableAddress: Byte = this.baseNametableAddress,
    ) = PpuControl(nmiEnabled, extWrite, tallSpriteMode, backgroundTableOffset, spritePatternTableOffset, drawVertical, baseNametableAddress)

    companion object : SimpleRamConverter<PpuControl>() {
        override fun toByte(value: PpuControl): Byte = value.byte
        override fun fromByte(byte: Byte): PpuControl = PpuControl(byte)
    }
}

@JvmInline
value class PpuMask(val byte: Byte) {
    /** Greyscale (0: normal color, 1: greyscale) **/
    val greyscale: Boolean get() = byte.bit(0)
    /** 1: Show background in leftmost 8 pixels of screen, 0: Hide **/
    val showLeftBackground: Boolean get() = byte.bit(1)
    /** 1: Show sprites in leftmost 8 pixels of screen, 0: Hide **/
    val showLeftSprites: Boolean get() = byte.bit(2)
    /** 1: Enable background rendering **/
    val backgroundEnabled: Boolean get() = byte.bit(3)
    /** 1: Enable sprite rendering **/
    val spriteEnabled: Boolean get() = byte.bit(4)
    /** Emphasize red (green on PAL/Dendy) **/
    val emphasizeRed: Boolean get() = byte.bit(5)
    /** Emphasize green (red on PAL/Dendy) **/
    val emphasizeGreen: Boolean get() = byte.bit(6)
    /** Emphasize blue **/
    val emphasizeBlue: Boolean get() = byte.bit(7)
    constructor(
        greyscale: Boolean = false,
        showLeftBackground: Boolean = false,
        showLeftSprites: Boolean = false,
        backgroundEnabled: Boolean = false,
        spriteEnabled: Boolean = false,
        emphasizeRed: Boolean = false,
        emphasizeGreen: Boolean = false,
        emphasizeBlue: Boolean = false,
    ): this((
            (if (greyscale) 0x1 shl 0 else 0) +
                    (if (showLeftBackground) 0x1 shl 1 else 0) +
                    (if (showLeftSprites) 0x1 shl 2 else 0) +
                    (if (backgroundEnabled) 0x1 shl 3 else 0) +
                    (if (spriteEnabled) 0x1 shl 4 else 0) +
                    (if (emphasizeRed) 0x1 shl 5 else 0) +
                    (if (emphasizeGreen) 0x1 shl 6 else 0) +
                    (if (emphasizeBlue) 0x1 shl 7 else 0)
            ).toByte())
    fun copy(
        greyscale: Boolean = this.greyscale,
        showLeftBackground: Boolean = this.showLeftBackground,
        showLeftSprites: Boolean = this.showLeftSprites,
        backgroundEnabled: Boolean = this.backgroundEnabled,
        spriteEnabled: Boolean = this.spriteEnabled,
        emphasizeRed: Boolean = this.emphasizeRed,
        emphasizeGreen: Boolean = this.emphasizeGreen,
        emphasizeBlue: Boolean = this.emphasizeBlue,
    ) = PpuMask(greyscale, showLeftBackground, showLeftSprites, backgroundEnabled, spriteEnabled, emphasizeRed, emphasizeGreen, emphasizeBlue)

    companion object : SimpleRamConverter<PpuMask>() {
        override fun toByte(value: PpuMask): Byte = value.byte
        override fun fromByte(byte: Byte): PpuMask = PpuMask(byte)
    }
}

@JvmInline
value class PpuStatus(val byte: Byte) {
    val vblank: Boolean get() = byte.bit(7)
    val sprite0Hit: Boolean get() = byte.bit(6)
    val spriteOverflow: Boolean get() = byte.bit(5)
    constructor(
        vblank: Boolean = false,
        sprite0Hit: Boolean = false,
        spriteOverflow: Boolean = false,
    ): this((
            (if (vblank) 0x1 shl 7 else 0) +
                    (if (sprite0Hit) 0x1 shl 6 else 0) +
                    (if (spriteOverflow) 0x1 shl 5 else 0)
            ).toByte())
    fun copy(
        vblank: Boolean = this.vblank,
        sprite0Hit: Boolean = this.sprite0Hit,
        spriteOverflow: Boolean = this.spriteOverflow,
    ) = PpuStatus(vblank, sprite0Hit, spriteOverflow)
}

/** Bitfield for Enemy_State array entries. Individual bits are flags; some whole-byte values have special meaning. */
@JvmInline
value class EnemyState(val byte: Byte) {
    constructor(value: Int) : this(value.toByte())

    val isActive: Boolean get() = byte != 0.toByte()
    /** Bit 0 -- enemy is airborne/jumping */
    val airborne: Boolean get() = byte.bit(0)
    /** Bit 1 -- enemy is stunned (set via SetStun) */
    val stunned: Boolean get() = byte.bit(1)
    /** Bit 3 -- hammer bro has thrown a hammer (cleared when hammer detaches) */
    val hammerThrown: Boolean get() = byte.bit(3)
    /** Bit 5 -- enemy is defeated and falling off screen */
    val defeated: Boolean get() = byte.bit(5)
    /** Bit 6 -- enemy falling off edge / Bowser bridge defeat */
    val fallingOffEdge: Boolean get() = byte.bit(6)
    /** Bit 7 -- kicked shell moving / power-up fully emerged */
    val kickedOrEmerged: Boolean get() = byte.bit(7)
    /** Low 3 bits, used for sub-state dispatch */
    val lowBits: Int get() = byte.toInt() and 0x07

    /** Unsigned int value (0..255) for numeric comparisons. */
    fun toInt(): Int = byte.toInt() and 0xFF

    /** Returns a new EnemyState with the given bit set */
    fun withBit(bit: Int): EnemyState = EnemyState(byte.bit(bit, true))
    /** Returns a new EnemyState with the given bit cleared */
    fun withoutBit(bit: Int): EnemyState = EnemyState(byte.bit(bit, false))
    /** Bitwise OR with a mask */
    infix fun or(mask: Int): EnemyState = EnemyState((byte.toInt() or mask).toByte())
    /** Bitwise AND with a mask */
    infix fun and(mask: Int): EnemyState = EnemyState((byte.toInt() and mask).toByte())

    override fun toString(): String = "EnemyState(0x${(byte.toInt() and 0xFF).toString(16).padStart(2, '0')})"

    companion object {
        val INACTIVE = EnemyState(0x00)
        val NORMAL = EnemyState(0x01)
        val STUNNED = EnemyState(0x02)
        val STUNNED_ON_GROUND = EnemyState(0x03)
        val STOMPED_SHELL = EnemyState(0x04)
        val SPINY_EGG = EnemyState(0x05)
        val DEFEATED = EnemyState(0x20)
        val BOWSER_FALLING = EnemyState(0x40)
        val KICKED_SHELL = EnemyState(0x80)
    }
}

/** Read an element as a typed EnemyState. */
fun ByteArray.getEnemyState(index: Int): EnemyState = EnemyState(this[index])
/** Write a typed EnemyState into the array. */
fun ByteArray.setEnemyState(index: Int, state: EnemyState) { this[index] = state.byte }

@JvmInline
value class VramBufferControl(val byte: Byte) {
    val drawVertically: Boolean get() = byte.bit(7)
    val repeat: Boolean get() = byte.bit(6)
    val length: Byte get() = byte.bitRange(0, 5)
    constructor(
        drawVertically: Boolean = false,
        repeat: Boolean = false,
        length: Byte = 0,
    ): this((
            (if (drawVertically) 0x1 shl 7 else 0) +
                    (if (repeat) 0x1 shl 6 else 0) +
                    (length.toInt() shr 0 and 1.shl(5).minus(1))
            ).toByte())
    fun copy(
        drawVertically: Boolean = this.drawVertically,
        repeat: Boolean = this.repeat,
        length: Byte = this.length,
    ) = VramBufferControl(drawVertically, repeat, length)
}