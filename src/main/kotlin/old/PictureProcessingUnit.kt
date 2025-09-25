package com.ivieleague.smbtranslation.old

typealias TwoBits = Byte
typealias ThreeBits = Byte
typealias Nibble = Byte
typealias FiveBits = Byte
typealias SevenBits = Byte
typealias ElevenBits = Byte
typealias VramAddress = Short

object PictureProcessingUnit {
    //    PPU_CTRL_REG1: $2000
    //    7  bit  0
    //    ---- ----
    //    VPHB SINN
    //    |||| ||||
    //    |||| ||++- Base nametable address
    //    |||| ||    (0 = $2000; 1 = $2400; 2 = $2800; 3 = $2C00)
    //    |||| |+--- VRAM address increment per CPU read/write of PPUDATA
    //    |||| |     (0: add 1, going across; 1: add 32, going down)
    //    |||| +---- Sprite pattern table address for 8x8 sprites
    //    ||||       (0: $0000; 1: $1000; ignored in 8x16 mode)
    //    |||+------ Background pattern table address (0: $0000; 1: $1000)
    //    ||+------- Sprite size (0: 8x8 pixels; 1: 8x16 pixels – see PPU OAM#Byte 1)
    //    |+-------- PPU master/slave select
    //    |          (0: read backdrop from EXT pins; 1: output color on EXT pins)
    //    +--------- Vblank NMI enable (0: off, 1: on)

    object Control {
        /**
         * Vblank NMI enable (0: off, 1: on)
         */
        var nmiEnabled: Boolean = false

        /**
         * PPU master/slave select
         * (0: read backdrop from EXT pins; 1: output color on EXT pins)
         */
        var extWrite: Boolean = false

        /**
         * Sprite size (0: 8x8 pixels; 1: 8x16 pixels – see PPU OAM#Byte 1)
         */
        var tallSpriteMode: Boolean = false

        /**
         * Background pattern table address (0: $0000; 1: $1000)
         */
        var backgroundTableOffset: Boolean = true

        /**
         * Sprite pattern table address for 8x8 sprites
         * (0: $0000; 1: $1000; ignored in 8x16 mode)
         */
        var spritePatternTableOffset: Boolean = false

        /**
         * VRAM address increment per CPU read/write of PPUDATA
         * (0: add 1, going across; 1: add 32, going down)
         */
        var drawVertical: Boolean = false

        /**
         * Base nametable address
         * (0 = $2000; 1 = $2400; 2 = $2800; 3 = $2C00)
         */
        var baseNametableAddress: TwoBits = 0x0
    }


    object Mask {
        //    PPU_CTRL_REG2: $2001
        //    7  bit  0
        //    ---- ----
        //    BGRs bMmG
        //    |||| ||||
        //    |||| |||+- Greyscale (0: normal color, 1: greyscale)
        //    |||| ||+-- 1: Show background in leftmost 8 pixels of screen, 0: Hide
        //    |||| |+--- 1: Show sprites in leftmost 8 pixels of screen, 0: Hide
        //    |||| +---- 1: Enable background rendering
        //    |||+------ 1: Enable sprite rendering
        //    ||+------- Emphasize red (green on PAL/Dendy)
        //    |+-------- Emphasize green (red on PAL/Dendy)
        //    +--------- Emphasize blue
        var emphasizeBlue: Boolean = false
        var emphasizeGreen: Boolean = false
        var emphasizeRed: Boolean = false
        var spriteEnabled: Boolean = false
        var backgroundEnabled: Boolean = false
        var showLeftBackground: Boolean = false
        var showLeftSprites: Boolean = false
        var greyscale: Boolean = false
    }


    object Status {
        //    PPU_STATUS / PPUSTATUS 	$2002 	VSO- ---- 	R 	vblank (V), sprite 0 hit (S), sprite overflow (O); read resets write pair for $2005/$2006
        var vblank: Boolean = false
        var spriteZeroHit: Boolean = false
        var spriteOverflow: Boolean = false
    }


    //    PPU_SPR_ADDR / OAMADDR 	$2003 	AAAA AAAA 	W 	OAM read/write address
    var oamAddress: Byte = 0x00
    fun setOamAddress(address: Byte) {
        TODO()
    }

    //    PPU_SPR_DATA / OAMDATA 	$2004 	DDDD DDDD 	RW 	OAM data read/write
    fun readOamData(): Byte = TODO()
    fun writeOamData(data: Byte) {
        oamAddress++
        TODO()
    }

    //    PPU_SCROLL_REG / PPUSCROLL 	$2005 	XXXX XXXX YYYY YYYY 	Wx2 	X and Y scroll bits 7-0 (two writes: X scroll, then Y scroll)
    fun scroll(x: Byte, y: Byte) { TODO() }

    //    PPU_ADDRESS / PPUADDR 	$2006 	..AA AAAA AAAA AAAA 	Wx2 	VRAM address (two writes: most significant byte, then least significant byte)
    var vramAddress: VramAddress = 0x0000
    fun setVramAddress(value: VramAddress) {
        vramAddress = value
    }

    //    PPU_DATA / PPUDATA 	$2007 	DDDD DDDD 	RW 	VRAM data read/write
    fun readVram(): Byte {
        vramAddress = vramAddress.plus(if(Control.drawVertical) 32 else 1).toShort()
        TODO()
    }
    fun writeVram(value: Byte) {
        vramAddress = vramAddress.plus(if(Control.drawVertical) 32 else 1).toShort()
        TODO()
    }

    //    SPR_DMA / OAMDMA 	$4014 	AAAA AAAA 	W 	OAM DMA high address
    // This is redundant.  We'll just edit the real data directly.
    fun copyPage(high: Byte) {
        TODO()
    }

}

class ObjectAttributes(
    var y: UByte = 0.toUByte(),
    var tileIndexNumber: Byte = 0,
    //    76543210
    //    ||||||||
    //    ||||||++- Palette (4 to 7) of sprite
    //    |||+++--- Unimplemented (read 0)
    //    ||+------ Priority (0: in front of background; 1: behind background)
    //    |+------- Flip sprite horizontally
    //    +-------- Flip sprite vertically
    var flipVertical: Boolean = false,
    var flipHorizontal: Boolean = false,
    var behindBackground: Boolean = false,
    var palette: TwoBits = 0x0,
    // three non-existent bits
    var x: UByte = 0.toUByte(),
) {
    infix fun set(other: ObjectAttributes) {
        y = other.y
        tileIndexNumber = other.tileIndexNumber
        flipVertical = other.flipVertical
        flipHorizontal = other.flipHorizontal
        behindBackground = other.behindBackground
        palette = other.palette
        x = other.x
    }

    companion object {
        // Exactly 0xFF bytes
        val buffered = Array<ObjectAttributes>(64) { ObjectAttributes() }
        val sprites = Array<ObjectAttributes>(64) { ObjectAttributes() }

        fun submitBuffer() {
            for(i in 0..<64) sprites[i] set buffered[i]
        }
    }
}