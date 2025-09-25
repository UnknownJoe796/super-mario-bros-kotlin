package com.ivieleague.smbtranslation

typealias TwoBits = Byte
typealias ThreeBits = Byte
typealias Nibble = Byte
typealias FiveBits = Byte
typealias SevenBits = Byte
typealias ElevenBits = Byte
typealias VramAddress = Short


class PictureProcessingUnit {
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
    class Control(val access: ByteAccess) {
        /**
         * Vblank NMI enable (0: off, 1: on)
         */
        var nmiEnabled: Boolean by BitAccess2(access, 7)

        /**
         * PPU master/slave select
         * (0: read backdrop from EXT pins; 1: output color on EXT pins)
         */
        var extWrite: Boolean by BitAccess2(access, 6)

        /**
         * Sprite size (0: 8x8 pixels; 1: 8x16 pixels – see PPU OAM#Byte 1)
         */
        var tallSpriteMode: Boolean by BitAccess2(access, 5)

        /**
         * Background pattern table address (0: $0000; 1: $1000)
         */
        var backgroundTableOffset: Boolean by BitAccess2(access, 4)

        /**
         * Sprite pattern table address for 8x8 sprites
         * (0: $0000; 1: $1000; ignored in 8x16 mode)
         */
        var spritePatternTableOffset: Boolean by BitAccess2(access, 3)

        /**
         * VRAM address increment per CPU read/write of PPUDATA
         * (0: add 1, going across; 1: add 32, going down)
         */
        var drawVertical: Boolean by BitAccess2(access, 2)

        /**
         * Base nametable address
         * (0 = $2000; 1 = $2400; 2 = $2800; 3 = $2C00)
         */
        var baseNametableAddress: TwoBits by BitRangeAccess2(access, 0,1)
    }
    private val controlByte = object: ByteAccess {
        override var value: Byte = 0
    }
    val control = Control(controlByte)

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

    class Mask(val access: ByteAccess) {
        var emphasizeBlue: Boolean by BitAccess2(access, 7)
        var emphasizeGreen: Boolean by BitAccess2(access, 6)
        var emphasizeRed: Boolean by BitAccess2(access, 5)
        var spriteEnabled: Boolean by BitAccess2(access, 4)
        var backgroundEnabled: Boolean by BitAccess2(access, 3)
        var showLeftBackground: Boolean by BitAccess2(access, 2)
        var showLeftSprites: Boolean by BitAccess2(access, 1)
        var greyscale: Boolean by BitAccess2(access, 0)
    }
    private val maskByte = object: ByteAccess {
        override var value: Byte = 0
    }
    val mask = Mask(maskByte)


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
        vramAddress = vramAddress.plus(if(control.drawVertical) 32 else 1).toShort()
        TODO()
    }
    fun writeVram(value: Byte) {
        vramAddress = vramAddress.plus(if(control.drawVertical) 32 else 1).toShort()
        TODO()
    }

    //    SPR_DMA / OAMDMA 	$4014 	AAAA AAAA 	W 	OAM DMA high address
    fun updateSpriteData(values: Array<GameRam.Sprite>) {
        TODO()
    }

}


class AudioProcessingUnit() {
    class Pulse {
        // $4000 / $4004 	DDLC VVVV 	Duty (D), envelope loop / length counter halt (L), constant volume (C), volume/envelope (V)
        var duty: TwoBits = 0
        var lengthCounterHalt: Boolean = false
        var constantVolume: Boolean = false
        var volume: Nibble = 0
        // $4001 / $4005 	EPPP NSSS 	Sweep unit: enabled (E), period (P), negate (N), shift (S)
        var sweepEnabled: Boolean = false
        var sweepPeriod: ThreeBits = 0
        var sweepNegate: Boolean = false
        var sweepShift: ThreeBits = 0
        // $4002 / $4006 	TTTT TTTT 	Timer low (T)
        var timer: Byte = 0
        // $4003 / $4007 	LLLL LTTT 	Length counter load (L), timer high (T)
        var length: FiveBits = 0
        var timerHigh: ThreeBits = 0
    }
    val pulse1 = Pulse()
    val pulse2 = Pulse()

    class Triangle {
        // $4008 	CRRR RRRR 	Length counter halt / linear counter control (C), linear counter load (R)
        var lengthCounterHalt: Boolean = false
        var linearCounterLoad: SevenBits = 0
        // $4009 	---- ---- 	Unused
        // $400A 	TTTT TTTT 	Timer low (T)
        var timer: ElevenBits = 0
        // $400B 	LLLL LTTT 	Length counter load (L), timer high (T), set linear counter reload flag
        var lengthCounterLoad: FiveBits = 0
    }
    val triangle = Triangle()

    class Noise {
        // $400C 	--LC VVVV 	Envelope loop / length counter halt (L), constant volume (C), volume/envelope (V)
        var lengthCounterHalt: Boolean = false
        var constantVolume: Boolean = false
        var volume: Nibble = 0
        // $400D 	---- ---- 	Unused
        // $400E 	M--- PPPP 	Noise mode (M), noise period (P)
        var noiseMode: Boolean = false
        var noisePeriod: Nibble = 0
        // $400F 	LLLL L--- 	Length counter load (L)
        var lengthCounterLoad: FiveBits = 0
    }
    val noise = Noise()

    class DeltaModulation {
        // $4010 	IL-- RRRR 	IRQ enable (I), loop (L), frequency (R)
        val irqEnable: Boolean = false
        val loop: Boolean = false
        val frequency: Nibble = 0
        // $4011 	-DDD DDDD 	Load counter (D)
        var loadCounter: SevenBits = 0
        // $4012 	AAAA AAAA 	Sample address (A)
        var sampleAddress: Byte = 0
        // $4013 	LLLL LLLL 	Sample length (L)
        var sampleLength: Byte = 0
    }
    val deltaModulation = DeltaModulation()

    //    $4015 	All 	Channel enable and length counter status
    //    $4015 write 	---D NT21 	Enable DMC (D), noise (N), triangle (T), and pulse channels (2/1)
    var dmcEnabled: Boolean = false
    var noiseEnabled: Boolean = false
    var triangleEnabled: Boolean = false
    var pulse2Enabled: Boolean = false
    var pulse1Enabled: Boolean = false
    //    $4015 read 	IF-D NT21 	DMC interrupt (I), frame interrupt (F), DMC active (D), length counter > 0 (N/T/2/1)
    class Status(
        val dmcInterrupt: Boolean = false,
        val frameInterrupt: Boolean = false,
        val dmcActive: Boolean = false,
        val noiseActive: Boolean = false,
        val triangleActive: Boolean = false,
        val pulse2Active: Boolean = false,
        val pulse1Active: Boolean = false,
    )
    fun status(): Status = TODO()

    // $4017 	All 	Frame counter
    // $4017 	MI-- ---- 	Mode (M, 0 = 4-step, 1 = 5-step), IRQ inhibit flag (I)
    enum class Mode { FourStep, FiveStep }
    var mode: Mode = Mode.FourStep
    var irqInhibit: Boolean = false
}

class Inputs {
    val joypadPort1 = JoypadBits()
    val joypadPort2 = JoypadBits()
}

class JoypadBits: ByteAccess {
    override var value: Byte = 0
    val a by BitAccess(7) // %10000000
    val b by BitAccess(6) // %01000000
    val select by BitAccess(5) // %00100000
    val start by BitAccess(4) // %00010000
    val up by BitAccess(3) // %00001000
    val down by BitAccess(2) // %00000100
    val left by BitAccess(1) // %00000010
    val right by BitAccess(0) // %00000001
}