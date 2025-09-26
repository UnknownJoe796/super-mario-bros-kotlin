package com.ivieleague.smbtranslation.utils

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
                    (palette.toInt() shr 0 and 1.shl(1).minus(1))
            ).toByte())
    fun copy(
        flipVertical: Boolean = this.flipVertical,
        flipHorizontal: Boolean = this.flipHorizontal,
        behindBackground: Boolean = this.behindBackground,
        palette: Byte = this.palette,
    ) = SpriteFlags(flipVertical, flipHorizontal, behindBackground, palette)
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
}

@JvmInline
value class PpuMask(val byte: Byte) {
    /** Greyscale (0: normal color, 1: greyscale) **/
    val greyscale: Boolean get() = byte.bit(0)
    /** 1: Show background in leftmost 8 pixels of screen, 0: Hide **/
    val showLeftSprites: Boolean get() = byte.bit(1)
    /** 1: Show sprites in leftmost 8 pixels of screen, 0: Hide **/
    val showLeftBackground: Boolean get() = byte.bit(2)
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
        showLeftSprites: Boolean = false,
        showLeftBackground: Boolean = false,
        backgroundEnabled: Boolean = false,
        spriteEnabled: Boolean = false,
        emphasizeRed: Boolean = false,
        emphasizeGreen: Boolean = false,
        emphasizeBlue: Boolean = false,
    ): this((
            (if (greyscale) 0x1 shl 0 else 0) +
                    (if (showLeftSprites) 0x1 shl 1 else 0) +
                    (if (showLeftBackground) 0x1 shl 2 else 0) +
                    (if (backgroundEnabled) 0x1 shl 3 else 0) +
                    (if (spriteEnabled) 0x1 shl 4 else 0) +
                    (if (emphasizeRed) 0x1 shl 5 else 0) +
                    (if (emphasizeGreen) 0x1 shl 6 else 0) +
                    (if (emphasizeBlue) 0x1 shl 7 else 0)
            ).toByte())
    fun copy(
        greyscale: Boolean = this.greyscale,
        showLeftSprites: Boolean = this.showLeftSprites,
        showLeftBackground: Boolean = this.showLeftBackground,
        backgroundEnabled: Boolean = this.backgroundEnabled,
        spriteEnabled: Boolean = this.spriteEnabled,
        emphasizeRed: Boolean = this.emphasizeRed,
        emphasizeGreen: Boolean = this.emphasizeGreen,
        emphasizeBlue: Boolean = this.emphasizeBlue,
    ) = PpuMask(greyscale, showLeftSprites, showLeftBackground, backgroundEnabled, spriteEnabled, emphasizeRed, emphasizeGreen, emphasizeBlue)
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
