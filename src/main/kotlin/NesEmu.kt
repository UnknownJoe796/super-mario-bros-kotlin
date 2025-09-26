package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.ByteAccess
import com.ivieleague.smbtranslation.utils.JoypadBits
import com.ivieleague.smbtranslation.utils.PpuControl
import com.ivieleague.smbtranslation.utils.PpuMask
import com.ivieleague.smbtranslation.utils.PpuStatus

typealias TwoBits = Byte
typealias ThreeBits = Byte
typealias Nibble = Byte
typealias FiveBits = Byte
typealias SevenBits = Byte
typealias ElevenBits = Byte
typealias VramAddress = Short


class PictureProcessingUnit {
    /**
     * PPU_CTRL_REG1: $2000
     */
    var control: PpuControl = PpuControl(0)

    /**
     * PPU_CTRL_REG2: $2001
     */
    var mask: PpuMask = PpuMask(0)

    /**
     * PPU_STATUS / PPUSTATUS: $2002
     */
    val status: PpuStatus = PpuStatus(0)

    //    PPU_SPR_ADDR / OAMADDR 	$2003 	AAAA AAAA 	W 	OAM read/write address
    var oamAddress: Byte = 0x00
    fun writeOamAddress(address: Byte) {
        // In hardware this writes to OAMADDR ($2003); here we mirror into our property
        oamAddress = address
        // TODO: Implement side effects if/when OAM is emulated
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
    var internalVramAddress: VramAddress = 0x0000
    fun setVramAddress(value: VramAddress) {
        internalVramAddress = value
    }

    //    PPU_DATA / PPUDATA 	$2007 	DDDD DDDD 	RW 	VRAM data read/write
    fun readVram(): Byte {
        internalVramAddress = internalVramAddress.plus(if(control.drawVertical) 32 else 1).toShort()
        TODO()
    }
    fun writeVram(value: Byte) {
        internalVramAddress = internalVramAddress.plus(if(control.drawVertical) 32 else 1).toShort()
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
    var joypadPort1: JoypadBits = JoypadBits(0)
    var joypadPort2: JoypadBits = JoypadBits(0)
}
